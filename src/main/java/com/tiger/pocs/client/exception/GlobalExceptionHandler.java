package com.tiger.pocs.client.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "com.tiger.pocs.client")
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ApiException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleApiException(ApiException ex) {
        log.error("API Exception occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", ex.getStatus().value());
        errorResponse.put("error", ex.getStatus().getReasonPhrase());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("errorCode", ex.getErrorCode());
        
        return Mono.just(ResponseEntity.status(ex.getStatus()).body(errorResponse));
    }
    
    @ExceptionHandler(NotFoundException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleNotFoundException(NotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("errorCode", ex.getErrorCode());
        
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
    }
    
    @ExceptionHandler(BadRequestException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleBadRequestException(BadRequestException ex) {
        log.error("Bad request: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("errorCode", ex.getErrorCode());
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }
    
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationException(WebExchangeBindException ex) {
        log.error("Validation error: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Validation Failed");
        errorResponse.put("message", "Request validation failed");
        
        if (ex.getBindingResult().hasFieldErrors()) {
            Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    error -> error.getField(),
                    error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                    (existing, replacement) -> existing
                ));
            errorResponse.put("fieldErrors", fieldErrors);
        }
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Invalid Argument");
        errorResponse.put("message", ex.getMessage());
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }
    
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An unexpected error occurred");
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}