package com.taskflow.vacations.exception;

import org.springframework.http.HttpStatus;

/** 404 - recurso não existe (ou o utilizador não tem permissão para saber que existe). */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
