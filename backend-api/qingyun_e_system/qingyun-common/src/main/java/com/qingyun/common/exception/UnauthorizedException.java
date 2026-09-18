package com.qingyun.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends QingyunException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, HttpStatus.UNAUTHORIZED, cause);
    }

    public UnauthorizedException() {
        super("未授权，请登录", HttpStatus.UNAUTHORIZED);
    }
}