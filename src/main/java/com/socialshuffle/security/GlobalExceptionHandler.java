package com.socialshuffle.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        Map<String, Object> body = buildErrorMap(HttpStatus.NOT_FOUND.value(), "Endpoint not found: " + ex.getResourcePath());
        body.put("availableEndpoints", "/api/system/health");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED.value(), "HTTP method not supported: " + ex.getMethod());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : "Request error";
        return buildError(ex.getStatusCode().value(), reason);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST.value(), "Malformed JSON request payload.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildError(HttpStatus.BAD_REQUEST.value(), "Missing required parameter: " + ex.getParameterName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database Integrity Conflict caught: {}", ex.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.CONFLICT.value(), "A database constraint conflict occurred (e.g., duplicate record).");
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<Map<String, Object>> handleDatabaseException(Exception ex) {
        log.error("Internal Database Exception caught securely: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Database operation failed. Request could not be processed.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        return buildError(HttpStatus.BAD_REQUEST.value(), "Invalid input parameters provided.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("General Exception caught: {}", ex.getMessage(), ex);
        String msg = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "An unexpected error occurred. Please try again later.";
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
    }

    private ResponseEntity<Map<String, Object>> buildError(int status, String message) {
        return ResponseEntity.status(status).body(buildErrorMap(status, message));
    }

    private Map<String, Object> buildErrorMap(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", message);
        body.put("message", message);
        body.put("status", status);
        body.put("timestamp", System.currentTimeMillis());
        return body;
    }
}
