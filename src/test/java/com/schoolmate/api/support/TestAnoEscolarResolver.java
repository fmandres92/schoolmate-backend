package com.schoolmate.api.support;

import com.schoolmate.api.config.AnoEscolarHeaderInterceptor;
import com.schoolmate.api.entity.AnoEscolar;
import com.schoolmate.api.security.AnoEscolarActivo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolver de test para @AnoEscolarActivo en tests standalone de MockMvc.
 * Simula el comportamiento del AnoEscolarArgumentResolver de producción
 * sin necesidad de levantar el contexto completo de Spring.
 *
 * Uso: MockMvcBuilders.standaloneSetup(controller)
 *          .setCustomArgumentResolvers(new TestAnoEscolarResolver())
 */
public class TestAnoEscolarResolver implements HandlerMethodArgumentResolver {

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
        WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        AnoEscolar anoEscolar = (AnoEscolar) request.getAttribute(AnoEscolarHeaderInterceptor.REQUEST_ATTR);
        AnoEscolarActivo annotation = parameter.getParameterAnnotation(AnoEscolarActivo.class);

        if (anoEscolar == null && annotation != null && annotation.required()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header X-Ano-Escolar-Id es requerido");
        }
        return anoEscolar;
    }
}
