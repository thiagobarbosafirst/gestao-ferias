package com.taskflow.vacations.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Formato ÚNICO de erro devolvido por toda a API, por exemplo:
 * <pre>
 * {
 *   "timestamp": "2026-09-26T10:00:00Z",
 *   "status": 409,
 *   "error": "Conflict",
 *   "message": "O período ... sobrepõe-se ...",
 *   "path": "/api/vacations",
 *   "fieldErrors": [ { "field": "email", "message": "Email inválido" } ]
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path,
                            List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }
}
