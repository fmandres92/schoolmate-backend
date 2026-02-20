package com.schoolmate.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClockProvider clockProvider;
    private final MessageSource messageSource;

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorCode errorCode,
            String directMessage
    ) throws IOException {
        String message = directMessage;
        if (message == null || message.isBlank()) {
            message = messageSource.getMessage(
                    errorCode.getMessageKey(),
                    null,
                    errorCode.name(),
                    LocaleContextHolder.getLocale()
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", errorCode.name());
        body.put("message", message);
        body.put("status", errorCode.getStatus().value());
        body.put("path", request.getRequestURI());
        body.put("timestamp", clockProvider.now().toString());

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
