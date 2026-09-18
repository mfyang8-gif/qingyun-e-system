package com.qingyun.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends QingyunException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, cause);
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s 不存在，ID: %d", resourceType, id), HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s 不存在: %s", resourceType, identifier), HttpStatus.NOT_FOUND);
    }
}