package com.taskflow.vacations.exception;

import org.springframework.http.HttpStatus;

/** 422 - dados bem formados mas que violam uma regra de negócio (ex.: data de fim antes da de início). */
public class BusinessException extends ApiException {
    public BusinessException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
