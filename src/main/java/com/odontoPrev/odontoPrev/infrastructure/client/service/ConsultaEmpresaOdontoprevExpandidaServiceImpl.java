package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.ConsultaEmpresaOdontoprevExpandidaService;
import com.odontoPrev.odontoPrev.infrastructure.client.OdontoprevClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper.EmpresaAlteracaoMapper;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.mapper.EmpresaInativacaoMapper;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaAlteracaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.EmpresaInativacaoRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevAlteracao;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprevExclusao;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevAlteracaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementação do serviço expandido para consulta de empresas na API OdontoPrev.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultaEmpresaOdontoprevExpandidaServiceImpl implements ConsultaEmpresaOdontoprevExpandidaService {
    
    private final OdontoprevClient odontoprevClient;
    private final ObjectMapper objectMapper;
    private final EmpresaAlteracaoMapper empresaAlteracaoMapper;
    private final EmpresaInativacaoMapper empresaInativacaoMapper;
    private final TokenService tokenService;
    private final IntegracaoOdontoprevAlteracaoRepository alteracaoRepository;
    
    @Value("${odontoprev.api.empresa}")
    private String empresa;
    
    @Value("${odontoprev.api.usuario}")
    private String usuario;
    
    @Value("${odontoprev.api.senha}")
    private String senha;
    
    @Value("${odontoprev.api.app-id}")
    private String appId;
    
    @Override
    public String adicionarEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        try {
            String dadosJson = objectMapper.writeValueAsString(dadosEmpresa);
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            
            return odontoprevClient.adicionarEmpresa(
                authorization,
                empresa,
                usuario,
                senha,
                appId,
                dadosJson
            );
            
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar dados da empresa para adição: {}", e.getMessage());
            throw new RuntimeException("Falha na serialização dos dados", e);
        } catch (Exception e) {
            log.error("Erro ao adicionar empresa na API OdontoPrev: {}", e.getMessage());
            throw new RuntimeException("Falha na comunicação com a API", e);
        }
    }
    
    @Override
    public String alterarEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        String codigoEmpresa = dadosEmpresa.getCodigoEmpresa();
        
        try {
            log.info("🔄 [ALTERAÇÃO EMPRESA] Iniciando alteração da empresa: {}", codigoEmpresa);
            log.info("🔍 [ALTERAÇÃO EMPRESA] Dados recebidos - CNPJ: {}, Nome: {}, Grupo: {}", 
                    dadosEmpresa.getCnpj(), dadosEmpresa.getNomeFantasia(), dadosEmpresa.getCodigoGrupoGerencial());
            
            // Cria request com APENAS campos modificados
            log.debug("🔄 [ALTERAÇÃO EMPRESA] Criando request com apenas campos modificados...");
            EmpresaAlteracaoRequest request = criarRequestMinimo(dadosEmpresa);
            log.debug("✅ [ALTERAÇÃO EMPRESA] Request mínimo criado");
            
            // Log detalhado do request
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.info("📤 [ALTERAÇÃO EMPRESA] Request JSON completo: {}", requestJson);
                log.info("📤 [ALTERAÇÃO EMPRESA] Tamanho do request: {} bytes", requestJson.length());
                
                // Log específico do endereço para debug
                if (request.getEndereco() != null) {
                    log.info("🏠 [ALTERAÇÃO EMPRESA] Dados do endereço:");
                    log.info("   tipoLogradouro: '{}'", request.getEndereco().getTipoLogradouro());
                    log.info("   logradouro: '{}'", request.getEndereco().getLogradouro());
                    log.info("   numero: '{}'", request.getEndereco().getNumero());
                    log.info("   bairro: '{}'", request.getEndereco().getBairro());
                    log.info("   cep: '{}'", request.getEndereco().getCep());
                    if (request.getEndereco().getCidade() != null) {
                        log.info("   cidade.codigo: {}", request.getEndereco().getCidade().getCodigo());
                        log.info("   cidade.nome: '{}'", request.getEndereco().getCidade().getNome());
                        log.info("   cidade.siglaUf: '{}'", request.getEndereco().getCidade().getSiglaUf());
                        log.info("   cidade.codigoPais: {}", request.getEndereco().getCidade().getCodigoPais());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [ALTERAÇÃO EMPRESA] Erro ao serializar request para log: {}", e.getMessage());
            }
            
            // Log dos headers
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            log.info("🔑 [ALTERAÇÃO EMPRESA] Headers da requisição:");
            log.info("   Authorization: {}...", authorization.substring(0, Math.min(30, authorization.length())));
            log.info("   empresa: {}", empresa);
            log.info("   usuario: {}", usuario);
            log.info("   senha: [OCULTA]");
            log.info("   app-id: {}", appId);
            
            // Log da URL
            log.info("🌐 [ALTERAÇÃO EMPRESA] URL da requisição: PUT /empresas/alterar");
            log.info("🌐 [ALTERAÇÃO EMPRESA] URL base configurada: {} + {}", 
                    "${odontoprev.api.base-url}", "${odontoprev.api.path}");
            
            long inicioChamada = System.currentTimeMillis();
            log.info("🚀 [ALTERAÇÃO EMPRESA] Enviando requisição para API...");
            
            String response = odontoprevClient.alterarEmpresa(
                authorization,
                empresa,
                usuario,
                senha,
                appId,
                request
            );
            
            long tempoResposta = System.currentTimeMillis() - inicioChamada;
            log.info("✅ [ALTERAÇÃO EMPRESA] Empresa {} alterada com sucesso em {}ms", codigoEmpresa, tempoResposta);
            log.info("📄 [ALTERAÇÃO EMPRESA] Resposta da API: {}", response);
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ [ALTERAÇÃO EMPRESA] Erro ao alterar empresa {}: {}", codigoEmpresa, e.getMessage());
            log.error("❌ [ALTERAÇÃO EMPRESA] Tipo da exceção: {}", e.getClass().getSimpleName());
            log.error("❌ [ALTERAÇÃO EMPRESA] Stack trace completo:", e);
            
            // Log adicional para diferentes tipos de erro
            if (e.getMessage() != null) {
                if (e.getMessage().contains("401")) {
                    log.error("🔐 [ALTERAÇÃO EMPRESA] ERRO 401 - Problema de autenticação/autorização");
                } else if (e.getMessage().contains("400")) {
                    log.error("📝 [ALTERAÇÃO EMPRESA] ERRO 400 - Dados inválidos enviados para API");
                } else if (e.getMessage().contains("403")) {
                    log.error("🚫 [ALTERAÇÃO EMPRESA] ERRO 403 - Acesso negado");
                } else if (e.getMessage().contains("404")) {
                    log.error("🔍 [ALTERAÇÃO EMPRESA] ERRO 404 - Endpoint não encontrado");
                } else if (e.getMessage().contains("500")) {
                    log.error("💥 [ALTERAÇÃO EMPRESA] ERRO 500 - Erro interno do servidor");
                } else if (e.getMessage().contains("timeout")) {
                    log.error("⏰ [ALTERAÇÃO EMPRESA] TIMEOUT - Requisição demorou muito para responder");
                } else if (e.getMessage().contains("connection")) {
                    log.error("🔌 [ALTERAÇÃO EMPRESA] CONNECTION - Problema de conectividade");
                }
            }
            
            throw new RuntimeException("Falha na comunicação com a API", e);
        }
    }
    
    @Override
    public String inativarEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        try {
            // Usar o mapper para converter para o DTO correto
            EmpresaInativacaoRequest request = empresaInativacaoMapper.toInativacaoRequest(dadosEmpresa, empresa);
            
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            
            log.info("🔧 [INATIVAÇÃO] Dados enviados: {}", objectMapper.writeValueAsString(request));
            
            return odontoprevClient.inativarEmpresa(
                authorization,
                empresa,
                usuario,
                senha,
                appId,
                request
            );
            
        } catch (Exception e) {
            log.error("Erro ao inativar empresa na API OdontoPrev: {}", e.getMessage());
            throw new RuntimeException("Falha na comunicação com a API", e);
        }
    }
    
    @Override
    public String inativarEmpresaExclusao(IntegracaoOdontoprevExclusao dadosExclusao) {
        try {
            // Usar o mapper para converter dados de exclusão para o DTO correto
            EmpresaInativacaoRequest request = empresaInativacaoMapper.toInativacaoRequestExclusao(dadosExclusao, empresa);
            
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            
            log.info("🔧 [INATIVAÇÃO EXCLUSÃO] Dados enviados: {}", objectMapper.writeValueAsString(request));
            
            return odontoprevClient.inativarEmpresa(
                authorization,
                empresa,
                usuario,
                senha,
                appId,
                request
            );
            
        } catch (Exception e) {
            log.error("Erro ao inativar empresa (exclusão) na API OdontoPrev: {}", e.getMessage());
            throw new RuntimeException("Falha na comunicação com a API", e);
        }
    }
    
    
    /**
     * CRIA REQUEST MÍNIMO COM APENAS CAMPOS MODIFICADOS
     *
     * Envia apenas os campos que realmente mudaram, evitando problemas
     * de validação com campos que não devem ser alterados.
     * Agora usa o mapper para aproveitar os dados de endereço da view.
     */
    private EmpresaAlteracaoRequest criarRequestMinimo(IntegracaoOdontoprev dadosEmpresa) {
        log.debug("🔧 [ALTERAÇÃO EMPRESA] Criando request mínimo para empresa: {}", dadosEmpresa.getCodigoEmpresa());
        
        // Tenta buscar dados específicos da view de alteração para mapear endereço/cidade corretamente
        EmpresaAlteracaoRequest request;
        try {
            String codigoEmpresa = dadosEmpresa.getCodigoEmpresa();
            java.util.Optional<IntegracaoOdontoprevAlteracao> viewOpt = alteracaoRepository.buscarPrimeiroDadoPorCodigoEmpresa(codigoEmpresa);
            if (viewOpt.isPresent()) {
                log.debug("📄 [ALTERAÇÃO EMPRESA] Dados da view encontrados para {}. Usando CODIGOCIDADE da view.", codigoEmpresa);
                request = empresaAlteracaoMapper.toAlteracaoRequest(viewOpt.get());
            } else {
                log.debug("📄 [ALTERAÇÃO EMPRESA] Dados da view não encontrados para {}. Fazendo fallback para entidade base.", codigoEmpresa);
                request = empresaAlteracaoMapper.toAlteracaoRequest(dadosEmpresa);
            }
        } catch (Exception e) {
            log.warn("⚠️ [ALTERAÇÃO EMPRESA] Falha ao buscar dados da view: {}. Fallback para entidade base.", e.getMessage());
            request = empresaAlteracaoMapper.toAlteracaoRequest(dadosEmpresa);
        }
        
        log.debug("✅ [ALTERAÇÃO EMPRESA] Request mínimo criado com campos:");
        log.debug("   codigoEmpresa: '{}'", request.getCodigoEmpresa());
        log.debug("   nomeFantasia: '{}'", request.getNomeFantasia());
        log.debug("   dataVigencia: {}", request.getDataVigencia());
        log.debug("   codigoUsuario: '{}'", request.getCodigoUsuario());
        log.debug("   endereco: {}", request.getEndereco() != null ? "PRESENTE" : "AUSENTE");
        
        return request;
    }
    
}