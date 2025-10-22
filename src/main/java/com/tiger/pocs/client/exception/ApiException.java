package com.tiger.pocs.client.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;
    
    public ApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
    
    public ApiException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}