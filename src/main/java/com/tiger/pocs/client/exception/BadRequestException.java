package com.tiger.pocs.client.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }
    
    public BadRequestException(String field, String reason) {
        super(String.format("Invalid %s: %s", field, reason), HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }
}