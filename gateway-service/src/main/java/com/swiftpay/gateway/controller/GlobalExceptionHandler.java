package com.swiftpay.gateway.controller;

import com.swiftpay.gateway.dto.ErrorResponse;
import com.swiftpay.gateway.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentService.DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(PaymentService.DuplicatePaymentException ex,
                                                          HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "Duplicate Payment", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentService.InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(PaymentService.InsufficientFundsException ex,
                                                                   HttpServletRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Funds", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentService.AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentService.AccountNotFoundException ex,
                                                         HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Account Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildError(HttpStatus.BAD_REQUEST, "Validation Failed", details, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String error,
                                                       String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build());
    }
}
