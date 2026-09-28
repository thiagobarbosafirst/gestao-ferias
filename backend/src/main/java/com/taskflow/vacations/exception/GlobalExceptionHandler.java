package com.taskflow.vacations.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Converte todas as exceções em respostas JSON com o mesmo formato ({@link ErrorResponse})
 * e o status HTTP adequado. Assim os controllers não precisam de try/catch.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Erros lançados pelos nossos serviços (404, 403, 409, 422...). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req, List.of());
    }

    /** Falhas de Bean Validation no corpo do pedido (@NotNull, @Email, ...) -> 400 com erros por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", req, fields);
    }

    /** JSON mal formado ou valor impossível de converter (ex.: data "2026-13-40", role inexistente) -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Corpo do pedido inválido ou mal formatado", req, List.of());
    }

    /** Parâmetro de URL/query com tipo errado (ex.: /api/users/abc) -> 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'", req, List.of());
    }

    /** Parâmetro obrigatório em falta (ex.: /api/vacations/calendar sem "from") -> 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório em falta: '" + ex.getParameterName() + "'", req, List.of());
    }

    /** Ordenação por um campo que não existe (ex.: ?sort=abc) -> 400. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleBadSort(PropertyReferenceException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Campo de ordenação inválido: '" + ex.getPropertyName() + "'", req, List.of());
    }

    /** @PreAuthorize negado (ex.: MANAGER a tentar criar utilizadores) -> 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Não tem permissão para realizar esta operação", req, List.of());
    }

    /**
     * Violação de constraint na base de dados. O caso mais importante é a constraint
     * "no_overlapping_vacations", que apanha pedidos sobrepostos criados em simultâneo.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage());
        String message = detail.contains("no_overlapping_vacations")
                ? "O período sobrepõe-se a férias já marcadas por outro colaborador"
                : "A operação viola uma restrição de integridade dos dados";
        return build(HttpStatus.CONFLICT, message, req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Endpoint não encontrado", req, List.of());
    }

    /** Método HTTP não suportado no endpoint (ex.: DELETE /api/vacations) -> 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " não suportado neste endpoint", req, List.of());
    }

    /** Content-Type não suportado (ex.: text/plain em vez de application/json) -> 415. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado; use application/json", req, List.of());
    }

    /** Qualquer outro erro inesperado -> 500, sem expor detalhes internos ao cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", req, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req,
                                                List<ErrorResponse.FieldError> fields) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, req.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
