package com.schoolmate.api.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmate.api.common.time.ClockProvider;
import com.schoolmate.api.entity.EventoAuditoria;
import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.repository.EventoAuditoriaRepository;
import com.schoolmate.api.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/api/auth/",
            "/api/dev/"
    );

    private final EventoAuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;
    private final ClockProvider clockProvider;
    private final EntityManager entityManager;

    public AuditoriaAspect(
            EventoAuditoriaRepository auditoriaRepository,
            ObjectMapper objectMapper,
            ClockProvider clockProvider,
            EntityManager entityManager
    ) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = objectMapper;
        this.clockProvider = clockProvider;
        this.entityManager = entityManager;
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void inRestController() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void postMapping() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PutMapping)")
    public void putMapping() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public void patchMapping() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void deleteMapping() {}

    @Pointcut("postMapping() || putMapping() || patchMapping() || deleteMapping()")
    public void mutationMapping() {}

    @AfterReturning(pointcut = "inRestController() && mutationMapping()", returning = "result")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditarOperacion(JoinPoint joinPoint, Object result) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                log.warn("Auditoria: No se pudo obtener RequestAttributes");
                return;
            }

            HttpServletRequest httpRequest = attrs.getRequest();
            String requestUri = httpRequest.getRequestURI();

            if (isExcluded(requestUri)) {
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
                log.warn("Auditoria: No se encontro UserPrincipal para {}", requestUri);
                return;
            }

            String metodoHttp = httpRequest.getMethod();
            String ipAddress = extraerIp(httpRequest);
            UUID anoEscolarId = parseUuidSafe(httpRequest.getHeader("X-Ano-Escolar-Id"));
            int responseStatus = extractResponseStatus(result);
            String requestBodyJson = extractRequestBody(joinPoint);

            EventoAuditoria evento = EventoAuditoria.builder()
                    .usuario(entityManager.getReference(Usuario.class, principal.getId()))
                    .usuarioEmail(principal.getEmail())
                    .usuarioRol(principal.getRol().name())
                    .metodoHttp(metodoHttp)
                    .endpoint(requestUri)
                    .requestBody(requestBodyJson)
                    .responseStatus(responseStatus)
                    .ipAddress(ipAddress)
                    .anoEscolarId(anoEscolarId)
                    .createdAt(clockProvider.now())
                    .build();

            auditoriaRepository.save(evento);
        } catch (IllegalStateException | ClassCastException | DataAccessException e) {
            log.warn("Error al registrar evento de auditoria: {}", e.getMessage(), e);
        }
    }

    private boolean isExcluded(String uri) {
        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String extraerIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private UUID parseUuidSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int extractResponseStatus(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        return 200;
    }

    private String extractRequestBody(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Annotation[][] paramAnnotations = method.getParameterAnnotations();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < paramAnnotations.length; i++) {
                for (Annotation annotation : paramAnnotations[i]) {
                    if (annotation instanceof RequestBody) {
                        if (args[i] != null) {
                            return objectMapper.writeValueAsString(args[i]);
                        }
                        return null;
                    }
                }
            }
            return null;
        } catch (JsonProcessingException e) {
            log.warn("Error al serializar request body para auditoria: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException | ClassCastException e) {
            log.warn("Error inesperado al serializar request body para auditoria: {}", e.getMessage(), e);
            return null;
        }
    }
}
