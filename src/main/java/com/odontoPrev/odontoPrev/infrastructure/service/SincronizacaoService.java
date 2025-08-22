package com.odontoPrev.odontoPrev.infrastructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoPrev.odontoPrev.domain.service.OdontoPrevService;
import com.odontoPrev.odontoPrev.infrastructure.client.LoginEmpresaApiClient;
import com.odontoPrev.odontoPrev.infrastructure.repository.ControleSyncRepository;
import com.odontoPrev.odontoPrev.infrastructure.repository.entity.ControleSync;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SincronizacaoService {

    private final OdontoPrevService odontoPrevService;
    private final LoginEmpresaApiClient loginEmpresaApiClient;
    private final ControleSyncRepository controleSyncRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 */2 * * * ?")
    public void sincronizarEmpresas() {
        log.info("Iniciando sincronização de dados de empresas.");

        // Reutiliza a lógica de obtenção de tokens do OdontoPrevService
        String apiToken = odontoPrevService.getValidApiToken();
        String empresaToken = odontoPrevService.getValidEmpresaToken(apiToken);

        if (apiToken == null || empresaToken == null) {
            log.error("Não foi possível sincronizar empresas. Tokens não disponíveis.");
            return;
        }

        String codigoEmpresa = "787392";
        ControleSync syncLog = buildInitialSyncLog(codigoEmpresa);
        long startTime = System.currentTimeMillis();

        try {
            String authHeader = "Bearer " + apiToken;
            Object apiResponse = loginEmpresaApiClient.getEmpresa(authHeader, codigoEmpresa);
            long duration = System.currentTimeMillis() - startTime;

            syncLog.setTempoRespostaMs(duration);
            syncLog.setResponseApi(convertResponseToString(apiResponse));
            syncLog.setStatusSync(ControleSync.StatusSync.SUCCESS);
            syncLog.setDataSucesso(LocalDateTime.now());
            log.info("Sincronização da empresa {} concluída com sucesso.", codigoEmpresa);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            syncLog.setTempoRespostaMs(duration);
            syncLog.setStatusSync(ControleSync.StatusSync.ERROR);
            syncLog.setErroMensagem(e.getMessage());
            log.error("Erro durante a chamada da API para a empresa {}. Erro: {}", codigoEmpresa, e.getMessage());

        } finally {
            syncLog.setTentativas(syncLog.getTentativas() + 1);
            syncLog.setDataUltimaTentativa(LocalDateTime.now());
            controleSyncRepository.save(syncLog);
        }
    }

    private ControleSync buildInitialSyncLog(String codigoEmpresa) {
        ControleSync syncLog = new ControleSync();
        syncLog.setCodigoEmpresa(codigoEmpresa);
        syncLog.setTipoOperacao(ControleSync.TipoOperacao.GET);
        syncLog.setEndpointDestino("/dcms/empresa/2.0/" + codigoEmpresa);
        syncLog.setDataCriacao(LocalDateTime.now());
        syncLog.setStatusSync(ControleSync.StatusSync.PENDING);
        syncLog.setMaxTentativas(3);
        syncLog.setTentativas(0);
        return syncLog;
    }

    private String convertResponseToString(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.warn("Não foi possível serializar a resposta da API para JSON. Armazenando toString().");
            return response.toString();
        }
    }
}
