package com.schoolmate.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CacheControlInterceptor implements HandlerInterceptor {

    private static final String NO_CACHE = "no-cache";
    private static final String NO_STORE = "no-store";
    private static final String NO_STORE_PRIVATE = "no-store, private";

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            @Nullable ModelAndView modelAndView
    ) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        if (response.getStatus() != HttpServletResponse.SC_OK) {
            return;
        }

        String path = normalizePath(request);
        response.setHeader(HttpHeaders.CACHE_CONTROL, resolveCacheControl(path));
    }

    private String resolveCacheControl(String path) {
        if (isProfesoresSesionesPath(path)
                || isPathOrSubpath(path, "/api/apoderado")
                || isPathOrSubpath(path, "/api/profesor")
                || isPathOrSubpath(path, "/api/auth")) {
            return NO_STORE_PRIVATE;
        }

        if (isPathOrSubpath(path, "/api/matriculas")
                || isPathOrSubpath(path, "/api/asistencia")
                || isPathOrSubpath(path, "/api/alumnos")) {
            return NO_STORE;
        }

        if ("/api/grados".equals(path)
                || "/api/materias".equals(path)
                || isPathOrSubpath(path, "/api/anos-escolares")
                || "/api/cursos".equals(path)
                || isPathOrSubpath(path, "/api/cursos")
                || "/api/malla-curricular".equals(path)
                || "/api/profesores".equals(path)) {
            return NO_CACHE;
        }

        return NO_CACHE;
    }

    private boolean isPathOrSubpath(String path, String basePath) {
        return basePath.equals(path) || path.startsWith(basePath + "/");
    }

    private boolean isProfesoresSesionesPath(String path) {
        return path.matches("^/api/profesores/[^/]+/sesiones$");
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path;
    }
}
