package com.odontoPrev.odontoPrev.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import feign.FeignException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Erro interno da aplicação: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Erro interno da aplicação");
        response.put("mensagem", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(FeignException ex) {
        log.error("Erro de comunicação com a API da Odontoprev: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
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
        log.error("Erro de validação: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Dados inválidos");
        response.put("mensagem", "Os dados fornecidos na requisição são inválidos");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        response.put("detalhes", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Parâmetro inválido: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Parâmetro inválido");
        response.put("mensagem", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("erro", "Erro interno do servidor");
        response.put("mensagem", "Ocorreu um erro inesperado. Tente novamente mais tarde");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}