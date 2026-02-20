package com.schoolmate.api.repository;

import com.schoolmate.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByRut(String rut);
    Optional<Usuario> findByApoderadoId(UUID apoderadoId);
    Optional<Usuario> findByProfesorId(UUID profesorId);
    Optional<Usuario> findByRefreshToken(String refreshToken);
    Boolean existsByEmail(String email);
    Boolean existsByRut(String rut);
    boolean existsByProfesorId(UUID profesorId);
}
