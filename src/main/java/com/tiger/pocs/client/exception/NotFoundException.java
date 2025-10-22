package com.tiger.pocs.client.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {
    
    public NotFoundException(String resource, String identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
    
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}