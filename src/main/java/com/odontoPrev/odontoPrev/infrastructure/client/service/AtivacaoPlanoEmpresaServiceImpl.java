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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public void ativarPlanoEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        String codigoEmpresa = dadosEmpresa.getCodigoEmpresa();
        
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
            
            // Etapa 5: Processar sucesso
            processarSucessoAtivacao(controleSync, response);
            
            log.info("✅ [ATIVAÇÃO PLANO] Plano ativado com sucesso para empresa: {}", codigoEmpresa);
            
        } catch (Exception e) {
            log.error("❌ [ATIVAÇÃO PLANO] Erro ao ativar plano para empresa {}: {}", 
                    codigoEmpresa, e.getMessage(), e);
            
            // Registrar erro no controle
            processarErroAtivacao(codigoEmpresa, e.getMessage());
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

        // Valores padrão baseados no cURL fornecido
        EmpresaAtivacaoPlanoRequest request = EmpresaAtivacaoPlanoRequest.builder()
                .sistema("SabinSinai")
                .tipoPessoa("J")
                .emiteCarteirinhaPlastica("N")
                .codigoEmpresaGestora(1)
                .codigoFilialEmpresaGestora(1)
                .codigoGrupoGerencial("787392")
                .codigoNaturezaJuridica("6550-2")
                .nomeNaturezaJuridica("Planos de saúde")
                .situacaoCadastral("ATIVO")
                .inscricaoMunicipal("997.179.737.204") // Valor padrão do cURL
                .inscricaoEstadual("997.179.737.204") // Valor padrão do cURL
                .dataConstituicao("2025-10-01T00:00:00.000Z")
                .renovacaoAutomatica("S")
                .codigoClausulaReajusteDiferenciado("1")
                .departamento("SEM DEPARTAMENTO")
                .dependentePaga("N")
                .permissaoCadastroDep(true)
                .modeloCobrancaVarejo(false)
                .numeroMinimoAssociados(3)
                .numeroFuncionarios(0)
                .numeroDepedentes(0)
                .idadeLimiteDependente(21)
                .valorFator(1)
                .tipoRetornoCritica("T")
                .codigoLayoutCarteirinha("B")
                .codigoOrdemCarteira(3)
                .codigoDocumentoContrato(0)
                .codigoCelula(9)
                .codigoMarca(1)
                .codigoDescricaoNF(0)
                .diaVencimentoAg(19)
                .codigoPerfilClienteFatura(3)
                .codigoBancoFatura("085 ")
                .multaFatura(0)
                .descontaIR("N")
                .retencaoIss("N")
                .liberaSenhaInternet("S")
                .faturamentoNotaCorte("N")
                .proRata("N")
                .custoFamiliar("S")
                .planoFamiliar("S")
                .percSinistroContrato(60)
                .idadeLimiteUniversitaria(24)
                .percentualINSSAutoGestao(0)
                .percentualMateriaisAutoGestao(0)
                .valorSinistroContrato(60.0)
                .percentualAssociado(0)
                .codigoRegiao(0)
                .codigoImagemFatura(1)
                .codigoMoeda("7")
                .codigoParceriaEstrategica(0)
                .sinistralidade(60)
                .posicaoIniTIT(1)
                .posicaoFimTIT(7)
                .regraDowngrade(0)
                .mesCompetenciaProximoFaturamento("09")
                .codigoUsuarioFaturamento("")
                .codigoUsuarioCadastro("")
                .ramo("Massificado")
                .cgc(dadosEmpresa.getCnpj()) // Usar cnpj da entidade
                .razaoSocial(dadosEmpresa.getNomeFantasia()) // Usar nomeFantasia da entidade
                .nomeFantasia(dadosEmpresa.getNomeFantasia())
                .diaInicioFaturamento(20)
                .codigoUsuarioConsultor("FEODPV01583")
                .mesAniversarioReajuste(7)
                .dataInicioContrato("2025-07-17T03:00:00.000")
                .dataVigencia("2025-07-17T03:00:00.000")
                .descricaoRamoAtividade("Saúde Suplementar")
                .diaVencimento(15)
                .cnae("6550-2/00")
                .codigoManual("1 ")
                .diaLimiteConsumoAg(19)
                .email("diretoria@sabinjf.com.br") // Valor padrão do cURL
                .diaMovAssociadoEmpresa(15)
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

        // Configurar contatos
        List<EmpresaAtivacaoPlanoRequest.Contato> contatos = List.of(
                EmpresaAtivacaoPlanoRequest.Contato.builder()
                        .cargo("Diretor Adjunto de Novos Projetos")
                        .nome("Gustavo de Moraes Ramalho")
                        .email("gustavoramalho@hospitalmontesinai.com.br")
                        .idCorretor("N")
                        .telefone(EmpresaAtivacaoPlanoRequest.Telefone.builder()
                                .telefone1("(11) 1111-1111")
                                .celular("(11) 11111-1111")
                                .build())
                        .listaTipoComunicacao(List.of(
                                EmpresaAtivacaoPlanoRequest.TipoComunicacao.builder()
                                        .id("1")
                                        .descricao("E-mail")
                                        .build()))
                        .build()
        );
        request.setContatos(contatos);

        // Configurar comissionamentos
        List<EmpresaAtivacaoPlanoRequest.Comissionamento> comissionamentos = List.of(
                EmpresaAtivacaoPlanoRequest.Comissionamento.builder()
                        .cnpjCorretor("27833136000139")
                        .codigoRegra(1)
                        .numeroParcelaDe(1)
                        .numeroParcelaAte(999)
                        .porcentagem(10)
                        .build()
        );
        request.setComissionamentos(comissionamentos);

        // Configurar grupos
        List<EmpresaAtivacaoPlanoRequest.Grupo> grupos = List.of(
                EmpresaAtivacaoPlanoRequest.Grupo.builder()
                        .codigoGrupo(268)
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
}
