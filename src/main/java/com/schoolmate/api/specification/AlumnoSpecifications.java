package com.schoolmate.api.specification;

import com.schoolmate.api.entity.Alumno;
import org.springframework.data.jpa.domain.Specification;

public final class AlumnoSpecifications {

    private AlumnoSpecifications() {
    }

    public static Specification<Alumno> activoTrue() {
        return (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    public static Specification<Alumno> byCursoId(String cursoId) {
        return (root, query, cb) -> cb.equal(root.get("curso").get("id"), cursoId);
    }

    public static Specification<Alumno> byGradoId(String gradoId) {
        return (root, query, cb) -> cb.equal(root.get("curso").get("grado").get("id"), gradoId);
    }

    public static Specification<Alumno> searchByRutDigits(String rutDigits) {
        return (root, query, cb) -> {
            String normalized = rutDigits.replaceAll("\\D", "");
            if (normalized.length() < 5) {
                return cb.conjunction();
            }

            return cb.like(
                    cb.function(
                            "regexp_replace",
                            String.class,
                            root.get("rut"),
                            cb.literal("[^0-9]"),
                            cb.literal(""),
                            cb.literal("g")
                    ),
                    normalized + "%"
            );
        };
    }

    public static Specification<Alumno> searchByNombre(String term) {
        return (root, query, cb) -> {
            String normalized = term.trim().toLowerCase();
            if (normalized.length() < 3) {
                return cb.conjunction();
            }

            String likePattern = "%" + normalized + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nombre")), likePattern),
                    cb.like(cb.lower(root.get("apellido")), likePattern),
                    cb.like(
                            cb.lower(cb.concat(cb.concat(root.get("nombre"), " "), root.get("apellido"))),
                            likePattern
                    )
            );
        };
    }
}
