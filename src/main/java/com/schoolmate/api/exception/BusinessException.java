package com.schoolmate.api.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {

    private final Map<String, String> details;

    public BusinessException(String message) {
        this(message, null);
    }

    public BusinessException(String message, Map<String, String> details) {
        super(message);
        this.details = details;
    }
}
