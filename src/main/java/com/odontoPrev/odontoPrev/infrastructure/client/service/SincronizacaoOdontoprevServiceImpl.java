package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaService;
import com.odontoPrev.odontoPrev.domain.service.SincronizacaoOdontoprevService;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementação principal do serviço de sincronização com OdontoPrev.
 * Responsável por orquestrar o processo de sincronização.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacaoOdontoprevServiceImpl implements SincronizacaoOdontoprevService {

    private final IntegracaoOdontoprevRepository integracaoRepository;
    private final ProcessamentoEmpresaService processamentoEmpresaService;

    @Override
    @Transactional
    public void executarSincronizacao() {
        LocalDateTime inicioSincronizacao = LocalDateTime.now();
        log.info("Iniciando sincronização com OdontoPrev - {}", inicioSincronizacao);

        try {
            List<String> codigosEmpresas = obterCodigosEmpresas();
            
            if (codigosEmpresas.isEmpty()) {
                log.info("Nenhuma empresa encontrada para sincronização");
                return;
            }
            
            log.info("Encontradas {} empresas para sincronização", codigosEmpresas.size());
            processarEmpresas(codigosEmpresas);
            
        } catch (Exception e) {
            log.error("Erro durante a sincronização com OdontoPrev: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na execução da sincronização", e);
        } finally {
            LocalDateTime fimSincronizacao = LocalDateTime.now();
            long duracao = java.time.Duration.between(inicioSincronizacao, fimSincronizacao).toMillis();
            log.info("Sincronização com OdontoPrev finalizada - {} (duração: {}ms)", 
                    fimSincronizacao, duracao);
        }
    }

    private List<String> obterCodigosEmpresas() {
        return integracaoRepository.buscarCodigosEmpresasDisponiveis();
    }

    private void processarEmpresas(List<String> codigosEmpresas) {
        for (String codigoEmpresa : codigosEmpresas) {
            try {
                processamentoEmpresaService.processar(codigoEmpresa);
            } catch (Exception e) {
                log.error("Erro ao processar empresa {}: {}", codigoEmpresa, e.getMessage(), e);
                // Continua processando as demais empresas
            }
        }
    }
}