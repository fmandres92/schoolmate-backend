package com.schoolmate.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    AUTH_BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "error.auth.bad_credentials"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "error.auth.unauthorized"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "error.auth.token_expired"),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED, "error.auth.session_revoked"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "error.access.denied"),

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "error.resource.not_found"),
    MATERIAS_NOT_FOUND(HttpStatus.NOT_FOUND, "error.profesor.materias_not_found"),

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "error.validation.failed"),
    BUSINESS_RULE(HttpStatus.BAD_REQUEST, "error.business.rule"),

    PROFESOR_RUT_DUPLICADO(HttpStatus.CONFLICT, "error.profesor.rut_duplicado"),
    PROFESOR_RUT_INMUTABLE(HttpStatus.BAD_REQUEST, "error.profesor.rut_inmutable"),
    PROFESOR_EMAIL_DUPLICADO(HttpStatus.CONFLICT, "error.profesor.email_duplicado"),
    PROFESOR_TELEFONO_DUPLICADO(HttpStatus.CONFLICT, "error.profesor.telefono_duplicado"),
    CURSO_SIN_SECCION_DISPONIBLE(HttpStatus.CONFLICT, "error.curso.sin_seccion_disponible"),
    MATERIA_EXCEDE_MINUTOS_MALLA(HttpStatus.CONFLICT, "error.jornada.materia_excede_minutos_malla"),
    BLOQUE_NO_ES_CLASE(HttpStatus.BAD_REQUEST, "error.jornada.bloque_no_es_clase"),
    BLOQUE_SIN_MATERIA(HttpStatus.BAD_REQUEST, "error.jornada.bloque_sin_materia"),
    MATERIA_NO_EN_MALLA(HttpStatus.BAD_REQUEST, "error.jornada.materia_no_en_malla"),
    PROFESOR_COLISION_HORARIO(HttpStatus.CONFLICT, "error.jornada.profesor_colision_horario"),
    PROFESOR_NO_ENSENA_MATERIA(HttpStatus.CONFLICT, "error.jornada.profesor_no_ensena_materia"),
    BLOQUE_SIN_MATERIA_PARA_PROFESOR(HttpStatus.BAD_REQUEST, "error.jornada.bloque_sin_materia_para_profesor"),

    DATA_INTEGRITY(HttpStatus.CONFLICT, "error.data.integrity"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }
}
