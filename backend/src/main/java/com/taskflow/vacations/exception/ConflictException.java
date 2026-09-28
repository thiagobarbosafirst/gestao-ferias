package com.taskflow.vacations.exception;

import org.springframework.http.HttpStatus;

/** 409 - conflito com o estado atual (ex.: férias sobrepostas, email duplicado, pedido já decidido). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
