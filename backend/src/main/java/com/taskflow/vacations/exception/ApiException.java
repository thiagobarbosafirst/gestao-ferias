package com.taskflow.vacations.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção base da aplicação: transporta o status HTTP que deve ser devolvido.
 * O {@link GlobalExceptionHandler} converte-a numa resposta JSON {@link ErrorResponse}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
