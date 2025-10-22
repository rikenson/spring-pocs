package com.tiger.pocs.client.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@Slf4j
@UtilityClass
public class LoggerUtil {
    
    private static final String CORRELATION_ID = "correlationId";
    private static final String REQUEST_ID = "requestId";
    
    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
    
    public static void logRequest(String controller, String method, Map<String, String> params) {
        String requestId = generateRequestId();
        MDC.put(REQUEST_ID, requestId);
        log.info("[{}] {} request received with params: {}", controller, method, params);
    }
    
    public static void logResponse(String controller, String method, int resultCount) {
        log.info("[{}] {} response sent with {} results", controller, method, resultCount);
        MDC.remove(REQUEST_ID);
    }
    
    public static void logError(String controller, String method, Throwable error) {
        log.error("[{}] {} error occurred: {}", controller, method, error.getMessage(), error);
        MDC.remove(REQUEST_ID);
    }
    
    public static <T> Mono<T> logMono(Mono<T> mono, String operation) {
        return mono
            .doOnSubscribe(sub -> log.debug("Starting operation: {}", operation))
            .doOnSuccess(result -> log.debug("Operation {} completed successfully", operation))
            .doOnError(error -> log.error("Operation {} failed: {}", operation, error.getMessage()));
    }
    
    public static <T> Flux<T> logFlux(Flux<T> flux, String operation) {
        return flux
            .doOnSubscribe(sub -> log.debug("Starting flux operation: {}", operation))
            .doOnComplete(() -> log.debug("Flux operation {} completed", operation))
            .doOnError(error -> log.error("Flux operation {} failed: {}", operation, error.getMessage()));
    }
    
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
    }
    
    public static void clearMDC() {
        MDC.clear();
    }
}