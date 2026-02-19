package com.schoolmate.api.config;

import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.exception.ApiException;
import com.schoolmate.api.exception.ErrorCode;
import com.schoolmate.api.security.AnoEscolarActivo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

@Component
public class AnoEscolarArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AnoEscolarActivo.class)
                && AnoEscolar.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new ApiException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "No se pudo obtener el request HTTP",
                    Map.of());
        }

        AnoEscolar anoEscolar = (AnoEscolar) request.getAttribute(AnoEscolarHeaderInterceptor.REQUEST_ATTR);
        AnoEscolarActivo annotation = parameter.getParameterAnnotation(AnoEscolarActivo.class);
        if (anoEscolar == null && annotation != null && annotation.required()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Header X-Ano-Escolar-Id es requerido",
                    Map.of());
        }

        return anoEscolar;
    }
}
