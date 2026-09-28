package com.taskflow.vacations.exception;

import org.springframework.http.HttpStatus;

/** 403 - o utilizador está autenticado mas não tem permissão para a operação. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
