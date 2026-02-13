package com.schoolmate.api.specification;

import com.schoolmate.api.entity.Alumno;
import org.springframework.data.jpa.domain.Specification;

public class AlumnoSpecifications {

    public static Specification<Alumno> activoTrue() {
        return (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    public static Specification<Alumno> searchByNombre(String q) {
        return (root, query, cb) -> {
            String pattern = "%" + q.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("nombre")), pattern),
                cb.like(cb.lower(root.get("apellido")), pattern)
            );
        };
    }

    public static Specification<Alumno> searchByRutDigits(String digits) {
        return (root, query, cb) -> cb.like(root.get("rut"), "%" + digits + "%");
    }

    public static Specification<Alumno> byIdIn(java.util.List<String> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }
}
