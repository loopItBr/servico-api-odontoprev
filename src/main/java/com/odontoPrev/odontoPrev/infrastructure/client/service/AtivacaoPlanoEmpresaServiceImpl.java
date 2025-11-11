package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAtivacaoPlanoResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import com.odontoPrev.odontoPrev.infrastructure.repository.ControleSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * IMPLEMENTAÇÃO DO SERVIÇO PARA ATIVAÇÃO DO PLANO DA EMPRESA
 *
 * Este serviço é responsável por ativar o plano odontológico de uma empresa
 * após o cadastro bem-sucedido da sincronização.
 *
 * FLUXO COMPLETO:
 * 1. Recebe dados da empresa sincronizada
 * 2. Converte dados para formato da API de ativação
 * 3. Cria registro de controle para auditoria
 * 4. Chama API da OdontoPrev para ativar o plano
 * 5. Registra resultado no controle de sincronização
 *
 * TRATAMENTO DE ERROS:
 * - Erros são registrados no controle de sincronização
 * - Sistema continua funcionando mesmo com falhas na ativação
 * - Logs detalhados para auditoria
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AtivacaoPlanoEmpresaServiceImpl implements AtivacaoPlanoEmpresaService {

    private final BeneficiarioOdontoprevFeignClient feignClient;
    private final ControleSyncRepository controleSyncRepository;
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;

    @Value("${odontoprev.api.codigo-grupo-gerencial:787392}")
    private String codigoGrupoGerencialPadrao;

    @PostConstruct
    public void init() {
        log.info("✅ [INICIALIZAÇÃO] AtivacaoPlanoEmpresaServiceImpl - codigoGrupoGerencialPadrao configurado: {}", codigoGrupoGerencialPadrao);
    }

    @Override
    @Transactional
    public EmpresaAtivacaoPlanoResponse ativarPlanoEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        String codigoEmpresa = dadosEmpresa.getCodigoEmpresa();
        
        // VALIDAÇÃO: Verificar se codigoEmpresa é válido
        if (codigoEmpresa == null || codigoEmpresa.trim().isEmpty()) {
            log.error("❌ [ATIVAÇÃO PLANO] codigoEmpresa é null ou vazio - não é possível ativar plano");
            throw new IllegalArgumentException("codigoEmpresa não pode ser null ou vazio para ativação do plano");
        }
        
        log.info("🚀 [ATIVAÇÃO PLANO] Iniciando ativação do plano para empresa: {}", codigoEmpresa);

        try {
            // Etapa 1: Converter dados da empresa para formato da API
            EmpresaAtivacaoPlanoRequest request = converterParaRequestAtivacao(dadosEmpresa);
            
            // Etapa 2: Criar registro de controle
            ControleSync controleSync = criarRegistroControleAtivacao(codigoEmpresa, request);
            
            // Etapa 3: Obter token de autenticação
            log.info("🔑 [ATIVAÇÃO PLANO] Obtendo token de autenticação para empresa: {}", codigoEmpresa);
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            log.info("🔑 [ATIVAÇÃO PLANO] Token obtido com sucesso para empresa: {}", codigoEmpresa);
            
            // Etapa 4: Chamar API da OdontoPrev
            log.info("📡 [ATIVAÇÃO PLANO] Chamando API de ativação para empresa: {}", codigoEmpresa);
            EmpresaAtivacaoPlanoResponse response = feignClient.ativarPlanoEmpresa(
                authorization,
                request
            );
            
            // VALIDAÇÃO: Verificar se a resposta da API contém codigoEmpresa válido
            if (response != null && (response.getCodigoEmpresa() == null || response.getCodigoEmpresa().trim().isEmpty())) {
                log.warn("⚠️ [ATIVAÇÃO PLANO] API retornou codigoEmpresa vazio para empresa: {} - Response: {}", 
                        codigoEmpresa, response);
                // Continuar processamento mesmo com codigoEmpresa vazio da API
            }
            
            // Etapa 5: Processar sucesso
            processarSucessoAtivacao(controleSync, response);
            
            log.info("✅ [ATIVAÇÃO PLANO] Plano ativado com sucesso para empresa: {}", codigoEmpresa);
            return response;

        } catch (Exception e) {
            log.error("❌ [ATIVAÇÃO PLANO] Erro ao ativar plano para empresa {}: {}", 
                    codigoEmpresa, e.getMessage(), e);
            
            // Registrar erro no controle
            processarErroAtivacao(codigoEmpresa, e.getMessage());
            throw e;
        }
    }

    /**
     * CONVERTE DADOS DA EMPRESA PARA FORMATO DA API DE ATIVAÇÃO
     *
     * Mapeia campos da entidade IntegracaoOdontoprev para o formato
     * esperado pela API de ativação do plano da empresa.
     */
    private EmpresaAtivacaoPlanoRequest converterParaRequestAtivacao(IntegracaoOdontoprev dadosEmpresa) {
        log.debug("🔄 [CONVERSÃO] Convertendo dados da empresa {} para request de ativação", 
                dadosEmpresa.getCodigoEmpresa());

        // Determinar codigoGrupoGerencial: usar da view se disponível, senão usar o padrão configurado
        String codigoGrupoGerencial = dadosEmpresa.getCodigoGrupoGerencial() != null 
                ? dadosEmpresa.getCodigoGrupoGerencial().toString() 
                : codigoGrupoGerencialPadrao;
        
        log.debug("📋 [ATIVAÇÃO PLANO] codigoGrupoGerencial - View: {}, Usando: {}, Padrão configurado: {}", 
                dadosEmpresa.getCodigoGrupoGerencial(), codigoGrupoGerencial, codigoGrupoGerencialPadrao);

        // Valores padrão baseados no cURL fornecido
        EmpresaAtivacaoPlanoRequest request = EmpresaAtivacaoPlanoRequest.builder()
                .sistema(dadosEmpresa.getSistema() != null ? dadosEmpresa.getSistema() : "SabinSinai")
                .tipoPessoa(dadosEmpresa.getTipoPessoa() != null ? dadosEmpresa.getTipoPessoa() : "J")
                .emiteCarteirinhaPlastica(dadosEmpresa.getEmiteCarteirinhaPlastica() != null ? dadosEmpresa.getEmiteCarteirinhaPlastica() : "N")
                .codigoEmpresaGestora(dadosEmpresa.getCodigoEmpresaGestora() != null ? dadosEmpresa.getCodigoEmpresaGestora().intValue() : 1)
                .codigoFilialEmpresaGestora(dadosEmpresa.getCodigoFilialEmpresaGestora() != null ? dadosEmpresa.getCodigoFilialEmpresaGestora().intValue() : 1)
                .codigoGrupoGerencial(codigoGrupoGerencial)
                .codigoNaturezaJuridica(dadosEmpresa.getCodigoNaturezaJuridica() != null ? dadosEmpresa.getCodigoNaturezaJuridica() : "6550-2")
                .nomeNaturezaJuridica(dadosEmpresa.getNomeNaturezaJuridica() != null ? dadosEmpresa.getNomeNaturezaJuridica() : "Planos de saúde")
                .situacaoCadastral(dadosEmpresa.getSituacaoCadastral() != null ? dadosEmpresa.getSituacaoCadastral() : "ATIVO")
                .inscricaoMunicipal(dadosEmpresa.getInscricaoMunicipal() != null ? dadosEmpresa.getInscricaoMunicipal() : "997.179.737.204") // Valor padrão do cURL
                .inscricaoEstadual(dadosEmpresa.getInscricaoEstadual() != null ? dadosEmpresa.getInscricaoEstadual() : "997.179.737.204") // Valor padrão do cURL
                .dataConstituicao(dadosEmpresa.getDataConstituicao() != null ? dadosEmpresa.getDataConstituicao() : "2025-10-01T00:00:00.000Z")
                .renovacaoAutomatica(dadosEmpresa.getRenovacaoAutomatica() != null ? dadosEmpresa.getRenovacaoAutomatica() : "S")
                .codigoClausulaReajusteDiferenciado(dadosEmpresa.getCodigoClausulaReajusteDiferenciado() != null ? dadosEmpresa.getCodigoClausulaReajusteDiferenciado().toString() : "1")
                .departamento(dadosEmpresa.getDepartamento() != null ? dadosEmpresa.getDepartamento().toString() : "SEM DEPARTAMENTO")
                .dependentePaga(dadosEmpresa.getDependentePaga() != null ? dadosEmpresa.getDependentePaga() : "N")
                .permissaoCadastroDep(dadosEmpresa.getPermissaoCadastroDep() != null ? dadosEmpresa.getPermissaoCadastroDep().equals("S") : true)
                .modeloCobrancaVarejo(false)
                .numeroMinimoAssociados(dadosEmpresa.getNumeroMinimoAssociados() != null ? dadosEmpresa.getNumeroMinimoAssociados().intValue() : 3)
                .numeroFuncionarios(dadosEmpresa.getNumeroFuncionarios() != null ? dadosEmpresa.getNumeroFuncionarios().intValue() : 0)
                .numeroDepedentes(dadosEmpresa.getNumeroDependentes() != null ? dadosEmpresa.getNumeroDependentes().intValue() : 0)
                .idadeLimiteDependente(dadosEmpresa.getIdadeLimiteDependente() != null ? dadosEmpresa.getIdadeLimiteDependente().intValue() : 21)
                .valorFator(dadosEmpresa.getValorFator() != null ? dadosEmpresa.getValorFator().intValue() : 1)
                .tipoRetornoCritica("T")
                .codigoLayoutCarteirinha(dadosEmpresa.getCodigoLayoutCarteirinha() != null ? dadosEmpresa.getCodigoLayoutCarteirinha() : "B")
                .codigoOrdemCarteira(dadosEmpresa.getCodigoOrdemCarteira() != null ? dadosEmpresa.getCodigoOrdemCarteira().intValue() : 3)
                .codigoDocumentoContrato(0)
                .codigoCelula(dadosEmpresa.getCodigoCelula() != null ? dadosEmpresa.getCodigoCelula().intValue() : 9)
                .codigoMarca(dadosEmpresa.getCodigoMarca() != null ? dadosEmpresa.getCodigoMarca().intValue() : 1)
                .codigoDescricaoNF(0)
                .diaVencimentoAg(dadosEmpresa.getDiaVencimentoAg() != null ? dadosEmpresa.getDiaVencimentoAg().intValue() : 19)
                .codigoPerfilClienteFatura(dadosEmpresa.getCodigoPerfilClienteFatura() != null ? dadosEmpresa.getCodigoPerfilClienteFatura().intValue() : 3)
                .codigoBancoFatura(dadosEmpresa.getCodigoBancoFatura() != null ? dadosEmpresa.getCodigoBancoFatura().toString().trim() + " " : "085 ")
                .multaFatura(dadosEmpresa.getMultaFatura() != null ? dadosEmpresa.getMultaFatura().intValue() : 0)
                .descontaIR(dadosEmpresa.getDescontaIr() != null ? dadosEmpresa.getDescontaIr() : "N")
                .retencaoIss(dadosEmpresa.getRetencaoIss() != null ? dadosEmpresa.getRetencaoIss() : "N")
                .liberaSenhaInternet(dadosEmpresa.getLiberaSenhaInternet() != null ? dadosEmpresa.getLiberaSenhaInternet() : "S")
                .faturamentoNotaCorte(dadosEmpresa.getFaturamentoNotaCorte() != null ? dadosEmpresa.getFaturamentoNotaCorte() : "N")
                .proRata(dadosEmpresa.getProrata() != null ? dadosEmpresa.getProrata() : "N")
                .custoFamiliar(dadosEmpresa.getCustoFamiliar() != null ? dadosEmpresa.getCustoFamiliar() : "S")
                .planoFamiliar(dadosEmpresa.getPlanoFamiliar() != null ? dadosEmpresa.getPlanoFamiliar() : "S")
                .percSinistroContrato(dadosEmpresa.getValorSinistroContrato() != null ? dadosEmpresa.getValorSinistroContrato().intValue() : 60)
                .idadeLimiteUniversitaria(dadosEmpresa.getIdadeLimiteUniversitario() != null ? dadosEmpresa.getIdadeLimiteUniversitario().intValue() : 24)
                .percentualINSSAutoGestao(0)
                .percentualMateriaisAutoGestao(0)
                .valorSinistroContrato(dadosEmpresa.getValorSinistroContrato() != null ? dadosEmpresa.getValorSinistroContrato().doubleValue() : 60.0)
                .percentualAssociado(0)
                .codigoRegiao(dadosEmpresa.getCodigoRegiao() != null ? dadosEmpresa.getCodigoRegiao().intValue() : 0)
                .codigoImagemFatura(dadosEmpresa.getCodigoImagemFatura() != null ? dadosEmpresa.getCodigoImagemFatura().intValue() : 1)
                .codigoMoeda(dadosEmpresa.getCodigoMoeda() != null ? dadosEmpresa.getCodigoMoeda().toString() : "7")
                .codigoParceriaEstrategica(0)
                .sinistralidade(60)
                .posicaoIniTIT(dadosEmpresa.getPosicaoInitTit() != null ? dadosEmpresa.getPosicaoInitTit().intValue() : 1)
                .posicaoFimTIT(dadosEmpresa.getPosicaoFimTit() != null ? dadosEmpresa.getPosicaoFimTit().intValue() : 7)
                .regraDowngrade(0)
                .mesCompetenciaProximoFaturamento("09")
                .codigoUsuarioFaturamento("")
                .codigoUsuarioCadastro("")
                .ramo("Massificado")
                .cgc(dadosEmpresa.getCnpj()) // Usar cnpj da entidade
                .razaoSocial(dadosEmpresa.getRazaoSocial() != null ? dadosEmpresa.getRazaoSocial() : dadosEmpresa.getNomeFantasia())
                .nomeFantasia(dadosEmpresa.getNomeFantasia())
                .diaInicioFaturamento(dadosEmpresa.getDiaInicioFaturamento() != null ? dadosEmpresa.getDiaInicioFaturamento().intValue() : 20)
                .codigoUsuarioConsultor(dadosEmpresa.getCodigoUsuarioConsultor() != null ? dadosEmpresa.getCodigoUsuarioConsultor().toString() : "FEODPV01583")
                .mesAniversarioReajuste(dadosEmpresa.getMesAniversarioReajuste() != null ? dadosEmpresa.getMesAniversarioReajuste().intValue() : 7)
                .dataInicioContrato(formatarDataInicioContrato(dadosEmpresa.getDataInicioContrato()))
                .dataVigencia(formatarDataVigencia(dadosEmpresa.getDataVigencia()))
                .descricaoRamoAtividade(dadosEmpresa.getDescricaoRamoAtividade() != null ? dadosEmpresa.getDescricaoRamoAtividade() : "Saúde Suplementar")
                .diaVencimento(dadosEmpresa.getDiaVencimento() != null ? dadosEmpresa.getDiaVencimento().intValue() : 15)
                .cnae(dadosEmpresa.getCnae() != null ? dadosEmpresa.getCnae() : "6550-2/00")
                .codigoManual(dadosEmpresa.getCodigoManual() != null ? dadosEmpresa.getCodigoManual().toString().trim() + " " : "1 ")
                .diaLimiteConsumoAg(19)
                .email(dadosEmpresa.getEmail() != null ? dadosEmpresa.getEmail() : "diretoria@sabinjf.com.br")
                .diaMovAssociadoEmpresa(dadosEmpresa.getDiaMovAssociadoEmpresa() != null ? dadosEmpresa.getDiaMovAssociadoEmpresa().intValue() : 15)
                .build();

        // Configurar planos (valores padrão do cURL)
        List<EmpresaAtivacaoPlanoRequest.Plano> planos = new ArrayList<>();
        
        // Plano 1
        planos.add(EmpresaAtivacaoPlanoRequest.Plano.builder()
                .codigoPlano("9972")
                .dataInicioPlano("2025-01-01T03:00:00.000")
                .valorDependente(27.42)
                .valorReembolsoUO(0.0)
                .valorTitular(27.42)
                .periodicidade("N")
                .percentualAssociado(0.0)
                .percentualDependenteRedeGenerica(0.0)
                .percentualAgregadoRedeGenerica(0.0)
                .redes(List.of(EmpresaAtivacaoPlanoRequest.Rede.builder()
                        .codigoRede("1")
                        .build()))
                .build());

        // Plano 2
        planos.add(EmpresaAtivacaoPlanoRequest.Plano.builder()
                .codigoPlano("9973")
                .dataInicioPlano("2025-01-01T03:00:00.000")
                .valorDependente(27.42)
                .periodicidade("N")
                .percentualAssociado(0.0)
                .percentualDependenteRedeGenerica(0.0)
                .percentualAgregadoRedeGenerica(0.0)
                .valorReembolsoUO(0.0)
                .valorTitular(27.42)
                .redes(List.of(EmpresaAtivacaoPlanoRequest.Rede.builder()
                        .codigoRede("1")
                        .build()))
                .build());

        // Plano 3
        planos.add(EmpresaAtivacaoPlanoRequest.Plano.builder()
                .codigoPlano("9974")
                .dataInicioPlano("2025-01-01T03:00:00.000")
                .valorDependente(27.42)
                .valorReembolsoUO(0.0)
                .valorTitular(27.42)
                .periodicidade("N")
                .percentualAssociado(0.0)
                .percentualDependenteRedeGenerica(0.0)
                .percentualAgregadoRedeGenerica(0.0)
                .redes(List.of(EmpresaAtivacaoPlanoRequest.Rede.builder()
                        .codigoRede("1")
                        .build()))
                .build());

        request.setPlanos(planos);

        // Configurar graus de parentesco
        List<EmpresaAtivacaoPlanoRequest.GrauParentesco> grausParentesco = List.of(
                EmpresaAtivacaoPlanoRequest.GrauParentesco.builder().codigoGrauParentesco("1").build(),
                EmpresaAtivacaoPlanoRequest.GrauParentesco.builder().codigoGrauParentesco("2").build(),
                EmpresaAtivacaoPlanoRequest.GrauParentesco.builder().codigoGrauParentesco("3").build(),
                EmpresaAtivacaoPlanoRequest.GrauParentesco.builder().codigoGrauParentesco("11").build(),
                EmpresaAtivacaoPlanoRequest.GrauParentesco.builder().codigoGrauParentesco("17").build(),
                EmpresaAtivacaoPlanoRequest.GrauParentesco.builder().codigoGrauParentesco("18").build(),
                EmpresaAtivacaoPlanoRequest.GrauParentesco.builder().codigoGrauParentesco("42").build()
        );
        request.setGrausParentesco(grausParentesco);

        // Configurar contatos da fatura
        List<EmpresaAtivacaoPlanoRequest.ContatoFatura> contatosDaFatura = List.of(
                EmpresaAtivacaoPlanoRequest.ContatoFatura.builder()
                        .codSequencial(1)
                        .email("diretoria@sabinjf.com.br")
                        .nomeContato("Célio Carneiro Chagas")
                        .relatorio(false)
                        .build()
        );
        request.setContatosDaFatura(contatosDaFatura);

        // Configurar endereço
        EmpresaAtivacaoPlanoRequest.Endereco endereco = EmpresaAtivacaoPlanoRequest.Endereco.builder()
                .cep("36033318")
                .descricao("Av. Presidente Itamar Franco")
                .complemento("loja 202 E")
                .tipoLogradouro("2")
                .logradouro("Av. Presidente Itamar Franco")
                .numero("4001")
                .bairro("Cascatinha")
                .cidade(EmpresaAtivacaoPlanoRequest.Cidade.builder()
                        .codigo(3670)
                        .nome("Juiz de Fora")
                        .siglaUf("MG")
                        .codigoPais(1)
                        .build())
                .build();
        request.setEndereco(endereco);

        // Configurar cobrança
        EmpresaAtivacaoPlanoRequest.Cobranca cobranca = EmpresaAtivacaoPlanoRequest.Cobranca.builder()
                .nome(dadosEmpresa.getNomeFantasia()) // Usar nomeFantasia da entidade
                .cgc(dadosEmpresa.getCnpj()) // Usar cnpj da entidade
                .endereco(endereco)
                .build();
        request.setCobranca(cobranca);

        // Configurar contatos usando dados da view
        List<EmpresaAtivacaoPlanoRequest.Contato> contatos = criarContatos(dadosEmpresa);
        request.setContatos(contatos);

        // Configurar comissionamentos usando dados da view
        List<EmpresaAtivacaoPlanoRequest.Comissionamento> comissionamentos = criarComissionamentos(dadosEmpresa);
        request.setComissionamentos(comissionamentos);

        // Configurar grupos - Incluir grupo com codigoGrupo 109 conforme especificação
        List<EmpresaAtivacaoPlanoRequest.Grupo> grupos = List.of(
                EmpresaAtivacaoPlanoRequest.Grupo.builder()
                        .codigoGrupo(109)
                        .build()
        );
        request.setGrupos(grupos);

        log.debug("✅ [CONVERSÃO] Request de ativação criado com sucesso para empresa: {}", 
                dadosEmpresa.getCodigoEmpresa());

        return request;
    }

    /**
     * CRIA REGISTRO DE CONTROLE PARA ATIVAÇÃO DO PLANO
     */
    private ControleSync criarRegistroControleAtivacao(String codigoEmpresa, EmpresaAtivacaoPlanoRequest request) {
        try {
            // VALIDAÇÃO: Verificar se codigoEmpresa é válido
            if (codigoEmpresa == null || codigoEmpresa.trim().isEmpty()) {
                log.error("❌ [CONTROLE] codigoEmpresa é null ou vazio - não é possível criar registro de controle");
                throw new IllegalArgumentException("codigoEmpresa não pode ser null ou vazio");
            }
            
            String payloadJson = objectMapper.writeValueAsString(request);

            ControleSync controle = ControleSync.builder()
                    .codigoEmpresa(codigoEmpresa)
                    .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                    .tipoControle(3) // Tipo 3 = Ativação de Plano
                    .endpointDestino("/empresa/2.0/empresas/contrato/empresarial")
                    .dadosJson(payloadJson)
                    .statusSync(ControleSync.StatusSync.PENDING)
                    .dataCriacao(LocalDateTime.now())
                    .build();

            ControleSync controleSalvo = controleSyncRepository.save(controle);
            log.info("✅ [CONTROLE] Registro de controle criado para ativação - ID: {}, Empresa: {}", 
                    controleSalvo.getId(), codigoEmpresa);
            
            return controleSalvo;

        } catch (Exception e) {
            log.error("❌ [CONTROLE] Erro ao criar registro de controle para ativação da empresa {}: {}", 
                    codigoEmpresa, e.getMessage(), e);
            return null;
        }
    }

    /**
     * PROCESSA SUCESSO DA ATIVAÇÃO DO PLANO
     */
    private void processarSucessoAtivacao(ControleSync controleSync, EmpresaAtivacaoPlanoResponse response) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            
            // VALIDAÇÃO: Verificar se a resposta da API contém codigoEmpresa válido
            if (response != null && response.getCodigoEmpresa() != null && !response.getCodigoEmpresa().trim().isEmpty()) {
                // Se a API retornou um codigoEmpresa válido, atualizar o controle
                log.info("🔄 [ATIVAÇÃO PLANO] Atualizando codigoEmpresa do controle: {} -> {}", 
                        controleSync.getCodigoEmpresa(), response.getCodigoEmpresa());
                controleSync.setCodigoEmpresa(response.getCodigoEmpresa());
            } else {
                log.warn("⚠️ [ATIVAÇÃO PLANO] API retornou codigoEmpresa vazio - mantendo codigoEmpresa original: {}", 
                        controleSync.getCodigoEmpresa());
            }
            
            controleSync.setStatusSync(ControleSync.StatusSync.SUCCESS);
            controleSync.setResponseApi(responseJson);
            controleSync.setDataSucesso(LocalDateTime.now());
            
            controleSyncRepository.save(controleSync);
            
            log.info("✅ [ATIVAÇÃO PLANO] Status atualizado para SUCESSO - Empresa: {}", 
                    controleSync.getCodigoEmpresa());
            
        } catch (Exception e) {
            log.error("❌ [ATIVAÇÃO PLANO] Erro ao processar sucesso da ativação: {}", e.getMessage(), e);
        }
    }

    /**
     * PROCESSA ERRO DA ATIVAÇÃO DO PLANO
     */
    private void processarErroAtivacao(String codigoEmpresa, String mensagemErro) {
        try {
            // Buscar controle existente ou criar novo
            ControleSync controleSync = controleSyncRepository
                    .findByCodigoEmpresaAndTipoControle(codigoEmpresa, 3)
                    .orElse(null);

            if (controleSync == null) {
                // Criar novo controle para erro
                controleSync = ControleSync.builder()
                        .codigoEmpresa(codigoEmpresa)
                        .tipoOperacao(ControleSync.TipoOperacao.CREATE)
                        .tipoControle(3)
                        .endpointDestino("/empresa/2.0/empresas/contrato/empresarial")
                        .statusSync(ControleSync.StatusSync.ERROR)
                        .dataCriacao(LocalDateTime.now())
                        .build();
            } else {
                controleSync.setStatusSync(ControleSync.StatusSync.ERROR);
            }

            controleSync.setErroMensagem(mensagemErro);
            controleSyncRepository.save(controleSync);
            
            log.info("❌ [ATIVAÇÃO PLANO] Status atualizado para ERRO - Empresa: {}", codigoEmpresa);
            
        } catch (Exception e) {
            log.error("❌ [ATIVAÇÃO PLANO] Erro ao processar erro da ativação: {}", e.getMessage(), e);
        }
    }

    /**
     * FORMATA DATA DE INÍCIO DO CONTRATO
     */
    private String formatarDataInicioContrato(String dataInicioContrato) {
        if (dataInicioContrato == null || dataInicioContrato.trim().isEmpty()) {
            return java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
        try {
            // Se já está no formato correto, retorna como está
            if (dataInicioContrato.contains("T")) {
                return dataInicioContrato;
            }
            // Se é apenas data, adiciona horário
            return dataInicioContrato + "T00:00:00.000";
        } catch (Exception e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao formatar dataInicioContrato '{}': {}", dataInicioContrato, e.getMessage());
            return java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
    }

    /**
     * FORMATA DATA DE VIGÊNCIA
     */
    private String formatarDataVigencia(String dataVigencia) {
        if (dataVigencia == null || dataVigencia.trim().isEmpty()) {
            return java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
        try {
            // Se já está no formato correto, retorna como está
            if (dataVigencia.contains("T")) {
                return dataVigencia;
            }
            // Se é apenas data, adiciona horário
            return dataVigencia + "T00:00:00.000";
        } catch (Exception e) {
            log.warn("⚠️ [CONVERSÃO] Erro ao formatar dataVigencia '{}': {}", dataVigencia, e.getMessage());
            return java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        }
    }

    /**
     * CRIA COMISSIONAMENTOS USANDO DADOS DA VIEW
     */
    private List<EmpresaAtivacaoPlanoRequest.Comissionamento> criarComissionamentos(IntegracaoOdontoprev dadosEmpresa) {
        List<EmpresaAtivacaoPlanoRequest.Comissionamento> comissionamentos = new ArrayList<>();
        
        // Verificar se há dados de comissionamento na view
        if (dadosEmpresa.getCnpjCorretor() != null && !dadosEmpresa.getCnpjCorretor().trim().isEmpty()) {
            comissionamentos.add(EmpresaAtivacaoPlanoRequest.Comissionamento.builder()
                    .cnpjCorretor(dadosEmpresa.getCnpjCorretor())
                    .codigoRegra(dadosEmpresa.getCodigoRegra() != null ? dadosEmpresa.getCodigoRegra().intValue() : 1)
                    .numeroParcelaDe(dadosEmpresa.getNumeroParcelaDe() != null ? dadosEmpresa.getNumeroParcelaDe().intValue() : 1)
                    .numeroParcelaAte(dadosEmpresa.getNumeroParcelaAte() != null ? dadosEmpresa.getNumeroParcelaAte().intValue() : 12)
                    .porcentagem(dadosEmpresa.getPorcentagem() != null ? dadosEmpresa.getPorcentagem().intValue() : 0)
                    .build());
        } else {
            // Comissionamento padrão se não houver dados na view
            comissionamentos.add(EmpresaAtivacaoPlanoRequest.Comissionamento.builder()
                    .cnpjCorretor("00000000000000")
                    .codigoRegra(1)
                    .numeroParcelaDe(1)
                    .numeroParcelaAte(12)
                    .porcentagem(0)
                    .build());
        }
        
        return comissionamentos;
    }

    /**
     * CRIA CONTATOS USANDO DADOS DA VIEW
     */
    private List<EmpresaAtivacaoPlanoRequest.Contato> criarContatos(IntegracaoOdontoprev dadosEmpresa) {
        List<EmpresaAtivacaoPlanoRequest.Contato> contatos = new ArrayList<>();
        
        // Verificar se há dados de contato na view
        if (dadosEmpresa.getNomeContato() != null && !dadosEmpresa.getNomeContato().trim().isEmpty()) {
            contatos.add(EmpresaAtivacaoPlanoRequest.Contato.builder()
                    .cargo(dadosEmpresa.getCargoContato() != null ? dadosEmpresa.getCargoContato() : "Gerente")
                    .nome(dadosEmpresa.getNomeContato())
                    .email(dadosEmpresa.getEmailContato() != null ? dadosEmpresa.getEmailContato() : dadosEmpresa.getEmail())
                    .idCorretor("N")
                    .telefone(EmpresaAtivacaoPlanoRequest.Telefone.builder()
                            .telefone1("(32) 99999-9999")
                            .celular("(32) 99999-9999")
                            .build())
                    .listaTipoComunicacao(List.of(
                            EmpresaAtivacaoPlanoRequest.TipoComunicacao.builder()
                                    .id("1")
                                    .descricao("E-mail")
                                    .build()
                    ))
                    .build());
        } else {
            // Contato padrão se não houver dados na view
            contatos.add(EmpresaAtivacaoPlanoRequest.Contato.builder()
                    .cargo("Gerente")
                    .nome("Contato Principal")
                    .email("contato@empresa.com")
                    .idCorretor("N")
                    .telefone(EmpresaAtivacaoPlanoRequest.Telefone.builder()
                            .telefone1("(32) 99999-9999")
                            .celular("(32) 99999-9999")
                            .build())
                    .listaTipoComunicacao(List.of(
                            EmpresaAtivacaoPlanoRequest.TipoComunicacao.builder()
                                    .id("1")
                                    .descricao("E-mail")
                                    .build()
                    ))
                    .build());
        }
        
        return contatos;
    }
}
