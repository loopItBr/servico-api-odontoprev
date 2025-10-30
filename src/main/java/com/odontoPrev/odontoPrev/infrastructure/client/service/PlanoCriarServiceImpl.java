package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.infrastructure.client.BeneficiarioOdontoprevFeignClient;
import com.odontoPrev.odontoPrev.infrastructure.client.adapter.out.dto.PlanoCriarRequest;
import com.odontoPrev.odontoPrev.infrastructure.client.domain.service.TokenService;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.IntegracaoOdontoprev;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para criação de planos na API OdontoPrev
 * 
 * Endpoint: POST /empresa/2.0/plano/criar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanoCriarServiceImpl {

    private final BeneficiarioOdontoprevFeignClient feignClient;
    private final TokenService tokenService;

    /**
     * Cria planos para uma empresa na OdontoPrev
     */
    public String criarPlanoEmpresa(IntegracaoOdontoprev dadosEmpresa) {
        try {
            log.info("🎯 [CRIAÇÃO PLANO] Iniciando criação de planos para empresa: {}", dadosEmpresa.getCodigoEmpresa());
            
            // Criar request com dados da view
            PlanoCriarRequest request = criarRequestPlano(dadosEmpresa);
            
            // Obter token de autorização
            String token = tokenService.obterTokenValido();
            String authorization = "Bearer " + token;
            
            log.info("🚀 [CRIAÇÃO PLANO] Enviando requisição para API...");
            log.info("📤 [CRIAÇÃO PLANO] Request: {}", request);
            
            // Chamar API
            String response = feignClient.criarPlano(authorization, request);
            
            log.info("✅ [CRIAÇÃO PLANO] Planos criados com sucesso para empresa: {}", dadosEmpresa.getCodigoEmpresa());
            log.info("📄 [CRIAÇÃO PLANO] Resposta da API: {}", response);
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ [CRIAÇÃO PLANO] Erro ao criar planos para empresa {}: {}", 
                    dadosEmpresa.getCodigoEmpresa(), e.getMessage());
            throw new RuntimeException("Falha na criação de planos", e);
        }
    }

    /**
     * Cria request com dados da view
     */
    private PlanoCriarRequest criarRequestPlano(IntegracaoOdontoprev dadosEmpresa) {
        log.info("🔧 [CRIAÇÃO PLANO] Criando request para empresa: {}", dadosEmpresa.getCodigoEmpresa());
        
        // Lista de planos
        List<PlanoCriarRequest.PlanoItem> listaPlano = new ArrayList<>();
        
        // PLANO 1 - Sempre presente
        if (dadosEmpresa.getCodigoPlano1() != null) {
            PlanoCriarRequest.PlanoItem plano1 = criarPlanoItem(
                dadosEmpresa.getCodigoPlano1(),
                dadosEmpresa.getDataInicioPlano1(),
                dadosEmpresa.getValorTitular1(),
                dadosEmpresa.getValorDependente1()
            );
            listaPlano.add(plano1);
            log.info("📋 [CRIAÇÃO PLANO] Plano 1 adicionado: {}", dadosEmpresa.getCodigoPlano1());
        }
        
        // PLANO 2 - Se presente
        if (dadosEmpresa.getCodigoPlano2() != null) {
            PlanoCriarRequest.PlanoItem plano2 = criarPlanoItem(
                dadosEmpresa.getCodigoPlano2(),
                dadosEmpresa.getDataInicioPlano2(),
                dadosEmpresa.getValorTitular2(),
                dadosEmpresa.getValorDependente2()
            );
            listaPlano.add(plano2);
            log.info("📋 [CRIAÇÃO PLANO] Plano 2 adicionado: {}", dadosEmpresa.getCodigoPlano2());
        }
        
        // PLANO 3 - Se presente
        if (dadosEmpresa.getCodigoPlano3() != null) {
            PlanoCriarRequest.PlanoItem plano3 = criarPlanoItem(
                dadosEmpresa.getCodigoPlano3(),
                dadosEmpresa.getDataInicioPlano3(),
                dadosEmpresa.getValorTitular3(),
                dadosEmpresa.getValorDependente3()
            );
            listaPlano.add(plano3);
            log.info("📋 [CRIAÇÃO PLANO] Plano 3 adicionado: {}", dadosEmpresa.getCodigoPlano3());
        }
        
        // Construir request (SEM codigoEmpresa - API rejeita)
        PlanoCriarRequest request = PlanoCriarRequest.builder()
                .codigoGrupoGerencial("") // Vazio conforme exemplo
                // .codigoEmpresa(List.of(dadosEmpresa.getCodigoEmpresa())) // REMOVIDO - API rejeita
                .sistema("Sabin Sinai")
                .codigoUsuario("0")
                .listaPlano(listaPlano)
                .build();
        
        log.info("✅ [CRIAÇÃO PLANO] Request criado com {} planos", listaPlano.size());
        return request;
    }

    /**
     * Cria item de plano individual
     */
    private PlanoCriarRequest.PlanoItem criarPlanoItem(Long codigoPlano, LocalDate dataInicio, 
                                                       String valorTitular, String valorDependente) {
        
        // Redes padrão
        List<PlanoCriarRequest.Rede> redes = List.of(
            PlanoCriarRequest.Rede.builder().codigoRede("1").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("31").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("32").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("33").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("35").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("36").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("37").build(),
            PlanoCriarRequest.Rede.builder().codigoRede("38").build()
        );
        
        return PlanoCriarRequest.PlanoItem.builder()
                .valorTitular(converterStringParaDouble(valorTitular))
                .codigoPlano(codigoPlano.intValue())
                .dataInicioPlano(dataInicio != null ? dataInicio.atStartOfDay() : LocalDateTime.now())
                .valorDependente(converterStringParaDouble(valorDependente))
                .valorReembolsoUO(0.0)
                .percentualAgregadoRedeGenerica(0.0)
                .percentualDependenteRedeGenerica(0.0)
                .idSegmentacaoGrupoRede(0)
                .idNomeFantasia(0)
                .redes(redes)
                .percentualAssociado(0.0)
                .planoFamiliar("")
                .periodicidade("")
                .build();
    }

    /**
     * Converte string para double
     */
    private Double converterStringParaDouble(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
