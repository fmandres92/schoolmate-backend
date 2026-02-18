package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByRut(String rut);
    Optional<Usuario> findByApoderadoId(String apoderadoId);
    Boolean existsByEmail(String email);
    Boolean existsByRut(String rut);
    boolean existsByProfesorId(String profesorId);
}
