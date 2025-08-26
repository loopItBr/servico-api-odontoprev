package com.odontoPrev.odontoPrev.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import feign.FeignException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${app.security.expose-error-details:false}")
    private boolean exposeErrorDetails;

    @ExceptionHandler(SincronizacaoException.class)
    public ResponseEntity<Map<String, Object>> handleSincronizacaoException(SincronizacaoException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Erro de sincronização [{}]: {} - Contexto: {}", 
                errorId, ex.getCodigoErro(), ex.getMessage(), ex.getContextoAdicional(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Erro na sincronização OdontoPrev");
        response.put("codigoErro", ex.getCodigoErro());
        response.put("mensagem", getMensagemSegura(ex, "Falha durante o processo de sincronização."));
        response.put("dataHoraOcorrencia", ex.getDataHoraOcorrencia());
        response.put("contexto", ex.getContextoAdicional());
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(AutenticacaoOdontoprevException.class)
    public ResponseEntity<Map<String, Object>> handleAutenticacaoOdontoprevException(AutenticacaoOdontoprevException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Erro de autenticação OdontoPrev [{}]: {} - Contexto: {}", 
                errorId, ex.getCodigoErro(), ex.getMessage(), ex.getContextoAdicional(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Falha na autenticação");
        response.put("codigoErro", ex.getCodigoErro());
        response.put("mensagem", "Não foi possível autenticar com a API da OdontoPrev");
        response.put("dataHoraOcorrencia", ex.getDataHoraOcorrencia());
        response.put("contexto", ex.getContextoAdicional());
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(ComunicacaoApiOdontoprevException.class)
    public ResponseEntity<Map<String, Object>> handleComunicacaoApiException(ComunicacaoApiOdontoprevException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Erro de comunicação com API OdontoPrev [{}]: {} - Contexto: {}", 
                errorId, ex.getCodigoErro(), ex.getMessage(), ex.getContextoAdicional(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Erro de comunicação externa");
        response.put("codigoErro", ex.getCodigoErro());
        response.put("mensagem", "Falha na comunicação com a API da OdontoPrev");
        response.put("dataHoraOcorrencia", ex.getDataHoraOcorrencia());
        response.put("contexto", ex.getContextoAdicional());
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_GATEWAY.value());
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(ConfiguracaoSincronizacaoException.class)
    public ResponseEntity<Map<String, Object>> handleConfiguracaoException(ConfiguracaoSincronizacaoException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Erro de configuração [{}]: {} - Contexto: {}", 
                errorId, ex.getCodigoErro(), ex.getMessage(), ex.getContextoAdicional(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Erro de configuração");
        response.put("codigoErro", ex.getCodigoErro());
        response.put("mensagem", getMensagemSegura(ex, "Problema na configuração da sincronização."));
        response.put("dataHoraOcorrencia", ex.getDataHoraOcorrencia());
        response.put("contexto", ex.getContextoAdicional());
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(RecursosIndisponiveisException.class)
    public ResponseEntity<Map<String, Object>> handleRecursosIndisponiveisException(RecursosIndisponiveisException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Recursos indisponíveis [{}]: {} - Contexto: {}", 
                errorId, ex.getCodigoErro(), ex.getMessage(), ex.getContextoAdicional(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Recursos indisponíveis");
        response.put("codigoErro", ex.getCodigoErro());
        response.put("mensagem", "Recursos necessários para a sincronização estão temporariamente indisponíveis");
        response.put("dataHoraOcorrencia", ex.getDataHoraOcorrencia());
        response.put("contexto", ex.getContextoAdicional());
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Erro interno da aplicação: {}", errorId, ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Erro interno da aplicação");
        response.put("mensagem", getMensagemSegura(ex, "Ocorreu um erro interno. Entre em contato com o suporte."));
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Erro de comunicação com a API da Odontoprev: {}", errorId, ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("errorId", errorId);
        
        if (ex.status() == 401) {
            response.put("erro", "Falha na autenticação");
            response.put("mensagem", "Token inválido ou expirado");
            response.put("status", HttpStatus.UNAUTHORIZED.value());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } else if (ex.status() == 403) {
            response.put("erro", "Acesso negado");
            response.put("mensagem", "Usuário não tem permissão para acessar este recurso");
            response.put("status", HttpStatus.FORBIDDEN.value());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } else if (ex.status() == 404) {
            response.put("erro", "Recurso não encontrado");
            response.put("mensagem", "O recurso solicitado não foi encontrado na Odontoprev");
            response.put("status", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else if (ex.status() >= 400 && ex.status() < 500) {
            response.put("erro", "Erro na requisição");
            response.put("mensagem", "Dados inválidos na requisição");
            response.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } else {
            response.put("erro", "Erro de comunicação com a Odontoprev");
            response.put("mensagem", "Falha na comunicação com o serviço externo");
            response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Erro de validação: {}", errorId, ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Dados inválidos");
        response.put("mensagem", "Os dados fornecidos na requisição são inválidos");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errorId", errorId);
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        response.put("detalhes", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Parâmetro inválido: {}", errorId, ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Parâmetro inválido");
        response.put("mensagem", getMensagemSegura(ex, "Os parâmetros fornecidos são inválidos."));
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[{}] Erro inesperado: {}", errorId, ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Erro interno do servidor");
        response.put("mensagem", "Ocorreu um erro inesperado. Tente novamente mais tarde");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Retorna mensagem segura baseada na configuração do ambiente.
     * Em produção, não expõe detalhes internos da exceção.
     */
    private String getMensagemSegura(Exception ex, String mensagemPadrao) {
        if (exposeErrorDetails) {
            return sanitizarMensagem(ex.getMessage());
        }
        return mensagemPadrao;
    }

    /**
     * Remove informações sensíveis da mensagem de erro.
     */
    private String sanitizarMensagem(String mensagem) {
        if (mensagem == null || mensagem.trim().isEmpty()) {
            return "Erro interno";
        }
        
        // Remove possíveis informações sensíveis
        return mensagem
            .replaceAll("(?i)(password|senha|token|key|secret|credential)=[^\\s,;]+", "$1=***")
            .replaceAll("(?i)(password|senha|token|key|secret|credential)\\s*:\\s*[^\\s,;]+", "$1:***")
            .replaceAll("(?i)\\b\\d{11,}\\b", "***") // Remove números longos (possíveis IDs)
            .replaceAll("(?i)\\b[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\b", "***") // Remove UUIDs
            .substring(0, Math.min(mensagem.length(), 200)); // Limita tamanho
    }
}