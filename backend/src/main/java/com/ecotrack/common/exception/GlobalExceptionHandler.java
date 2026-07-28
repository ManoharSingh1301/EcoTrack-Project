package com.ecotrack.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Single application-wide exception handler. Produces a structured
 * {@link ErrorResponse} JSON body for every error type raised by any domain
 * (users, items, borrow, favorites, chat).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse body(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(body(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return new ResponseEntity<>(body(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return new ResponseEntity<>(body(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return new ResponseEntity<>(body(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAction(
            UnauthorizedActionException ex, HttpServletRequest request) {
        log.warn("Unauthorized action: {}", ex.getMessage());
        return new ResponseEntity<>(body(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Missing required request header: {}", ex.getHeaderName());
        String msg = "Required request header '" + ex.getHeaderName() + "' is missing. "
                + "You must be signed in (send a valid 'Authorization: Bearer <token>' header).";
        return new ResponseEntity<>(body(HttpStatus.BAD_REQUEST, "Bad Request", msg, request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = ((FieldError) err).getField();
            String errorMessage = err.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", validationErrors);
        ErrorResponse error = body(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields have validation errors", request);
        error.setValidationErrors(validationErrors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        String msg = "Content-Type '" + ex.getContentType() + "' is not supported for this endpoint. "
                + "Use 'multipart/form-data' (with an 'item' part) or 'application/json'.";
        return new ResponseEntity<>(body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", msg, request),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestPart(
            MissingServletRequestPartException ex, HttpServletRequest request) {
        log.warn("Missing request part: {}", ex.getRequestPartName());
        String msg = "Required multipart part '" + ex.getRequestPartName() + "' is missing. "
                + "When sending multipart/form-data, include an 'item' part with Content-Type application/json.";
        return new ResponseEntity<>(body(HttpStatus.BAD_REQUEST, "Bad Request", msg, request),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        log.warn("Request rejected: {}", ex.getReason());
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return new ResponseEntity<>(body(status, status.getReasonPhrase(), ex.getReason(), request), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: ", ex);
        return new ResponseEntity<>(body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
