package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.BeneficiarioOdontoprevRepository;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoBeneficiarioService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.BeneficiarioInclusaoResponse;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.PROCESSAMENTO_BENEFICIARIO;
import static com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException.TipoOperacao.INCLUSAO;

/**
 * IMPLEMENTAÇÃO DO SERVIÇO DE PROCESSAMENTO DE BENEFICIÁRIOS
 *
 * Realiza o processamento completo de inclusão de beneficiários na OdontoPrev,
 * incluindo validação, comunicação com API, atualização de status e execução
 * da procedure no sistema Tasy.
 *
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. Validação de dados obrigatórios
 * 2. Conversão de entidade para DTO de request
 * 3. Chamada para API da OdontoPrev
 * 4. Processamento da resposta (cdAssociado)
 * 5. Atualização do status no banco
 * 6. Execução da procedure no Tasy
 * 7. Registro de logs de auditoria
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoBeneficiarioServiceImpl implements ProcessamentoBeneficiarioService {

    private final BeneficiarioOdontoprevFeignClient odontoprevClient;
    private final BeneficiarioOdontoprevRepository beneficiarioRepository;
    private final ControleSyncBeneficiarioRepository controleSyncRepository;
    private final JdbcTemplate jdbcTemplate;
    private final OdontoprevApiHeaderService headerService;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    /**
     * PROCESSA INCLUSÃO DE UM ÚNICO BENEFICIÁRIO
     *
     * Executa todo o fluxo de inclusão com tratamento completo de erros
     * e atualização de status conforme resultado da operação.
     */
    @Override
    @Transactional
    @MonitorarOperacao(
            operacao = "PROCESSAR_INCLUSAO_BENEFICIARIO",
            incluirParametros = {"codigoMatricula"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public void processarInclusaoBeneficiario(BeneficiarioOdontoprev beneficiario) {
        String codigoMatricula = beneficiario.getCodigoMatricula();
        ControleSyncBeneficiario controleSync = null;

        try {
            // Etapa 1: Validação de dados obrigatórios
            if (!validarDadosObrigatorios(beneficiario)) {
                String mensagem = "Beneficiário possui dados obrigatórios ausentes ou inválidos";
                registrarTentativaErro(beneficiario, "INCLUSAO", null, mensagem, null);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, INCLUSAO);
            }

            // Etapa 2: Conversão para DTO de request
            BeneficiarioInclusaoRequest request = converterParaInclusaoRequest(beneficiario);

            // Etapa 3: Criar registro de controle ANTES de chamar a API
            controleSync = criarRegistroControle(beneficiario, "INCLUSAO", request);

            // Etapa 4: Chamada para API da OdontoPrev
            log.info("Enviando beneficiário {} para inclusão na OdontoPrev", codigoMatricula);

            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;

            BeneficiarioInclusaoResponse response = odontoprevClient.incluirBeneficiario(
                    authorization,
                    headerService.getEmpresa(),
                    headerService.getUsuario(),
                    headerService.getSenha(),
                    headerService.getAppId(),
                    request
            );

            // Etapa 5: Processamento da resposta
            if (!StringUtils.hasText(response.getCdAssociado())) {
                String mensagem = "OdontoPrev não retornou código do associado (cdAssociado)";
                registrarTentativaErro(beneficiario, "INCLUSAO", controleSync, mensagem, null);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, INCLUSAO);
            }

            // Etapa 6: Atualização do status no banco
            beneficiarioRepository.atualizarCdAssociado(
                    beneficiario.getId(),
                    response.getCdAssociado(),
                    "SINCRONIZADO",
                    LocalDateTime.now()
            );

            // Etapa 7: Execução da procedure no Tasy
            executarProcedureTasy(beneficiario, response.getCdAssociado());

            // Etapa 8: Registrar sucesso no controle
            registrarTentativaSucesso(controleSync, objectMapper.writeValueAsString(response));

            log.info("Beneficiário {} processado com sucesso. CdAssociado: {}",
                    codigoMatricula, response.getCdAssociado());

        } catch (Exception e) {
            // Tratamento de erro abrangente
            String mensagem = "Erro durante processamento de inclusão: " + e.getMessage();
            registrarTentativaErro(beneficiario, "INCLUSAO", controleSync, mensagem, e);
        }
    }

    /**
     * VALIDA CAMPOS OBRIGATÓRIOS PARA INCLUSÃO DE BENEFICIÁRIO
     *
     * Este método valida todos os campos obrigatórios conforme a
     * documentação oficial da API OdontoPrev (endpoint /incluir).
     *
     * REFERÊNCIA DA DOCUMENTAÇÃO (documentacao.md):
     * - Linha 306: codigoMatricula (Sim)
     * - Linha 308: cpf (Sim)
     * - Linha 309: dataDeNascimento (Sim)
     * - Linha 310: dtVigenciaRetroativa (Sim)
     * - Linha 335-342: endereco - cep, cidade, logradouro, numero, uf (Sim)
     * - Linha 321: nomeBeneficiario (Sim)
     * - Linha 322: nomeDaMae (Sim)
     * - Linha 327: sexo (Sim)
     * - Linha 329: telefoneCelular (Sim)
     * - Linha 331: telefoneResidencial (Não) - CAMPO OPCIONAL
     * - Linha 361-363: codigoEmpresa, codigoPlano, departamento (Sim)
     *
     * IMPORTANTE: telefoneResidencial NÃO é obrigatório segundo a documentação
     */
    @Override
    @MonitorarOperacao(
            operacao = "VALIDAR_DADOS_OBRIGATORIOS_BENEFICIARIO",
            incluirParametros = {"codigoMatricula"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public boolean validarDadosObrigatorios(BeneficiarioOdontoprev beneficiario) {
        // Validação de campos obrigatórios básicos (documentação linhas 306-310)
        // codigoMatricula, cpf, dataDeNascimento, dtVigenciaRetroativa
        if (!StringUtils.hasText(beneficiario.getCodigoMatricula()) ||
            !StringUtils.hasText(beneficiario.getCpf()) ||
            beneficiario.getDataNascimento() == null ||
            beneficiario.getDtVigenciaRetroativa() == null) {
            log.debug("Validação falhou: campos básicos ausentes (matrícula, CPF, datas)");
            return false;
        }

        // Validação de endereço obrigatório (documentação linhas 335-342)
        // Todos os campos de endereço são obrigatórios: cep, cidade, logradouro, numero, uf
        if (!StringUtils.hasText(beneficiario.getCep()) ||
            !StringUtils.hasText(beneficiario.getCidade()) ||
            !StringUtils.hasText(beneficiario.getLogradouro()) ||
            !StringUtils.hasText(beneficiario.getNumero()) ||
            !StringUtils.hasText(beneficiario.getUf())) {
            log.debug("Validação falhou: dados de endereço incompletos");
            return false;
        }

        // Validação de dados pessoais obrigatórios (documentação linhas 321-327)
        // nomeBeneficiario, nomeDaMae, sexo
        if (!StringUtils.hasText(beneficiario.getNomeBeneficiario()) ||
            !StringUtils.hasText(beneficiario.getNomeMae()) ||
            !StringUtils.hasText(beneficiario.getSexo())) {
            log.debug("Validação falhou: dados pessoais incompletos (nome, nome mãe, sexo)");
            return false;
        }

        // Validação de contato obrigatório - APENAS CELULAR É OBRIGATÓRIO
        // Documentação linha 329: telefoneCelular (Sim)
        // Documentação linha 331: telefoneResidencial (Não) - CAMPO OPCIONAL
        if (!StringUtils.hasText(beneficiario.getTelefoneCelular())) {
            log.debug("Validação falhou: telefone celular ausente");
            return false;
        }
        // REMOVIDO: validação de telefoneResidencial pois é OPCIONAL

        // Validação de vinculação empresarial obrigatória (documentação linhas 361-363)
        // codigoEmpresa, codigoPlano, departamento - todos dentro do objeto "venda"
        if (!StringUtils.hasText(beneficiario.getCodigoEmpresa()) ||
            !StringUtils.hasText(beneficiario.getCodigoPlano()) ||
            !StringUtils.hasText(beneficiario.getDepartamento())) {
            log.debug("Validação falhou: dados empresariais incompletos (empresa, plano, departamento)");
            return false;
        }

        log.debug("Validação concluída com sucesso para beneficiário {}",
                 beneficiario.getCodigoMatricula());
        return true;
    }

    /**
     * CONVERTE ENTIDADE PARA DTO DE REQUEST
     *
     * Mapeia todos os campos da entidade para o formato esperado pela API.
     */
    private BeneficiarioInclusaoRequest converterParaInclusaoRequest(BeneficiarioOdontoprev beneficiario) {
        return BeneficiarioInclusaoRequest.builder()
                // Campos obrigatórios
                .codigoMatricula(beneficiario.getCodigoMatricula())
                .cpf(beneficiario.getCpf())
                .dataNascimento(beneficiario.getDataNascimento().toString())
                .dtVigenciaRetroativa(beneficiario.getDtVigenciaRetroativa().toString())
                .cep(beneficiario.getCep())
                .cidade(beneficiario.getCidade())
                .logradouro(beneficiario.getLogradouro())
                .numero(beneficiario.getNumero())
                .uf(beneficiario.getUf())
                .nomeBeneficiario(beneficiario.getNomeBeneficiario())
                .nomeDaMae(beneficiario.getNomeMae())
                .sexo(beneficiario.getSexo())
                .telefoneCelular(beneficiario.getTelefoneCelular())
                .telefoneResidencial(beneficiario.getTelefoneResidencial())
                .usuario("PONTETECH") // Valor fixo conforme especificação
                .codigoEmpresa(beneficiario.getCodigoEmpresa())
                .codigoPlano(beneficiario.getCodigoPlano())
                .departamento(beneficiario.getDepartamento())
                // Campos opcionais
                .rg(beneficiario.getRg())
                .estadoCivil(beneficiario.getEstadoCivil())
                .nmCargo(beneficiario.getNmCargo())
                .grauParentesco(beneficiario.getGrauParentesco())
                .pisPasep(beneficiario.getPisPasep())
                .bairro(beneficiario.getBairro())
                .email(beneficiario.getEmail())
                .complemento(beneficiario.getComplemento())
                .rgEmissor(beneficiario.getRgEmissor())
                .cns(beneficiario.getCns())
                .build();
    }

    /**
     * EXECUTA PROCEDURE NO SISTEMA TASY
     *
     * Chama a procedure SS_PLS_CAD_CARTEIRINHA_ODONTOPREV para registrar
     * o código do associado no sistema Tasy.
     */
    @MonitorarOperacao(
            operacao = "EXECUTAR_PROCEDURE_CARTEIRINHA_TASY",
            incluirParametros = {"cdAssociado"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    private void executarProcedureTasy(BeneficiarioOdontoprev beneficiario, String cdAssociado) {
        try {
            String sql = """
                BEGIN
                  SS_PLS_CAD_CARTEIRINHA_ODONTOPREV(
                    p_nr_seq_segurado => ?,
                    p_cd_cgc_estipulante => ?,
                    p_cd_associado => ?
                  );
                END;
                """;

            jdbcTemplate.update(sql,
                    beneficiario.getNrSequencia(),
                    beneficiario.getCdCgcEstipulante(),
                    cdAssociado);

            log.info("Procedure executada com sucesso para beneficiário {} com cdAssociado {}",
                    beneficiario.getCodigoMatricula(), cdAssociado);

        } catch (Exception e) {
            log.error("Erro ao executar procedure para beneficiário {}: {}",
                    beneficiario.getCodigoMatricula(), e.getMessage(), e);
            throw new ProcessamentoBeneficiarioException(
                    "Falha na execução da procedure no Tasy: " + e.getMessage(),
                    beneficiario.getCodigoMatricula(),
                    INCLUSAO,
                    e
            );
        }
    }

    /**
     * CRIA REGISTRO DE CONTROLE DE SINCRONIZAÇÃO
     *
     * Registra a tentativa de sincronização antes de chamar a API.
     */
    @MonitorarOperacao(
            operacao = "CRIAR_REGISTRO_CONTROLE_SYNC",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    private ControleSyncBeneficiario criarRegistroControle(
            BeneficiarioOdontoprev beneficiario,
            String tipoOperacao,
            Object payload) {

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            ControleSyncBeneficiario controle = ControleSyncBeneficiario.builder()
                    .codigoEmpresa(beneficiario.getCodigoEmpresa())
                    .codigoBeneficiario(beneficiario.getCodigoMatricula())
                    .tipoLog(tipoOperacao.substring(0, 1)) // I, A, E
                    .tipoOperacao(tipoOperacao)
                    .endpointDestino("/incluir")
                    .dadosJson(payloadJson)
                    .statusSync("PENDING")
                    .tentativas(1)
                    .maxTentativas(3)
                    .dataUltimaTentativa(LocalDateTime.now())
                    .build();

            return controleSyncRepository.save(controle);

        } catch (Exception e) {
            log.error("Erro ao criar registro de controle para beneficiário {}: {}",
                    beneficiario.getCodigoMatricula(), e.getMessage(), e);
            // Retorna null para não bloquear o processamento
            return null;
        }
    }

    /**
     * REGISTRA TENTATIVA DE SINCRONIZAÇÃO COM SUCESSO
     *
     * Atualiza o registro de controle marcando como SUCCESS.
     */
    @MonitorarOperacao(
            operacao = "REGISTRAR_TENTATIVA_SUCESSO",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    private void registrarTentativaSucesso(ControleSyncBeneficiario controle, String responseApi) {
        if (controle == null) {
            return;
        }

        try {
            controleSyncRepository.marcarComoSucesso(
                    controle.getId(),
                    LocalDateTime.now(),
                    responseApi
            );
            log.debug("Registro de controle {} marcado como SUCCESS", controle.getId());
        } catch (Exception e) {
            log.error("Erro ao registrar sucesso no controle: {}", e.getMessage(), e);
        }
    }

    /**
     * REGISTRA TENTATIVA DE SINCRONIZAÇÃO COM ERRO
     *
     * Cria ou atualiza registro de controle com status de erro.
     */
    @MonitorarOperacao(
            operacao = "REGISTRAR_TENTATIVA_ERRO",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    private void registrarTentativaErro(
            BeneficiarioOdontoprev beneficiario,
            String tipoOperacao,
            ControleSyncBeneficiario controle,
            String mensagemErro,
            Exception exception) {

        try {
            if (controle == null) {
                // Se não existe controle, cria um novo com erro
                String payloadJson = "{}";
                controle = ControleSyncBeneficiario.builder()
                        .codigoEmpresa(beneficiario.getCodigoEmpresa())
                        .codigoBeneficiario(beneficiario.getCodigoMatricula())
                        .tipoLog(tipoOperacao.substring(0, 1))
                        .tipoOperacao(tipoOperacao)
                        .endpointDestino("/incluir")
                        .dadosJson(payloadJson)
                        .statusSync("ERROR")
                        .tentativas(1)
                        .maxTentativas(3)
                        .dataUltimaTentativa(LocalDateTime.now())
                        .erroMensagem(mensagemErro)
                        .build();

                controleSyncRepository.save(controle);
            } else {
                // Atualiza controle existente
                controleSyncRepository.atualizarAposTentativa(
                        controle.getId(),
                        "ERROR",
                        controle.getTentativas() + 1,
                        LocalDateTime.now(),
                        mensagemErro,
                        exception != null ? exception.getClass().getName() : null
                );
            }

            log.debug("Erro registrado no controle de sync para beneficiário {}: {}",
                    beneficiario.getCodigoMatricula(), mensagemErro);

        } catch (Exception e) {
            log.error("Erro ao registrar erro no controle de sync: {}", e.getMessage(), e);
        }
    }
}
