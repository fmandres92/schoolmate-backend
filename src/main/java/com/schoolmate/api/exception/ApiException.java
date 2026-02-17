package com.schoolmate.api.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String field;
    private final Object[] messageArgs;
    private final String customMessage;
    private final Map<String, String> details;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, null, null, null, null);
    }

    public ApiException(ErrorCode errorCode, Object[] messageArgs) {
        this(errorCode, null, messageArgs, null, null);
    }

    public ApiException(ErrorCode errorCode, String field) {
        this(errorCode, field, null, null, null);
    }

    public ApiException(ErrorCode errorCode, String field, Object[] messageArgs) {
        this(errorCode, field, messageArgs, null, null);
    }

    public ApiException(ErrorCode errorCode, String customMessage, Map<String, String> details) {
        this(errorCode, null, null, customMessage, details);
    }

    private ApiException(
        ErrorCode errorCode,
        String field,
        Object[] messageArgs,
        String customMessage,
        Map<String, String> details
    ) {
        super(customMessage != null ? customMessage : errorCode.name());
        this.errorCode = errorCode;
        this.field = field;
        this.messageArgs = messageArgs;
        this.customMessage = customMessage;
        this.details = details;
    }
}
