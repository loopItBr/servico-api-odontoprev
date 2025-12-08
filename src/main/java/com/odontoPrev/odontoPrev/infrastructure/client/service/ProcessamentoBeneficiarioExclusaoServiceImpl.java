package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.entity.BeneficiarioOdontoprev;
import com.odontoPrev.odontoPrev.domain.entity.ControleSyncBeneficiario;
import com.odontoPrev.odontoPrev.domain.repository.ControleSyncBeneficiarioRepository;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoBeneficiarioExclusaoService;
import com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.AssociadoInativacao;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresarialModelInativacao;
import com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collections;

import static com.odontoPrev.odontoPrev.infrastructure.aop.MonitorarOperacao.TipoExcecao.PROCESSAMENTO_BENEFICIARIO;
import static com.odontoPrev.odontoPrev.infrastructure.exception.ProcessamentoBeneficiarioException.TipoOperacao.EXCLUSAO;

/**
 * IMPLEMENTAÇÃO DO SERVIÇO DE PROCESSAMENTO DE EXCLUSÃO/INATIVAÇÃO DE BENEFICIÁRIOS
 *
 * Realiza o processamento completo de inativação de beneficiários na OdontoPrev
 * quando têm status Rescindido/Suspenso no sistema Tasy.
 *
 * FLUXO COMPLETO DE PROCESSAMENTO:
 * 1. Validação de pré-requisitos (beneficiário deve existir na OdontoPrev)
 * 2. Validação de campos obrigatórios para inativação
 * 3. Conversão de entidade para DTO de inativação
 * 4. Chamada para API da OdontoPrev (POST /inativarAssociadoEmpresarial)
 * 5. Atualização do status no banco
 * 6. Registro de logs de auditoria
 *
 * CARACTERÍSTICAS DA INATIVAÇÃO:
 * - Beneficiário deve ter cdAssociado preenchido
 * - Pode ser executada múltiplas vezes (idempotente)
 * - OdontoPrev trata duplicação de registros
 * - Não há retorno específico da API (void response)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoBeneficiarioExclusaoServiceImpl implements ProcessamentoBeneficiarioExclusaoService {

    private final BeneficiarioOdontoprevFeignClient odontoprevClient;
    private final ControleSyncBeneficiarioRepository controleSyncRepository;
    private final BeneficiarioTokenService beneficiarioTokenService;
    private final ObjectMapper objectMapper;

    @Value("${odontoprev.api.login.usuario}")
    private String cdUsuario;

    /**
     * PROCESSA INATIVAÇÃO DE UM ÚNICO BENEFICIÁRIO
     *
     * Executa todo o fluxo de inativação com validação e tratamento de erros.
     */
    @Override
    @Transactional
    @MonitorarOperacao(
            operacao = "PROCESSAR_INATIVACAO_BENEFICIARIO",
            incluirParametros = {"codigoMatricula", "cdAssociado"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public void processarInativacaoBeneficiario(BeneficiarioOdontoprev beneficiario) {
        String codigoMatricula = beneficiario.getCodigoMatricula();
        String cdAssociado = beneficiario.getCdAssociado();
        ControleSyncBeneficiario controleSync = null;

        try {
            // Etapa 1: Validação de pré-requisitos
            if (!validarBeneficiarioParaInativacao(beneficiario)) {
                String mensagem = "Beneficiário não atende pré-requisitos para inativação";
                atualizarStatusErro(beneficiario, mensagem, null);
                throw new ProcessamentoBeneficiarioException(mensagem, codigoMatricula, EXCLUSAO);
            }

            // Etapa 2: Conversão para EmpresarialModel
            EmpresarialModelInativacao empresarialModel = converterParaEmpresarialModel(beneficiario);

            // Etapa 3: Criar ou atualizar registro de controle
            log.info("🔍 [EXCLUSÃO] Verificando se já existe registro de controle para beneficiário {}", codigoMatricula);
            controleSync = criarOuAtualizarRegistroControle(beneficiario, empresarialModel);

            // Etapa 4: Serializar EmpresarialModel para JSON string
            String empresarialModelJson = objectMapper.writeValueAsString(empresarialModel);

            // Etapa 5: Obter tokens para autenticação dupla
            String[] tokens = beneficiarioTokenService.obterTokensCompletos();
            String tokenOAuth2 = tokens[0];
            String tokenLoginEmpresa = tokens[1];
            
            log.info("🔑 [EXCLUSÃO] Tokens obtidos - OAuth2: {}..., LoginEmpresa: {}...",
                    tokenOAuth2.substring(0, Math.min(20, tokenOAuth2.length())),
                    tokenLoginEmpresa.substring(0, Math.min(20, tokenLoginEmpresa.length())));

            // Etapa 6: Chamada para API da OdontoPrev
            log.info("🚀 [EXCLUSÃO] Enviando inativação do beneficiário {} (cdAssociado: {}) para OdontoPrev",
                    codigoMatricula, cdAssociado);

            long inicioChamada = System.currentTimeMillis();
            odontoprevClient.inativarBeneficiario(
                    tokenOAuth2,
                    tokenLoginEmpresa,
                    empresarialModelJson
            );
            long tempoResposta = System.currentTimeMillis() - inicioChamada;
            
            log.info("✅ [EXCLUSÃO] Inativação do beneficiário {} processada com sucesso em {}ms", 
                    codigoMatricula, tempoResposta);

            // Etapa 7: Atualização do status no banco
            atualizarStatusSucesso(beneficiario, controleSync);

        } catch (Exception e) {
            // Tratamento de erro abrangente
            String mensagem = "Erro durante processamento de inativação: " + e.getMessage();
            atualizarStatusErro(beneficiario, mensagem, controleSync);

            // Relança exceção para ser tratada pelo Global Exception Handler
            throw new ProcessamentoBeneficiarioException(
                    mensagem,
                    codigoMatricula,
                    EXCLUSAO,
                    e
            );
        }
    }

    /**
     * VALIDA SE BENEFICIÁRIO PODE SER INATIVADO
     *
     * Verifica pré-requisitos e campos obrigatórios para inativação.
     */
    @Override
    @MonitorarOperacao(
            operacao = "VALIDAR_BENEFICIARIO_PARA_INATIVACAO",
            incluirParametros = {"codigoMatricula"},
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    public boolean validarBeneficiarioParaInativacao(BeneficiarioOdontoprev beneficiario) {
        // Pré-requisito 1: Deve ter cdAssociado (já existe na OdontoPrev)
        if (!StringUtils.hasText(beneficiario.getCdAssociado())) {
            log.warn("Beneficiário {} não pode ser inativado: cdAssociado não informado",
                    beneficiario.getCodigoMatricula());
            return false;
        }

        // Pré-requisito 2: Campos obrigatórios para inativação
        if (!StringUtils.hasText(beneficiario.getCodigoEmpresa()) ||
            !StringUtils.hasText(beneficiario.getCodigoMatricula()) ||
            !StringUtils.hasText(beneficiario.getNomeBeneficiario())) {
            log.warn("Beneficiário {} não pode ser inativado: campos obrigatórios ausentes",
                    beneficiario.getCodigoMatricula());
            return false;
        }

        // Pré-requisito 3: Deve ter motivo de inativação
        if (beneficiario.getIdMotivoInativacao() == null || beneficiario.getIdMotivoInativacao() <= 0) {
            log.warn("Beneficiário {} não pode ser inativado: motivo de inativação não informado",
                    beneficiario.getCodigoMatricula());
            return false;
        }

        return true;
    }

    /**
     * CONVERTE ENTIDADE PARA EMPRESARIAL MODEL DE INATIVAÇÃO
     *
     * Mapeia campos da entidade para o formato EmpresarialModel esperado
     * pela API de inativação (multipart/form-data).
     *
     * ESTRUTURA CRIADA:
     * {
     *   "cdEmpresa": "787392",
     *   "cdUsuario": "13433638",
     *   "associado": [{
     *     "cdMatricula": "...",
     *     "cdAssociado": "...",
     *     "nome": "...",
     *     "email": "...",
     *     "idMotivo": "25"
     *   }],
     *   "dataInativacao": "2024-12-29"
     * }
     */
    private EmpresarialModelInativacao converterParaEmpresarialModel(BeneficiarioOdontoprev beneficiario) {
        // Cria o objeto AssociadoInativacao
        AssociadoInativacao associado = AssociadoInativacao.builder()
                .cdMatricula(beneficiario.getCodigoMatricula())
                .cdAssociado(beneficiario.getCdAssociado())
                .nome(beneficiario.getNomeBeneficiario())
                .email(beneficiario.getEmail())
                .idMotivo("25") // Sempre 25 (numérico como string) - Iniciativa do beneficiário
                .build();

        // Determina data de inativação (usa data específica ou data atual)
        String dataInativacao = beneficiario.getDataInativacao() != null
                ? beneficiario.getDataInativacao().toString()
                : LocalDate.now().toString();

        // Cria o EmpresarialModel completo
        return EmpresarialModelInativacao.builder()
                .cdEmpresa(beneficiario.getCodigoEmpresa())
                .cdUsuario(cdUsuario) // Vem da configuração (ex: "13433638")
                .associado(Collections.singletonList(associado))
                .dataInativacao(dataInativacao)
                .build();
    }

    /**
     * CRIA OU ATUALIZA REGISTRO DE CONTROLE DE SINCRONIZAÇÃO
     * 
     * Verifica se já existe um registro de controle para este beneficiário.
     * Se existir, atualiza o registro existente.
     * Se não existir, cria um novo registro.
     */
    @MonitorarOperacao(
            operacao = "CRIAR_OU_ATUALIZAR_REGISTRO_CONTROLE_INATIVACAO",
            excecaoEmErro = PROCESSAMENTO_BENEFICIARIO
    )
    private ControleSyncBeneficiario criarOuAtualizarRegistroControle(
            BeneficiarioOdontoprev beneficiario,
            Object payload) {

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String codigoMatricula = beneficiario.getCodigoMatricula();
            String codigoEmpresa = beneficiario.getCodigoEmpresa();

            // Verificar se já existe um registro de controle para este beneficiário
            Optional<ControleSyncBeneficiario> controleExistente = controleSyncRepository
                    .findByCodigoEmpresaAndCodigoBeneficiarioAndTipoOperacao(
                            codigoEmpresa, codigoMatricula, "EXCLUSAO");

            ControleSyncBeneficiario controle;

            if (controleExistente.isPresent()) {
                // Atualizar registro existente
                controle = controleExistente.get();
                controle.setDadosJson(payloadJson);
                controle.setStatusSync("PENDING"); // PENDING = 7 caracteres (padrão para novos registros)
                controle.setTentativas(controle.getTentativas() + 1);
                controle.setDataUltimaTentativa(LocalDateTime.now());
                controle.setResponseApi(null); // Limpar resposta anterior
                controle.setErroMensagem(null); // Limpar erro anterior
                
                log.info("🔄 [CONTROLE] Atualizando registro existente para beneficiário {} - ID: {}, Tentativa: {}", 
                        codigoMatricula, controle.getId(), controle.getTentativas());
            } else {
                // Criar novo registro
                controle = ControleSyncBeneficiario.builder()
                        .codigoEmpresa(codigoEmpresa)
                        .codigoBeneficiario(codigoMatricula)
                        .tipoLog("E")
                        .tipoOperacao("EXCLUSAO")
                        .endpointDestino("/cadastroonline-pj/1.0/inativar")
                        .dadosJson(payloadJson)
                        .statusSync("PENDING") // PENDING = 7 caracteres (padrão para novos registros)
                        .tentativas(1)
                        .maxTentativas(3)
                        .dataUltimaTentativa(LocalDateTime.now())
                        .build();
                
                log.info("🆕 [CONTROLE] Criando novo registro de controle para beneficiário {}", codigoMatricula);
            }

            // Truncar campos antes de salvar
            if (controle.getCodigoEmpresa() != null && controle.getCodigoEmpresa().length() > 6) {
                controle.setCodigoEmpresa(controle.getCodigoEmpresa().substring(0, 6));
            }
            if (controle.getCodigoBeneficiario() != null && controle.getCodigoBeneficiario().length() > 15) {
                controle.setCodigoBeneficiario(controle.getCodigoBeneficiario().substring(0, 15));
            }
            if (controle.getTipoLog() != null && controle.getTipoLog().length() > 1) {
                controle.setTipoLog(controle.getTipoLog().substring(0, 1));
            }
            if (controle.getTipoOperacao() != null && controle.getTipoOperacao().length() > 10) {
                controle.setTipoOperacao(controle.getTipoOperacao().substring(0, 10));
            }
            if (controle.getEndpointDestino() != null && controle.getEndpointDestino().length() > 200) {
                controle.setEndpointDestino(controle.getEndpointDestino().substring(0, 200));
            }
            
            // Garantir que tentativas e maxTentativas sejam não-negativos
            if (controle.getTentativas() == null || controle.getTentativas() < 0) {
                controle.setTentativas(0);
            }
            if (controle.getMaxTentativas() == null || controle.getMaxTentativas() < 0) {
                controle.setMaxTentativas(3);
            }
            
            // Garantir que statusSync seja válido
            if (controle.getStatusSync() != null && controle.getStatusSync().length() > 10) {
                controle.setStatusSync(controle.getStatusSync().substring(0, 10));
            }
            
            ControleSyncBeneficiario controleSalvo = controleSyncRepository.saveAndFlush(controle);
            log.info("✅ [CONTROLE] Registro de controle processado - ID: {}, Status: {}, Tipo: {}", 
                    controleSalvo.getId(), controleSalvo.getStatusSync(),
                    controleSalvo.getTentativas() > 1 ? "ATUALIZAÇÃO" : "CRIAÇÃO");
            return controleSalvo;

        } catch (Exception e) {
            log.error("❌ [CONTROLE] Erro ao criar/atualizar registro de controle para beneficiário {}: {}",
                    beneficiario.getCodigoMatricula(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * REGISTRA TENTATIVA DE SUCESSO
     *
     * Atualiza o registro de controle com o resultado de sucesso.
     * IMPORTANTE: Usa saveAndFlush para garantir que o commit seja imediato
     * e visível para outras threads/processos, evitando duplicação.
     * 
     * CRÍTICO: Se a transação atual estiver marcada como rollback-only,
     * tenta buscar o registro novamente e atualizar em uma nova transação.
     */
    private void atualizarStatusSucesso(BeneficiarioOdontoprev beneficiario, ControleSyncBeneficiario controle) {
        if (controle == null) {
            log.warn("⚠️ [TBSYNC] Tentativa de registrar sucesso em controle nulo");
            return;
        }
        
        if (controle.getId() == null) {
            log.error("❌ [TBSYNC] Controle não possui ID, não é possível atualizar - Matrícula: {}", 
                    controle.getCodigoBeneficiario());
            return;
        }
        
        try {
            // Truncar campos antes de salvar
            if (controle.getCodigoEmpresa() != null && controle.getCodigoEmpresa().length() > 6) {
                controle.setCodigoEmpresa(controle.getCodigoEmpresa().substring(0, 6));
            }
            if (controle.getCodigoBeneficiario() != null && controle.getCodigoBeneficiario().length() > 15) {
                controle.setCodigoBeneficiario(controle.getCodigoBeneficiario().substring(0, 15));
            }
            if (controle.getTipoLog() != null && controle.getTipoLog().length() > 1) {
                controle.setTipoLog(controle.getTipoLog().substring(0, 1));
            }
            if (controle.getTipoOperacao() != null && controle.getTipoOperacao().length() > 10) {
                controle.setTipoOperacao(controle.getTipoOperacao().substring(0, 10));
            }
            
            // Garantir que tentativas e maxTentativas sejam não-negativos
            if (controle.getTentativas() == null || controle.getTentativas() < 0) {
                controle.setTentativas(0);
            }
            if (controle.getMaxTentativas() == null || controle.getMaxTentativas() < 0) {
                controle.setMaxTentativas(3);
            }
            
            controle.setStatusSync("SUCCESS"); // SUCCESS = 7 caracteres (padrão em inglês)
            controle.setDataSucesso(LocalDateTime.now());
            controle.setResponseApi("Inativação realizada com sucesso");
            
            // IMPORTANTE: Usar saveAndFlush para garantir commit imediato e visibilidade
            controleSyncRepository.saveAndFlush(controle);
            log.info("✅ [TBSYNC] Status do beneficiário {} atualizado para SUCCESS no controle de sincronização - ID: {}", 
                    beneficiario.getCodigoMatricula(), controle.getId());
        } catch (Exception e) {
            log.error("❌ [TBSYNC] Erro ao registrar sucesso no controle (primeira tentativa) - ID: {} | Matrícula: {} | Erro: {}", 
                    controle.getId(), controle.getCodigoBeneficiario(), e.getMessage(), e);
            
            // TENTATIVA DE RECUPERAÇÃO: Se a transação atual está rollback-only, buscar o registro novamente
            try {
                log.info("🔄 [TBSYNC] Tentando recuperar registro para atualização - ID: {} | Matrícula: {}", 
                        controle.getId(), controle.getCodigoBeneficiario());
                
                // Buscar o registro novamente do banco (fora da transação atual)
                var controleRecuperado = controleSyncRepository.findById(controle.getId());
                
                if (controleRecuperado.isPresent()) {
                    ControleSyncBeneficiario controleAtualizado = controleRecuperado.get();
                    controleAtualizado.setStatusSync("SUCCESS");
                    controleAtualizado.setDataSucesso(LocalDateTime.now());
                    controleAtualizado.setResponseApi("Inativação realizada com sucesso");
                    controleSyncRepository.saveAndFlush(controleAtualizado);
                    log.info("✅ [TBSYNC] Registro recuperado e atualizado com sucesso - ID: {} | Matrícula: {}", 
                            controleAtualizado.getId(), controleAtualizado.getCodigoBeneficiario());
                } else {
                    log.error("❌ [TBSYNC] Registro não encontrado no banco para recuperação - ID: {} | Matrícula: {}", 
                            controle.getId(), controle.getCodigoBeneficiario());
                }
            } catch (Exception recoveryException) {
                log.error("❌ [TBSYNC] Erro na tentativa de recuperação do registro - ID: {} | Matrícula: {} | Erro: {}", 
                        controle.getId(), controle.getCodigoBeneficiario(), recoveryException.getMessage(), recoveryException);
            }
        }
    }

    /**
     * REGISTRA TENTATIVA DE ERRO
     *
     * Atualiza o registro de controle com o resultado de erro.
     * IMPORTANTE: Garante que dadosJson esteja preenchido com o payload enviado.
     * CRÍTICO: Garante que todos os campos obrigatórios estejam preenchidos para evitar constraint violations.
     */
    private void atualizarStatusErro(BeneficiarioOdontoprev beneficiario, String mensagemErro, ControleSyncBeneficiario controle) {
        // VALIDAÇÕES CRÍTICAS: Garantir que todos os campos obrigatórios estejam preenchidos
        if (beneficiario == null) {
            log.error("❌ [TBSYNC] Beneficiário não pode ser nulo ao registrar erro");
            return;
        }
        
        String codigoEmpresa = beneficiario.getCodigoEmpresa();
        String codigoBeneficiario = beneficiario.getCodigoMatricula();
        
        if (codigoEmpresa == null || codigoEmpresa.trim().isEmpty()) {
            log.error("❌ [TBSYNC] Código da empresa não pode ser nulo ou vazio ao registrar erro - Matrícula: {}", codigoBeneficiario);
            return;
        }
        
        if (codigoBeneficiario == null || codigoBeneficiario.trim().isEmpty()) {
            log.error("❌ [TBSYNC] Código do beneficiário não pode ser nulo ou vazio ao registrar erro");
            return;
        }
        
        try {
            if (controle == null || controle.getId() == null) {
                // Se não existe controle, tenta criar o request para ter o JSON correto
                String payloadJson = "{}";
                try {
                    // Tenta criar o request mesmo com dados inválidos para ter o JSON
                    EmpresarialModelInativacao request = converterParaEmpresarialModel(beneficiario);
                    payloadJson = objectMapper.writeValueAsString(request);
                } catch (Exception e) {
                    log.debug("⚠️ Não foi possível criar request para beneficiário {}: {}", 
                             beneficiario.getCodigoMatricula(), e.getMessage());
                    // Mantém "{}" se não conseguir criar o request
                }
                
                // CRÍTICO: Validar e truncar campos para garantir que estejam dentro dos limites
                // codigoEmpresa: máximo 6 caracteres
                String codigoEmpresaTruncado = codigoEmpresa != null ? codigoEmpresa.substring(0, Math.min(6, codigoEmpresa.length())) : codigoEmpresa;
                
                // codigoBeneficiario: máximo 15 caracteres
                String codigoBeneficiarioTruncado = codigoBeneficiario != null ? codigoBeneficiario.substring(0, Math.min(15, codigoBeneficiario.length())) : codigoBeneficiario;
                
                // tipoLog: máximo 1 caractere (E = Exclusão)
                String tipoLogTruncado = "E";
                
                // tipoOperacao: máximo 10 caracteres
                String tipoOperacaoTruncado = "EXCLUSAO".substring(0, Math.min(10, "EXCLUSAO".length()));
                
                // endpointDestino: máximo 200 caracteres
                String endpointDestino = "/cadastroonline-pj/1.0/inativar";
                String endpointDestinoTruncado = endpointDestino.length() > 200 ? endpointDestino.substring(0, 200) : endpointDestino;
                
                log.info("📝 [TBSYNC-ERRO] VALIDAÇÃO DE CAMPOS - Empresa: '{}' ({} chars) | Beneficiário: '{}' ({} chars) | TipoLog: '{}' ({} chars) | TipoOp: '{}' ({} chars) | Status: 'ERROR' (5 chars)", 
                        codigoEmpresaTruncado, codigoEmpresaTruncado != null ? codigoEmpresaTruncado.length() : 0,
                        codigoBeneficiarioTruncado, codigoBeneficiarioTruncado != null ? codigoBeneficiarioTruncado.length() : 0,
                        tipoLogTruncado, tipoLogTruncado != null ? tipoLogTruncado.length() : 0,
                        tipoOperacaoTruncado, tipoOperacaoTruncado != null ? tipoOperacaoTruncado.length() : 0);
                
                controle = ControleSyncBeneficiario.builder()
                        .codigoEmpresa(codigoEmpresaTruncado)
                        .codigoBeneficiario(codigoBeneficiarioTruncado)
                        .tipoLog(tipoLogTruncado) // E = Exclusão
                        .tipoOperacao(tipoOperacaoTruncado)
                        .endpointDestino(endpointDestinoTruncado)
                        .dadosJson(payloadJson)
                        .statusSync("ERROR") // ERROR = 5 caracteres (padrão em inglês)
                        .tentativas(1)
                        .maxTentativas(3) // CRÍTICO: Garantir que maxTentativas não seja nulo
                        .erroMensagem(mensagemErro != null ? mensagemErro : "Erro desconhecido")
                        .dataUltimaTentativa(LocalDateTime.now())
                        .build();
            } else {
                // Truncar campos antes de atualizar
                if (controle.getCodigoEmpresa() != null && controle.getCodigoEmpresa().length() > 6) {
                    controle.setCodigoEmpresa(controle.getCodigoEmpresa().substring(0, 6));
                }
                if (controle.getCodigoBeneficiario() != null && controle.getCodigoBeneficiario().length() > 15) {
                    controle.setCodigoBeneficiario(controle.getCodigoBeneficiario().substring(0, 15));
                }
                if (controle.getTipoLog() != null && controle.getTipoLog().length() > 1) {
                    controle.setTipoLog(controle.getTipoLog().substring(0, 1));
                }
                if (controle.getTipoOperacao() != null && controle.getTipoOperacao().length() > 10) {
                    controle.setTipoOperacao(controle.getTipoOperacao().substring(0, 10));
                }
                if (controle.getEndpointDestino() != null && controle.getEndpointDestino().length() > 200) {
                    controle.setEndpointDestino(controle.getEndpointDestino().substring(0, 200));
                }
                
                // Garantir que tentativas e maxTentativas sejam não-negativos
                if (controle.getTentativas() == null || controle.getTentativas() < 0) {
                    controle.setTentativas(0);
                }
                if (controle.getMaxTentativas() == null || controle.getMaxTentativas() < 0) {
                    controle.setMaxTentativas(3);
                }
                
                controle.setStatusSync("ERROR"); // ERROR = 5 caracteres (padrão em inglês)
                controle.setDataUltimaTentativa(LocalDateTime.now());
                controle.setErroMensagem(mensagemErro != null ? mensagemErro : "Erro desconhecido");
                
                // INCREMENTAR TENTATIVAS: Se o controle já existe, incrementar número de tentativas
                int tentativasAtuais = controle.getTentativas() != null ? controle.getTentativas() : 0;
                controle.setTentativas(Math.max(0, tentativasAtuais + 1)); // Garantir que não seja negativo
            }

            // Log do nome da tabela que será usada
            log.error("🔍 [TBSYNC] Tentando salvar na tabela: TASY.TB_CONTROLE_SYNC_ODONTOPREV_BENEF");
            log.error("🔍 [TBSYNC] Nome da entidade: {}", controle.getClass().getSimpleName());
            log.error("🔍 [TBSYNC] Schema configurado: TASY");
            log.error("🔍 [TBSYNC] Nome da tabela configurado: TB_CONTROLE_SYNC_ODONTOPREV_BENEF");
            
            controleSyncRepository.saveAndFlush(controle);
            log.info("✅ [TBSYNC] Status do beneficiário {} atualizado para ERROR no controle de sincronização - ID: {} | Mensagem: {}", 
                    beneficiario.getCodigoMatricula(), controle.getId(), mensagemErro);
        } catch (Exception e) {
            log.error("❌ [TBSYNC] Erro ao registrar erro no controle (primeira tentativa) - Matrícula: {} | Erro: {}", 
                    codigoBeneficiario, e.getMessage(), e);
            
            // TENTATIVA DE RECUPERAÇÃO: Se a transação atual está rollback-only, buscar o registro novamente
            if (controle != null && controle.getId() != null) {
                try {
                    log.info("🔄 [TBSYNC] Tentando recuperar registro para atualização - ID: {} | Matrícula: {}", 
                            controle.getId(), codigoBeneficiario);
                    
                    // Buscar o registro novamente do banco (fora da transação atual)
                    var controleRecuperado = controleSyncRepository.findById(controle.getId());
                    
                    if (controleRecuperado.isPresent()) {
                        ControleSyncBeneficiario controleAtualizado = controleRecuperado.get();
                        
                        // Truncar campos
                        if (controleAtualizado.getCodigoEmpresa() != null && controleAtualizado.getCodigoEmpresa().length() > 6) {
                            controleAtualizado.setCodigoEmpresa(controleAtualizado.getCodigoEmpresa().substring(0, 6));
                        }
                        if (controleAtualizado.getCodigoBeneficiario() != null && controleAtualizado.getCodigoBeneficiario().length() > 15) {
                            controleAtualizado.setCodigoBeneficiario(controleAtualizado.getCodigoBeneficiario().substring(0, 15));
                        }
                        if (controleAtualizado.getTipoLog() != null && controleAtualizado.getTipoLog().length() > 1) {
                            controleAtualizado.setTipoLog(controleAtualizado.getTipoLog().substring(0, 1));
                        }
                        if (controleAtualizado.getTipoOperacao() != null && controleAtualizado.getTipoOperacao().length() > 10) {
                            controleAtualizado.setTipoOperacao(controleAtualizado.getTipoOperacao().substring(0, 10));
                        }
                        if (controleAtualizado.getEndpointDestino() != null && controleAtualizado.getEndpointDestino().length() > 200) {
                            controleAtualizado.setEndpointDestino(controleAtualizado.getEndpointDestino().substring(0, 200));
                        }
                        
                        // Garantir que tentativas e maxTentativas sejam não-negativos
                        if (controleAtualizado.getTentativas() == null || controleAtualizado.getTentativas() < 0) {
                            controleAtualizado.setTentativas(0);
                        }
                        if (controleAtualizado.getMaxTentativas() == null || controleAtualizado.getMaxTentativas() < 0) {
                            controleAtualizado.setMaxTentativas(3);
                        }
                        
                        controleAtualizado.setStatusSync("ERROR");
                        controleAtualizado.setDataUltimaTentativa(LocalDateTime.now());
                        controleAtualizado.setErroMensagem(mensagemErro != null ? mensagemErro : "Erro desconhecido");
                        
                        int tentativasAtuais = controleAtualizado.getTentativas() != null ? controleAtualizado.getTentativas() : 0;
                        controleAtualizado.setTentativas(Math.max(0, tentativasAtuais + 1));
                        
                        controleSyncRepository.saveAndFlush(controleAtualizado);
                        log.info("✅ [TBSYNC] Registro recuperado e atualizado com sucesso - ID: {} | Matrícula: {}", 
                                controleAtualizado.getId(), controleAtualizado.getCodigoBeneficiario());
                    } else {
                        log.error("❌ [TBSYNC] Registro não encontrado no banco para recuperação - ID: {} | Matrícula: {}", 
                                controle.getId(), codigoBeneficiario);
                    }
                } catch (Exception recoveryException) {
                    log.error("❌ [TBSYNC] Erro na tentativa de recuperação do registro - ID: {} | Matrícula: {} | Erro: {}", 
                            controle.getId(), codigoBeneficiario, recoveryException.getMessage(), recoveryException);
                }
            }
        }
    }
}
