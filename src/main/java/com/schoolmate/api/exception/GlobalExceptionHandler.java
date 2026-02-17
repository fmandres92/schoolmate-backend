package com.schoolmate.api.exception;

import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;
    private final ClockProvider clockProvider;

    public GlobalExceptionHandler(MessageSource messageSource, ClockProvider clockProvider) {
        this.messageSource = messageSource;
        this.clockProvider = clockProvider;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildErrorResponse(ErrorCode.AUTH_BAD_CREDENTIALS, null, null, null, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), null, null, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return buildErrorResponse(ErrorCode.BUSINESS_RULE, ex.getMessage(), null, null, request, ex.getDetails());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        return buildErrorResponse(
            ex.getErrorCode(),
            ex.getCustomMessage(),
            ex.getField(),
            ex.getMessageArgs(),
            request,
            ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return buildErrorResponse(ErrorCode.VALIDATION_FAILED, null, null, null, request, errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(ErrorCode.ACCESS_DENIED, null, null, null, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildErrorResponse(ErrorCode.DATA_INTEGRITY, null, null, null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, null, null, null, request);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            ErrorCode errorCode,
            String directMessage,
            String field,
            Object[] messageArgs,
            HttpServletRequest request
    ) {
        return buildErrorResponse(errorCode, directMessage, field, messageArgs, request, null);
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            ErrorCode errorCode,
            String directMessage,
            String field,
            Object[] messageArgs,
            HttpServletRequest request,
            Map<String, String> details
    ) {
        String message = directMessage;
        if (message == null || message.isBlank()) {
            message = messageSource.getMessage(
                    errorCode.getMessageKey(),
                    messageArgs,
                    errorCode.name(),
                    LocaleContextHolder.getLocale()
            );
        }

        ApiErrorResponse body = ApiErrorResponse.builder()
                .code(errorCode.name())
                .message(message)
                .status(errorCode.getStatus().value())
                .field(field)
                .path(request.getRequestURI())
                .timestamp(clockProvider.now())
                .details(details)
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }
}
