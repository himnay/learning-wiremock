package com.learnwiremock.movies.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders every error as {@code {timestamp, status, error, message, path}}. Extends
 * {@link ResponseEntityExceptionHandler} so Spring MVC's own exceptions (malformed JSON, a
 * non-numeric id, a missing parameter, an unsupported method, an unknown path) keep their 4xx
 * status instead of falling through to the catch-all 500 below.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Handles not found. */
    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(
            MovieNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), HttpHeaders.EMPTY);
    }

    /** Bean Validation failures on the request body: 400 listing the missing fields. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> fields = ex.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).sorted().toList();
        String message = "Please pass all the input fields : " + fields;
        return error(HttpStatus.BAD_REQUEST, message, path(request), headers);
    }

    /** Every other Spring MVC exception, with the status and detail Spring assigns to it. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : ex.getMessage();
        return error(HttpStatus.valueOf(statusCode.value()), message, path(request), headers);
    }

    /** Handles generic. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(
            Exception ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(), HttpHeaders.EMPTY);
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message, String path, HttpHeaders headers) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servlet
                ? servlet.getRequest().getRequestURI()
                : request.getDescription(false);
    }
}
