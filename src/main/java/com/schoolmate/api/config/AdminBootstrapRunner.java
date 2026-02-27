package com.schoolmate.api.config;

import com.schoolmate.api.entity.Usuario;
import com.schoolmate.api.enums.Rol;
import com.schoolmate.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@schoolmate.cl}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Value("${app.admin.nombre:Administrador}")
    private String adminNombre;

    @Value("${app.admin.apellido:Sistema}")
    private String adminApellido;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Verificar si ya existe al menos un admin activo
        boolean existeAdmin = usuarioRepository.existsByRolAndActivoTrue(Rol.ADMIN);

        if (existeAdmin) {
            log.info("Admin existente encontrado, omitiendo bootstrap.");
            return;
        }

        // No existe admin — intentar crear uno
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("No se definió ADMIN_PASSWORD (app.admin.password). "
                    + "La aplicación arranca sin usuario administrador. "
                    + "Defina la variable de entorno ADMIN_PASSWORD para crear el admin inicial.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setNombre(adminNombre);
        admin.setApellido(adminApellido);
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);

        usuarioRepository.save(admin);
        log.info("Usuario administrador inicial creado: {}", adminEmail);
    }
}
