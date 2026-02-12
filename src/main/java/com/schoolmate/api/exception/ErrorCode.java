package com.schoolmate.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    AUTH_BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.auth.bad_credentials"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.access.denied"),

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.resource.not_found"),
    MATERIAS_NOT_FOUND(HttpStatus.NOT_FOUND, "error.profesor.materias_not_found"),

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "error.validation.failed"),
    BUSINESS_RULE(HttpStatus.BAD_REQUEST, "error.business.rule"),

    PROFESOR_RUT_DUPLICADO(HttpStatus.CONFLICT, "error.profesor.rut_duplicado"),
    PROFESOR_EMAIL_DUPLICADO(HttpStatus.CONFLICT, "error.profesor.email_duplicado"),
    PROFESOR_TELEFONO_DUPLICADO(HttpStatus.CONFLICT, "error.profesor.telefono_duplicado"),

    DATA_INTEGRITY(HttpStatus.CONFLICT, "error.data.integrity"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }
}
