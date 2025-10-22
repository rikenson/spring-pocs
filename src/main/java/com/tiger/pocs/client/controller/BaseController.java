package com.tiger.pocs.client.controller;

import com.tiger.pocs.client.util.LoggerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@CrossOrigin(origins = "*")
public abstract class BaseController {
    
    protected String getControllerName() {
        return this.getClass().getSimpleName();
    }
    
    protected <R> Mono<R> logMono(Mono<R> mono, Map<String, ?> requestData) {
        Map<String, String> stringMap = convertToStringMap(requestData);
        LoggerUtil.logRequest(getControllerName(), "search", stringMap);
        return mono
            .doOnSuccess(result -> LoggerUtil.logResponse(getControllerName(), "search", 1))
            .doOnError(error -> LoggerUtil.logError(getControllerName(), "search", error));
    }
    
    protected <R> Flux<R> logFlux(Flux<R> flux, Map<String, ?> requestData) {
        Map<String, String> stringMap = convertToStringMap(requestData);
        LoggerUtil.logRequest(getControllerName(), "search", stringMap);
        return flux
            .doOnComplete(() -> LoggerUtil.logResponse(getControllerName(), "search", -1))
            .doOnError(error -> LoggerUtil.logError(getControllerName(), "search", error));
    }
    
    private Map<String, String> convertToStringMap(Map<String, ?> map) {
        return map.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> String.valueOf(entry.getValue())
            ));
    }
    
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of(
            "status", "UP",
            "service", getControllerName(),
            "timestamp", java.time.Instant.now().toString()
        ));
    }
}