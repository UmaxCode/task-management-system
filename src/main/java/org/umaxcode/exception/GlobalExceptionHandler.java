package org.umaxcode.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskManagementException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorMessage taskManagementExceptionHandler(TaskManagementException ex, HttpServletRequest request) {

        return ErrorMessage.builder()
                .path(request.getRequestURI())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorMessage authenticationExceptionHandler(AuthenticationException ex, HttpServletRequest request) {

        return ErrorMessage.builder()
                .path(request.getRequestURI())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorMessage accessDeniedHandler(AccessDeniedException ex, HttpServletRequest request) {

        return ErrorMessage.builder()
                .path(request.getRequestURI())
                .message("You do not have permission to access this resource.")
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorMessage handleArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ErrorMessage.builder()
                .path(request.getRequestURI())
                .message(errors)
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorMessage exceptionHandler(Exception ex, HttpServletRequest request) {

        return ErrorMessage.builder()
                .path(request.getRequestURI())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
