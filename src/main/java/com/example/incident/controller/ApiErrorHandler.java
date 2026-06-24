package com.example.incident.controller;

import com.example.incident.model.IncidentApi.ApiException;
import com.example.incident.model.IncidentApi.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/** Maps failures to the consistent ErrorResponse JSON shape required by the spec. */
@RestControllerAdvice
class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> onApi(ApiException ex, HttpServletRequest req) {
        return respond(ex.getHttpStatus(), ex.getCode(), ex.getMessage(), req.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> onValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return respond(400, "VALIDATION_FAILED", "Request validation failed", req.getRequestURI(), fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> onBadRequest(Exception ex, HttpServletRequest req) {
        String code = ex instanceof HttpMessageNotReadableException ? "MALFORMED_REQUEST" : "INVALID_PARAMETER";
        return respond(400, code, ex.getMessage(), req.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> onUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error path={}", req.getRequestURI(), ex);
        return respond(500, "INTERNAL_ERROR", "An unexpected error occurred", req.getRequestURI(), List.of());
    }

    private static ResponseEntity<ErrorResponse> respond(
            int status, String code, String message, String path, List<ErrorResponse.FieldError> fieldErrors
    ) {
        log.warn("api_error code={} status={} path={}", code, status, path);
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, Instant.now(), path, fieldErrors));
    }
}
