package com.schoolmate.api.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String field;
    private final Object[] messageArgs;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public ApiException(ErrorCode errorCode, Object[] messageArgs) {
        this(errorCode, null, messageArgs);
    }

    public ApiException(ErrorCode errorCode, String field) {
        this(errorCode, field, null);
    }

    public ApiException(ErrorCode errorCode, String field, Object[] messageArgs) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.field = field;
        this.messageArgs = messageArgs;
    }
}
