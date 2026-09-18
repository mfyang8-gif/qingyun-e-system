package com.qingyun.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class QingyunException extends RuntimeException {

    private final HttpStatus status;

    public QingyunException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public QingyunException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public QingyunException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public QingyunException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}