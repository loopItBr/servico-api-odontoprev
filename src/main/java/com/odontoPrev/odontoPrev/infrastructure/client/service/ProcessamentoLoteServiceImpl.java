package com.odontoPrev.odontoPrev.infrastructure.client.service;

import com.odontoPrev.odontoPrev.domain.service.ProcessamentoEmpresaService;
import com.odontoPrev.odontoPrev.domain.service.ProcessamentoLoteService;
import com.odontoPrev.odontoPrev.infrastructure.repository.IntegracaoOdontoprevRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Processamento de empresas em lotes paginados.
 * Evita carregar muitos registros na memória processando em páginas pequenas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoLoteServiceImpl implements ProcessamentoLoteService {

    private final IntegracaoOdontoprevRepository integracaoRepository;
    private final ProcessamentoEmpresaService processamentoEmpresaService;

    @Override
    public void processarEmpresasEmLotes(int tamanhoBatch, int maxThreads, long totalEmpresas) {
        if (totalEmpresas == 0) {
            log.info("Nenhuma empresa encontrada para processamento");
            return;
        }

        log.info("Processando {} empresas em lotes de {}", totalEmpresas, tamanhoBatch);
        
        long empresasProcessadas = processarTodasAsPaginas(tamanhoBatch, totalEmpresas);

        log.info("Processamento finalizado: {} empresas", empresasProcessadas);
    }

    @Override
    public List<String> buscarCodigosEmpresasPaginado(int offset, int limit) {
        int numeroPagina = calcularNumeroPagina(offset, limit);
        return integracaoRepository.buscarCodigosEmpresasPaginado(PageRequest.of(numeroPagina, limit));
    }

    @Override
    public long contarTotalEmpresas() {
        return integracaoRepository.contarTotalEmpresas();
    }


    private long processarTodasAsPaginas(int tamanhoBatch, long totalEmpresas) {
        int numeroPagina = 0;
        long empresasProcessadas = 0;
        
        while (true) {
            List<String> loteAtual = buscarProximoLote(numeroPagina, tamanhoBatch);
            
            if (loteEstaVazio(loteAtual)) {
                break;
            }
            
            logInicioLote(numeroPagina, loteAtual);
            long processadasNoLote = processarLote(loteAtual);
            empresasProcessadas += processadasNoLote;
            
            numeroPagina++;
            logFimLote(numeroPagina, empresasProcessadas, totalEmpresas);
        }
        
        return empresasProcessadas;
    }

    private List<String> buscarProximoLote(int numeroPagina, int tamanhoBatch) {
        int offset = numeroPagina * tamanhoBatch;
        return buscarCodigosEmpresasPaginado(offset, tamanhoBatch);
    }

    private boolean loteEstaVazio(List<String> lote) {
        return lote.isEmpty();
    }

    private void logInicioLote(int numeroPagina, List<String> lote) {
        log.info("Processando lote {} - {} empresas", numeroPagina + 1, lote.size());
    }

    private long processarLote(List<String> codigosEmpresas) {
        long processadasNoLote = 0;
        
        for (String codigoEmpresa : codigosEmpresas) {
            if (processarEmpresaComSeguranca(codigoEmpresa)) {
                processadasNoLote++;
            }
        }
        
        return processadasNoLote;
    }

    private boolean processarEmpresaComSeguranca(String codigoEmpresa) {
        try {
            processamentoEmpresaService.processar(codigoEmpresa);
            return true;
        } catch (Exception e) {
            logErroProcessamentoEmpresa(codigoEmpresa, e);
            return false;
        }
    }

    private void logErroProcessamentoEmpresa(String codigoEmpresa, Exception e) {
        log.error("Erro ao processar empresa {}: {}", codigoEmpresa, e.getMessage());
    }

    private void logFimLote(int numeroPagina, long empresasProcessadas, long totalEmpresas) {
        log.info("Lote {} concluído - Total processadas: {}/{}", 
                numeroPagina, empresasProcessadas, totalEmpresas);
    }

    private int calcularNumeroPagina(int offset, int limit) {
        return offset / limit;
    }
}