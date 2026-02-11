# SchoolMate Hub API - Documentación Técnica Completa

> **Versión**: 0.2.1  
> **Última Actualización**: Febrero 2026  
> **Estado**: ✅ FASE 2 COMPLETADA - Catálogo Base (Años Escolares, Grados, Materias) operativo  
> **Nota**: Sistema de estados de Año Escolar refactorizado a cálculo automático por fechas  

---

## 1. VISIÓN GENERAL DEL PROYECTO

### 1.1 Descripción
**SchoolMate Hub API** es el backend REST API del Sistema de Gestión Escolar SchoolMate Hub. Proporciona autenticación JWT, persistencia de datos, y una arquitectura escalable basada en Spring Boot. El backend implementa el patrón **Use Case** en lugar de Services tradicionales para mantener el código modular y mantenible.

### 1.2 Características Principales
- **Autenticación JWT**: Tokens seguros con claims personalizados (rol, profesorId, alumnoId)
- **Arquitectura Use Case**: Una clase = una acción de negocio
- **Triple Rol**: ADMIN, PROFESOR, APODERADO con permisos diferenciados
- **Base de Datos Flexible**: H2 para desarrollo, PostgreSQL para producción
- **Migraciones Automáticas**: Flyway para control de schema
- **Validación de Datos**: Bean Validation en DTOs
- **CORS Configurado**: Listo para integración con frontend

### 1.3 Stack Tecnológico

#### Core
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 LTS | Lenguaje principal |
| Spring Boot | 4.0.2 | Framework principal |
| Spring Security | 7.0.2 | Autenticación y autorización |
| Spring Data JPA | 4.0.2 | Acceso a datos |
| Hibernate | 7.2.1 | ORM |

#### Seguridad
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| JJWT | 0.12.6 | Generación y validación de JWT |
| BCrypt | - | Hash de contraseñas |

#### Base de Datos
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| PostgreSQL | 42.7.9 | Base de datos (Supabase) |
| Flyway | 11.14.1 | Migraciones de schema |

#### Utilidades
| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Lombok | 1.18.36 | Reducción de boilerplate |
| Maven | 3.9.12 | Gestión de dependencias |

---

## 2. ARQUITECTURA DEL SISTEMA

### 2.1 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENTE (Frontend)                        │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   REACT / VITE                            │  │
│  │              http://localhost:5173                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                   │
│                              │ HTTP / JSON                       │
│                              ▼                                   │
└──────────────────────────────┼───────────────────────────────────┘
                               │
┌──────────────────────────────┼───────────────────────────────────┐
│                         SPRING BOOT API                          │
│                    http://localhost:8080                         │
│                              │                                   │
│  ┌───────────────────────────┴──────────────────────────────┐   │
│  │                    SECURITY LAYER                         │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │ JWTAuthenticationFilter                             │ │   │
│  │  │ - Extrae token del header                           │ │   │
│  │  │ - Valida token                                      │ │   │
│  │  │ - Carga UserPrincipal en SecurityContext            │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │ SecurityConfig                                      │ │   │
│  │  │ - Configura CORS                                    │ │   │
│  │  │ - Define rutas públicas/protegidas                  │ │   │
│  │  │ - Habilita @PreAuthorize                            │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   CONTROLLER LAYER                        │   │
│  │                                                            │   │
│  │  AuthController                                            │   │
│  │  ├── POST /api/auth/login    → LoginUsuario.execute()     │   │
│  │  └── GET  /api/auth/me       → Datos del token            │   │
│  │                                                            │   │
│  │  [Futuros controllers por dominio]                         │   │
│  │  ├── AnoEscolarController                                 │   │
│  │  ├── CursoController                                      │   │
│  │  ├── ProfesorController                                   │   │
│  │  ├── AlumnoController                                     │   │
│  │  ├── AsistenciaController                                 │   │
│  │  └── ReporteController                                    │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    USE CASE LAYER                         │   │
│  │                                                            │   │
│  │  LoginUsuario                                              │   │
│  │  ├── Busca usuario por email                              │   │
│  │  ├── Valida contraseña con BCrypt                         │   │
│  │  ├── Verifica que esté activo                             │   │
│  │  └── Genera JWT con claims                                │   │
│  │                                                            │   │
│  │  [Futuros use cases por dominio]                           │   │
│  │  ├── GuardarAsistenciaClase                               │   │
│  │  ├── ObtenerClasesHoyProfesor                             │   │
│  │  ├── ObtenerAsistenciaMensualAlumno                       │   │
│  │  └── CrearReporte                                         │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  REPOSITORY LAYER                         │   │
│  │                                                            │   │
│  │  UsuarioRepository extends JpaRepository                  │   │
│  │  ├── findByEmail(String email)                            │   │
│  │  └── existsByEmail(String email)                          │   │
│  │                                                            │   │
│  │  [Futuros repositories]                                    │   │
│  │  ├── AnoEscolarRepository                                 │   │
│  │  ├── CursoRepository                                      │   │
│  │  ├── ProfesorRepository                                   │   │
│  │  └── AsistenciaClaseRepository                            │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    ENTITY LAYER                           │   │
│  │                                                            │   │
│  │  Usuario (JPA Entity)                                      │   │
│  │  ├── id, email, passwordHash, nombre, apellido            │   │
│  │  ├── rol (ADMIN/PROFESOR/APODERADO)                       │   │
│  │  ├── profesorId, alumnoId (nullable)                      │   │
│  │  └── activo, createdAt, updatedAt                         │   │
│  │                                                            │   │
│  │  [Futuras entidades]                                       │   │
│  │  ├── AnoEscolar, Grado, Materia, Curso                    │   │
│  │  ├── Profesor, Alumno, Asignacion                         │   │
│  │  ├── AsistenciaClase, RegistroAsistencia                  │   │
│  │  └── Reporte                                              │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              DATABASE (H2 / PostgreSQL)                   │   │
│  │                                                            │   │
│  │  Flyway Migrations:                                        │   │
│  │  ├── V1__create_usuario_table.sql                         │   │
│  │  └── V2__seed_usuarios.sql                                │   │
│  │                                                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 Flujo de Autenticación

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Cliente   │────▶│   Login     │────▶│   Login     │────▶│   Usuario   │
│   (React)   │     │   Endpoint  │     │   Use Case  │     │   Repository│
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │                   │
      │ POST /login       │                   │                   │
      │ {email, password} │                   │                   │
      │──────────────────▶│                   │                   │
      │                   │ execute(request)  │                   │
      │                   │──────────────────▶│                   │
      │                   │                   │ findByEmail(email)│
      │                   │                   │──────────────────▶│
      │                   │                   │                   │
      │                   │                   │◀──────────────────│
      │                   │                   │ Usuario entity    │
      │                   │                   │                   │
      │                   │                   │ BCrypt check      │
      │                   │                   │ password vs hash  │
      │                   │                   │                   │
      │                   │                   │ JwtTokenProvider  │
      │                   │                   │ generateToken()   │
      │                   │                   │                   │
      │                   │◀──────────────────│ AuthResponse      │
      │                   │ {token, user}     │                   │
      │                   │                   │                   │
      │◀──────────────────│ 200 OK            │                   │
      │ JWT + User data   │                   │                   │
      │                   │                   │                   │
      │                   │                   │                   │
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Cliente   │────▶│   Request   │────▶│   JWT       │────▶│   Security  │
│   (React)   │     │   Protegido │     │   Filter    │     │   Context   │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │                   │
      │ GET /api/me       │                   │                   │
      │ Authorization:    │                   │                   │
      │ Bearer <token>    │                   │                   │
      │──────────────────▶│                   │                   │
      │                   │                   │ Extract token     │
      │                   │                   │ Validate token    │
      │                   │                   │                   │
      │                   │                   │ Load user from DB │
      │                   │                   │                   │
      │                   │                   │ Set Authentication│
      │                   │                   │ in SecurityContext│
      │                   │                   │                   │
      │                   │                   │◀──────────────────│
      │                   │                   │ UserPrincipal     │
      │                   │                   │                   │
      │                   │◀──────────────────│ Continue filter   │
      │                   │ chain             │                   │
      │                   │                   │                   │
      │◀──────────────────│ 200 OK            │                   │
      │ User data from    │ {id, email, etc}  │                   │
      │ @Authentication   │                   │                   │
      │ Principal         │                   │                   │
```

### 2.3 Patrones de Diseño

1. **Use Case Pattern**: Cada acción de negocio es una clase con un método `execute()`
2. **Repository Pattern**: Spring Data JPA para acceso a datos
3. **DTO Pattern**: Objetos de transferencia entre capas
4. **JWT Authentication**: Tokens stateless para sesiones
5. **Role-Based Access Control (RBAC)**: `@PreAuthorize` en controllers
6. **Ownership Pattern**: Validación de propiedad en use cases

---

## 3. ESTRUCTURA DE CARPETAS

```
schoolmate-hub-api/
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/com/schoolmate/api/
│   │   │   ├── 📄 SchoolmateApiApplication.java    # Punto de entrada
│   │   │   │
│   │   │   ├── 📁 config/                         # Configuraciones
│   │   │   │   ├── 📄 SecurityConfig.java         # Configuración Spring Security
│   │   │   │   └── 📄 CorsConfig.java             # Configuración CORS
│   │   │   │
│   │   │   ├── 📁 security/                       # Seguridad JWT
│   │   │   │   ├── 📄 JwtConfig.java              # Configuración JWT (properties)
│   │   │   │   ├── 📄 JwtTokenProvider.java       # Genera y valida tokens
│   │   │   │   ├── 📄 JwtAuthenticationFilter.java # Filtro de autenticación
│   │   │   │   ├── 📄 UserPrincipal.java          # Implementa UserDetails
│   │   │   │   └── 📄 CustomUserDetailsService.java # Carga usuario desde BD
│   │   │   │
│  │  │  ├── 📁 entity/                         # Entidades JPA
│  │  │  │   ├── 📄 Usuario.java                # Tabla usuario
│  │  │  │   ├── 📄 AnoEscolar.java             # Tabla año escolar
│  │  │  │   ├── 📄 Grado.java                  # Tabla grado
│  │  │  │   └── 📄 Materia.java                # Tabla materia
│  │  │  │
│  │  │  ├── 📁 enums/                          # Enumeraciones
│  │  │  │   ├── 📄 Rol.java                    # ADMIN, PROFESOR, APODERADO
│  │  │  │   └── 📄 EstadoAnoEscolar.java       # FUTURO, PLANIFICACION, ACTIVO, CERRADO
│  │  │  │
│  │  │  ├── 📁 repository/                     # Repositorios Spring Data
│  │  │  │   ├── 📄 UsuarioRepository.java      # Acceso a tabla usuario
│  │  │  │   ├── 📄 AnoEscolarRepository.java   # Acceso a tabla año escolar
│  │  │  │   ├── 📄 GradoRepository.java        # Acceso a tabla grado
│  │  │  │   └── 📄 MateriaRepository.java      # Acceso a tabla materia
│  │  │  │
│  │  │  ├── 📁 usecase/                        # Casos de uso
│  │  │  │   └── 📁 auth/
│  │  │  │       └── 📄 LoginUsuario.java       # Login de usuarios
│  │  │  │
│  │  │  ├── 📁 controller/                     # Controladores REST
│  │  │  │   ├── 📄 AuthController.java         # Endpoints de auth
│  │  │  │   ├── 📄 AnoEscolarController.java   # Endpoints de años escolares
│  │  │  │   ├── 📄 GradoController.java        # Endpoints de grados
│  │  │  │   └── 📄 MateriaController.java      # Endpoints de materias
│   │   │   │
│   │   │   ├── 📁 dto/                            # Data Transfer Objects
│   │   │   │   ├── 📁 request/
│   │   │   │   │   ├── 📄 LoginRequest.java       # Request de login
│   │   │   │   │   └── 📄 AnoEscolarRequest.java  # Request de año escolar
│   │   │   │   └── 📁 response/
│   │   │   │       ├── 📄 AuthResponse.java       # Response de auth
│   │   │   │       └── 📄 AnoEscolarResponse.java # Response de año escolar
│   │   │   │
│   │   │   └── 📁 exception/                      # Manejo de excepciones
│   │   │       ├── 📄 GlobalExceptionHandler.java # Handler global
│   │   │       ├── 📄 ResourceNotFoundException.java
│   │   │       ├── 📄 UnauthorizedException.java
│   │   │       └── 📄 BusinessException.java
│   │   │
│   │   └── 📁 resources/                          # Recursos
│   │       ├── 📄 application.yml                 # Configuración base
│   │       ├── 📄 application-dev.yml             # Configuración desarrollo (H2)
│   │       ├── 📄 application-prod.yml            # Configuración producción (PostgreSQL)
│   │       └── 📁 db/migration/                   # Migraciones Flyway
│   │           ├── 📄 V1__create_usuario_table.sql
│   │           ├── 📄 V2__seed_usuarios.sql
│   │           ├── 📄 V3__create_catalogo_base.sql
│   │           └── 📄 V4__seed_catalogo_base.sql
│   │
│   └── 📁 test/                                   # Tests
│       └── 📁 java/com/schoolmate/api/
│           └── 📄 SchoolmateApiApplicationTests.java
│
├── 📄 pom.xml                                     # Configuración Maven
├── 📄 README.md                                   # Documentación básica
└── 📄 PROJECT_DOCUMENTATION.md                    # Este archivo
```

---

## 4. SISTEMA DE AUTENTICACIÓN Y SEGURIDAD

### 4.1 Modelo de Usuarios

```java
// Entity: Usuario.java
@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    @Id
    private String id;                    // 'admin-1', 'prof-1', 'apod-1'
    
    @Column(nullable = false, unique = true)
    private String email;                 // 'admin@edugestio.cl'
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;          // BCrypt hash
    
    @Column(nullable = false)
    private String nombre;                // 'Carlos'
    
    @Column(nullable = false)
    private String apellido;              // 'Mendoza'
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;                      // ADMIN, PROFESOR, APODERADO
    
    @Column(name = "profesor_id")
    private String profesorId;            // FK a profesor (nullable)
    
    @Column(name = "alumno_id")
    private String alumnoId;              // FK a alumno (nullable)
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### 4.2 Roles del Sistema

```java
// Enum: Rol.java
public enum Rol {
    ADMIN,      // Director/Coordinador - Acceso total
    PROFESOR,   // Docente - Solo sus clases y alumnos
    APODERADO   // Padre/Tutor - Solo su hijo vinculado
}
```

### 4.3 Usuarios de Prueba (Seed Data)

| ID | Email | Password | Nombre | Apellido | Rol | profesorId | alumnoId |
|----|-------|----------|--------|----------|-----|------------|----------|
| admin-1 | admin@edugestio.cl | admin123 | Carlos | Mendoza | ADMIN | null | null |
| prof-1 | profesor@edugestio.cl | prof123 | Carlos | Rodríguez | PROFESOR | p2 | null |
| apod-1 | apoderado@edugestio.cl | apod123 | Carlos | Soto | APODERADO | null | al1 |

**Notas:**
- Las contraseñas están hasheadas con BCrypt (strength 10)
- El profesor está vinculado al profesor con ID 'p2' del frontend
- El apoderado está vinculado al alumno con ID 'al1' (Benjamín Soto Pérez)

### 4.4 UserPrincipal (UserDetails)

```java
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {
    private final String id;
    private final String email;
    private final String password;
    private final Rol rol;
    private final String profesorId;
    private final String alumnoId;
    private final String nombre;
    private final String apellido;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        // Retorna: ROLE_ADMIN, ROLE_PROFESOR, o ROLE_APODERADO
    }
    
    @Override
    public String getUsername() { return email; }
    
    // ... otros métodos de UserDetails
}
```

### 4.5 Claims del JWT

```json
{
  "sub": "admin@edugestio.cl",
  "id": "admin-1",
  "rol": "ADMIN",
  "profesorId": null,
  "alumnoId": null,
  "nombre": "Carlos",
  "apellido": "Mendoza",
  "iat": 1707585600,
  "exp": 1707672000
}
```

### 4.6 Flujo de Autenticación

```
1. CLIENTE ENVÍA CREDENCIALES
   POST /api/auth/login
   {
     "email": "admin@edugestio.cl",
     "password": "admin123"
   }
   
2. AUTHCONTROLLER RECIBE REQUEST
   ↓
   Llama a loginUsuario.execute(request)
   
3. LOGINUSUARIO EJECUTA
   a. Busca usuario por email
      usuarioRepository.findByEmail(email)
   
   b. Valida que exista
      → Si no existe: BadCredentialsException
   
   c. Valida que esté activo
      → Si no está activo: BadCredentialsException
   
   d. Valida contraseña con BCrypt
      passwordEncoder.matches(password, hash)
      → Si no coincide: BadCredentialsException
   
   e. Genera JWT
      UserPrincipal principal = UserPrincipal.fromUsuario(usuario)
      String token = jwtTokenProvider.generateToken(principal)
   
   f. Retorna AuthResponse
      {
        "token": "eyJhbGciOiJIUzI1NiIs...",
        "tipo": "Bearer",
        "id": "admin-1",
        "email": "admin@edugestio.cl",
        "nombre": "Carlos",
        "apellido": "Mendoza",
        "rol": "ADMIN",
        "profesorId": null,
        "alumnoId": null
      }

4. CLIENTE RECIBE TOKEN
   Almacena en localStorage/sessionStorage
   
5. CLIENTE ENVÍA TOKEN EN SUBSECUENTES REQUESTS
   Authorization: Bearer <token>
   
6. JWTAUTHENTICATIONFILTER INTERCEPTA
   a. Extrae token del header
   b. Valida token (firma, expiración)
   c. Extrae email del token
   d. Carga UserDetails desde BD
   e. Crea Authentication y lo setea en SecurityContext
   
7. CONTROLLER PUEDE ACCEDER AL USUARIO
   @AuthenticationPrincipal UserPrincipal user
```

### 4.7 Protección de Rutas

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    // PÚBLICO - No requiere token
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // ...
    }
    
    // PROTEGIDO - Requiere token válido
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserPrincipal user) {
        // user contiene los datos del JWT
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "rol", user.getRol().name()
        ));
    }
}

// Ejemplo futuro con roles
@RestController
@RequestMapping("/api/cursos")
public class CursoController {
    
    // Solo ADMIN puede crear cursos
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Curso> crear(@RequestBody CursoRequest request) {
        // ...
    }
    
    // ADMIN y PROFESOR pueden ver, pero PROFESOR solo los suyos
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR')")
    public ResponseEntity<Curso> obtener(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal user) {
        // Validar propiedad si es PROFESOR
        // ...
    }
}
```

---

## 5. MODELO DE DATOS

### 5.1 Entidades Actuales (Fase 2)

#### Usuario
| Campo | Tipo | Constraints | Descripción |
|-------|------|-------------|-------------|
| id | VARCHAR(36) | PK | Identificador único |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email del usuario |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt hash |
| nombre | VARCHAR(100) | NOT NULL | Nombre |
| apellido | VARCHAR(100) | NOT NULL | Apellido |
| rol | VARCHAR(20) | NOT NULL | ADMIN/PROFESOR/APODERADO |
| profesor_id | VARCHAR(36) | NULL | FK a profesor (solo PROFESOR) |
| alumno_id | VARCHAR(36) | NULL | FK a alumno (solo APODERADO) |
| activo | BOOLEAN | NOT NULL, DEFAULT TRUE | Estado del usuario |
| created_at | TIMESTAMP | NOT NULL | Fecha de creación |
| updated_at | TIMESTAMP | NOT NULL | Fecha de actualización |

**Índices:**
- idx_usuario_email (email)
- idx_usuario_rol (rol)

#### AnoEscolar
| Campo | Tipo | Constraints | Descripción |
|-------|------|-------------|-------------|
| id | VARCHAR(36) | PK | Identificador único |
| ano | INTEGER | NOT NULL, UNIQUE | Año escolar (ej: 2026) |
| fecha_inicio_planificacion | DATE | NOT NULL | Inicio de planificación (3 meses antes) |
| fecha_inicio | DATE | NOT NULL | Fecha de inicio del año |
| fecha_fin | DATE | NOT NULL | Fecha de término del año |
| created_at | TIMESTAMP | NOT NULL | Fecha de creación |
| updated_at | TIMESTAMP | NOT NULL | Fecha de actualización |

**Constraints:**
- chk_fechas_orden: fecha_inicio_planificacion < fecha_inicio < fecha_fin
- uq_ano_escolar_ano: UNIQUE (ano)

**Estado Calculado (no persistido):**
El estado se calcula automáticamente comparando `LocalDate.now()` con las fechas:
- **FUTURO**: Hoy < fecha_inicio_planificacion
- **PLANIFICACION**: fecha_inicio_planificacion <= hoy < fecha_inicio
- **ACTIVO**: fecha_inicio <= hoy <= fecha_fin
- **CERRADO**: hoy > fecha_fin

**Índices:**
- idx_ano_escolar_ano (ano)

#### Grado
| Campo | Tipo | Constraints | Descripción |
|-------|------|-------------|-------------|
| id | VARCHAR(36) | PK | Identificador único |
| nombre | VARCHAR(50) | NOT NULL | Nombre del grado (ej: "1° Básico") |
| nivel | INTEGER | NOT NULL | Nivel numérico (1-8) |
| created_at | TIMESTAMP | NOT NULL | Fecha de creación |
| updated_at | TIMESTAMP | NOT NULL | Fecha de actualización |

**Índices:**
- idx_grado_nivel (nivel)

#### Materia
| Campo | Tipo | Constraints | Descripción |
|-------|------|-------------|-------------|
| id | VARCHAR(36) | PK | Identificador único |
| nombre | VARCHAR(100) | NOT NULL | Nombre de la materia |
| icono | VARCHAR(50) | NULL | Icono Lucide (ej: "Calculator") |
| created_at | TIMESTAMP | NOT NULL | Fecha de creación |
| updated_at | TIMESTAMP | NOT NULL | Fecha de actualización |

#### MateriaGrado (Tabla intermedia)
| Campo | Tipo | Constraints | Descripción |
|-------|------|-------------|-------------|
| materia_id | VARCHAR(36) | PK, FK | Referencia a materia |
| grado_id | VARCHAR(36) | PK, FK | Referencia a grado |

**Notas:**
- Relación muchos-a-muchos entre Materia y Grado
- Una materia puede aplicar a múltiples grados
- Religión solo aplica a grados 3-8

### 5.2 Entidades Futuras (Fases 3-9)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  ano_escolar │1───*│    curso     │*───1│    grado     │
│              │     │              │     │              │
│ id (PK)      │     │ id (PK)      │     │ id (PK)      │
│ ano          │     │ nombre       │     │ nombre       │
│ fecha_inicio │     │ letra        │     │ nivel        │
│ fecha_fin    │     │ grado_id(FK) │     └──────────────┘
│ fecha_inicio_│     │ ano_id (FK)  │
│  planificacion
└──────────────┘     │ activo       │
                     └──────┬───────┘
                            │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │   alumno     │  │  asignacion  │  │   reporte    │
    │              │  │              │  │              │
    │ id (PK)      │  │ id (PK)      │  │ id (PK)      │
    │ rut          │  │ curso_id(FK) │  │ alumno_id(FK)│
    │ nombre       │  │ profesor_id  │  │ profesor_id  │
    │ apellido     │  │ materia_id   │  │ materia      │
    │ curso_id(FK) │  │ dia_semana   │  │ gravedad     │
    │ apoderado_*  │  │ hora_inicio  │  │ estado       │
    │ activo       │  │ hora_fin     │  │ descripcion  │
    └──────────────┘  └──────┬───────┘  └──────────────┘
                             │
                      ┌──────┴───────┐
                      │   profesor   │
                      │              │
                      │ id (PK)      │
                      │ rut          │
                      │ nombre       │
                      │ apellido     │
                      │ email        │
                      │ materias     │
                      │ activo       │
                      └──────┬───────┘
                             │
                      ┌──────┴───────┐
                      │  asistencia  │
                      │   _clase     │
                      │              │
                      │ id (PK)      │
                      │ fecha        │
                      │ asignacion_id│
                      │ profesor_id  │
                      │ curso_id     │
                      │ completada   │
                      └──────┬───────┘
                             │
                      ┌──────┴───────┐
                      │  registro    │
                      │ _asistencia  │
                      │              │
                      │ id (PK)      │
                      │ asistencia_  │
                      │  clase_id(FK)│
                      │ alumno_id(FK)│
                      │ estado       │
                      │ observacion  │
                      │ hora_registro│
                      └──────────────┘

   ┌──────────────┐     ┌──────────────┐
   │   materia    │     │   usuario    │
   │              │     │              │
   │ id (PK)      │     │ id (PK)      │
   │ nombre       │     │ email        │
   │ icono        │     │ password_hash│
   └──────────────┘     │ rol          │
                        │ profesor_id  │
   ┌──────────────┐     │ alumno_id    │
   │ materia_grado│     │ activo       │
   │              │     └──────────────┘
   │ materia_id   │
   │ grado_id     │
   └──────────────┘
```

### 5.3 Migraciones Flyway

**V1__create_usuario_table.sql**
```sql
CREATE TABLE usuario (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    profesor_id VARCHAR(36),
    alumno_id VARCHAR(36),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_rol ON usuario(rol);
```

**V2__seed_usuarios.sql**
```sql
INSERT INTO usuario (id, email, password_hash, nombre, apellido, rol, profesor_id, alumno_id, activo)
VALUES
    ('admin-1', 'admin@edugestio.cl', '$2a$10$...', 'Carlos', 'Mendoza', 'ADMIN', NULL, NULL, TRUE),
    ('prof-1', 'profesor@edugestio.cl', '$2a$10$...', 'Carlos', 'Rodríguez', 'PROFESOR', 'p2', NULL, TRUE),
    ('apod-1', 'apoderado@edugestio.cl', '$2a$10$...', 'Carlos', 'Soto', 'APODERADO', NULL, 'al1', TRUE);
```

**V3__create_catalogo_base.sql**
```sql
-- Tabla: ano_escolar
CREATE TABLE ano_escolar (
    id VARCHAR(36) PRIMARY KEY,
    ano INTEGER NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ano_escolar_activo ON ano_escolar(activo);
CREATE INDEX idx_ano_escolar_ano ON ano_escolar(ano);

-- Tabla: grado
CREATE TABLE grado (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    nivel INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_grado_nivel ON grado(nivel);

-- Tabla: materia
CREATE TABLE materia (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    icono VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla intermedia: materia_grado
CREATE TABLE materia_grado (
    materia_id VARCHAR(36) NOT NULL,
    grado_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (materia_id, grado_id),
    FOREIGN KEY (materia_id) REFERENCES materia(id),
    FOREIGN KEY (grado_id) REFERENCES grado(id)
);
```

**V4__seed_catalogo_base.sql**
```sql
-- Años Escolares (3 años con fecha_inicio_planificacion calculada)
-- El estado (FUTURO, PLANIFICACION, ACTIVO, CERRADO) se calcula automáticamente
INSERT INTO ano_escolar (id, ano, fecha_inicio_planificacion, fecha_inicio, fecha_fin) VALUES
    ('1', 2025, '2024-12-01', '2025-03-01', '2025-12-15'),
    ('2', 2026, '2025-12-01', '2026-03-01', '2026-12-15'),
    ('3', 2027, '2026-12-01', '2027-03-01', '2027-12-15');

-- Grados (8 grados: 1° Básico a 8° Básico)
INSERT INTO grado (id, nombre, nivel) VALUES
    ('1', '1° Básico', 1),
    ('2', '2° Básico', 2),
    ('3', '3° Básico', 3),
    ('4', '4° Básico', 4),
    ('5', '5° Básico', 5),
    ('6', '6° Básico', 6),
    ('7', '7° Básico', 7),
    ('8', '8° Básico', 8);

-- Materias (11 materias con iconos Lucide)
INSERT INTO materia (id, nombre, icono) VALUES
    ('1',  'Matemáticas',              'Calculator'),
    ('2',  'Lenguaje y Comunicación',  'BookOpen'),
    ('3',  'Ciencias Naturales',       'Microscope'),
    ('4',  'Historia y Geografía',     'Globe'),
    ('5',  'Inglés',                   'Languages'),
    ('6',  'Educación Física',         'Dumbbell'),
    ('7',  'Artes Visuales',           'Palette'),
    ('8',  'Música',                   'Music'),
    ('9',  'Tecnología',              'Monitor'),
    ('10', 'Orientación',             'Heart'),
    ('11', 'Religión',                'BookHeart');
```

---

## 6. USE CASES

### 6.1 Filosofía de Use Cases

En lugar de Services tradicionales con múltiples métodos, usamos **una clase por acción de negocio**:

```
❌ Service tradicional (anti-patrón)
AsistenciaService.java
├── tomarAsistencia()
├── obtenerAsistencia()
├── obtenerAsistenciaMensual()
├── obtenerAsistenciaPromedio()
└── ... 15 métodos más

✅ Use Case pattern (nuestro enfoque)
usecase/asistencia/
├── GuardarAsistenciaClase.java
├── ObtenerAsistenciaClase.java
├── ObtenerAsistenciaMensualAlumno.java
└── ObtenerAsistenciaPromedioCurso.java
```

### 6.2 Reglas de Use Cases

1. **Un use case = una clase = una acción de negocio**
2. **Un solo método público**: `execute(...)`
3. **Nombre**: Verbo + Sustantivo (`GuardarAsistenciaClase`)
4. **Sin interfaces**: Clases concretas directas
5. **Sin herencia**: No hay `BaseUseCase<T>`
6. **Inyección de dependencias**: Repositorios y servicios vía constructor

### 6.3 Anatomía de un Use Case

```java
@Component
@RequiredArgsConstructor
public class LoginUsuario {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse execute(LoginRequest request) {
        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        
        // 2. Validar estado
        if (!usuario.getActivo()) {
            throw new BadCredentialsException("Usuario desactivado");
        }
        
        // 3. Validar contraseña
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        
        // 4. Generar JWT
        UserPrincipal principal = UserPrincipal.fromUsuario(usuario);
        String token = tokenProvider.generateToken(principal);
        
        // 5. Retornar respuesta
        return AuthResponse.builder()
            .token(token)
            .tipo("Bearer")
            .id(usuario.getId())
            .email(usuario.getEmail())
            .nombre(usuario.getNombre())
            .apellido(usuario.getApellido())
            .rol(usuario.getRol().name())
            .profesorId(usuario.getProfesorId())
            .alumnoId(usuario.getAlumnoId())
            .build();
    }
}
```

### 6.4 Use Cases Implementados

| Dominio | Use Case | Descripción | Estado |
|---------|----------|-------------|--------|
| **Auth** | LoginUsuario | Login con JWT | ✅ Implementado |

**Nota sobre Año Escolar:** El sistema de activación manual fue eliminado. El estado ahora se calcula automáticamente basado en fechas. Ver sección 7.1 - Años Escolares.

### 6.5 Use Cases Futuros

| Dominio | Use Case | Descripción |
|---------|----------|-------------|
| **Auth** | RefrescarToken | Renovación de token |
| **Curso** | ObtenerDetalleCurso | Curso + alumnos + horario |
| **Profesor** | ObtenerDetalleProfesor | Profesor + asignaciones |
| | ObtenerClasesHoyProfesor | Clases del día con estados |
| **Alumno** | ObtenerDetalleAlumno | Alumno + curso + asistencia |
| **Asistencia** | GuardarAsistenciaClase | Toma de lista con validaciones |
| | ObtenerAsistenciaClase | Por asignación y fecha |
| | ObtenerAsistenciaMensualAlumno | Para calendario apoderado |
| | ObtenerResumenAsistenciaAlumno | Porcentajes y totales |
| | ObtenerAsistenciaPromedioCurso | Para reportes admin |
| **Reporte** | CrearReporte | Profesor crea reporte |
| | CambiarEstadoReporte | Admin cambia estado |
| **Dashboard** | ObtenerDashboardAdmin | Stats globales |
| | ObtenerDashboardProfesor | Clases hoy + promedios |
| | ObtenerDashboardApoderado | Info alumno + asistencia |

---

## 7. API REST

### 7.1 Endpoints Actuales (Fase 2)

#### Autenticación

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| POST | `/api/auth/login` | Login con email y password | Público |
| GET | `/api/auth/me` | Datos del usuario autenticado | Autenticado |

#### Años Escolares

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/api/anos-escolares` | Listar todos con estado calculado | ADMIN |
| GET | `/api/anos-escolares/{id}` | Obtener por ID | ADMIN |
| GET | `/api/anos-escolares/activo` | Obtener año escolar activo actual | Autenticado |
| POST | `/api/anos-escolares` | Crear nuevo año escolar | ADMIN |
| PUT | `/api/anos-escolares/{id}` | Actualizar fechas (solo si no CERRADO) | ADMIN |

**Estado Calculado:**
Los años escolares retornan un campo `estado` calculado automáticamente:
- `FUTURO`: Hoy < fecha_inicio_planificacion
- `PLANIFICACION`: fecha_inicio_planificacion <= hoy < fecha_inicio
- `ACTIVO`: fecha_inicio <= hoy <= fecha_fin
- `CERRADO`: hoy > fecha_fin

**Reglas de Negocio:**
- Solo puede haber UN año activo a la vez (garantizado por fechas sin solapamiento)
- Años CERRADOS son inmutables (no se pueden editar)
- Validaciones al crear/editar:
  - `planificacion < inicio < fin`
  - No solapamiento con otros años
  - `ano` debe coincidir con año de `fecha_inicio`
  - No crear años con `fecha_fin` en el pasado

**Response de ejemplo:**
```json
{
  "id": "2",
  "ano": 2026,
  "fechaInicioPlanificacion": "2025-12-01",
  "fechaInicio": "2026-03-01",
  "fechaFin": "2026-12-15",
  "estado": "ACTIVO",
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

#### Grados

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/api/grados` | Listar todos (ordenados por nivel asc) | ADMIN |
| GET | `/api/grados/{id}` | Obtener por ID | ADMIN |

#### Materias

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| GET | `/api/materias` | Listar todas (ordenadas por nombre asc) | ADMIN |
| GET | `/api/materias/{id}` | Obtener por ID | ADMIN |
| POST | `/api/materias` | Crear nueva materia | ADMIN |
| PUT | `/api/materias/{id}` | Actualizar materia | ADMIN |
| DELETE | `/api/materias/{id}` | Eliminar materia | ADMIN |

**POST /api/auth/login**
```bash
# Request
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@edugestio.cl",
  "password": "admin123"
}

# Response 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "tipo": "Bearer",
  "id": "admin-1",
  "email": "admin@edugestio.cl",
  "nombre": "Carlos",
  "apellido": "Mendoza",
  "rol": "ADMIN",
  "profesorId": null,
  "alumnoId": null
}

# Response 401 Unauthorized
{
  "error": "Credenciales inválidas",
  "status": 401
}
```

**GET /api/auth/me**
```bash
# Request
GET http://localhost:8080/api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

# Response 200 OK
{
  "id": "admin-1",
  "email": "admin@edugestio.cl",
  "nombre": "Carlos",
  "apellido": "Mendoza",
  "rol": "ADMIN",
  "profesorId": "",
  "alumnoId": ""
}

# Response 401 Unauthorized
# (Sin token o token inválido)
```

### 7.2 Endpoints Futuros

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| GET | `/api/cursos` | - | ADMIN |
| GET | `/api/cursos/{id}` | ObtenerDetalleCurso | ADMIN |
| POST | `/api/cursos` | - | ADMIN |
| GET | `/api/profesores/{id}/clases-hoy` | ObtenerClasesHoyProfesor | PROFESOR |
| POST | `/api/asistencia/clase` | GuardarAsistenciaClase | PROFESOR |
| GET | `/api/alumnos/{id}/asistencia/mensual` | ObtenerAsistenciaMensualAlumno | APODERADO |
| GET | `/api/dashboard/admin` | ObtenerDashboardAdmin | ADMIN |
| GET | `/api/dashboard/profesor` | ObtenerDashboardProfesor | PROFESOR |
| GET | `/api/dashboard/apoderado` | ObtenerDashboardApoderado | APODERADO |

### 7.3 Códigos de Error

| Status | Descripción | Cuándo ocurre |
|--------|-------------|---------------|
| 200 | OK | Request exitoso |
| 400 | Bad Request | Errores de validación (@Valid) |
| 401 | Unauthorized | Credenciales inválidas o token expirado |
| 403 | Forbidden | Usuario autenticado pero sin permisos |
| 404 | Not Found | Recurso no existe |
| 500 | Internal Server Error | Error inesperado |

---

## 8. CONFIGURACIÓN

### 8.1 Perfiles de Spring

**application.yml** (Base)
```yaml
spring:
  application:
    name: schoolmate-hub-api
  profiles:
    active: dev  # Cambiar a 'prod' en producción

server:
  port: 8080
```

**application-dev.yml** (Desarrollo con H2)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:schoolmate;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: none  # Flyway maneja el schema
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

jwt:
  secret: schoolmate-dev-secret-key-que-debe-tener-al-menos-256-bits-para-hs256-algorithm-ok
  expiration: 86400000  # 24 horas en ms

logging:
  level:
    com.schoolmate: DEBUG
    org.springframework.security: DEBUG
```

**application-prod.yml** (Producción con PostgreSQL)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: ${JWT_SECRET}  # Variable de entorno obligatoria
  expiration: 86400000

logging:
  level:
    com.schoolmate: INFO
```

### 8.2 Variables de Entorno (Producción)

```bash
# Base de datos PostgreSQL
export DB_HOST=db.xxx.supabase.co
export DB_PORT=5432
export DB_NAME=postgres
export DB_USERNAME=postgres
export DB_PASSWORD=tu_password_seguro

# JWT
export JWT_SECRET=tu_secret_jwt_muy_largo_y_seguro_minimo_256_bits

# Opcional
export SERVER_PORT=8080
```

### 8.3 Consola H2 (Desarrollo)

URL: `http://localhost:8080/h2-console`

**Configuración de conexión:**
- JDBC URL: `jdbc:h2:mem:schoolmate`
- User Name: `sa`
- Password: (dejar vacío)

---

## 9. DESARROLLO

### 9.1 Requisitos

- **Java**: 21 LTS
- **Maven**: 3.9+
- **IDE**: IntelliJ IDEA (recomendado) o Eclipse

### 9.2 Scripts Maven

```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Empaquetar (genera JAR)
mvn clean package

# Ejecutar aplicación
mvn spring-boot:run

# Ejecutar con perfil de producción
mvn spring-boot:run -Dspring.profiles.active=prod
```

### 9.3 Ejecutar Localmente

```bash
# 1. Clonar repositorio
git clone https://github.com/fmandres92/schoolmate-backend.git
cd schoolmate-backend

# 2. Compilar
mvn clean compile

# 3. Ejecutar
mvn spring-boot:run

# 4. Verificar
# La aplicación estará en http://localhost:8080

# 5. Probar login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@edugestio.cl","password":"admin123"}'
```

### 9.4 Testing

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar con cobertura
mvn jacoco:report

# Tests específicos
mvn test -Dtest=LoginUsuarioTest
```

### 9.5 Debugging

**Logs de Spring Security:**
```yaml
logging:
  level:
    org.springframework.security: DEBUG
```

**Logs de Hibernate:**
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## 10. PLAN DE MIGRACIÓN POR FASES

### FASE 0 - Fundación ✅ COMPLETADA

**Objetivo**: Spring Boot + BD + JWT funcionando

**Backend:**
- ✅ Proyecto Maven con Spring Boot 4.0.2
- ✅ Java 21 LTS
- ✅ Spring Security + JWT
- ✅ Supabase PostgreSQL (dev y prod)
- ✅ Flyway migraciones
- ✅ Tabla `usuario` con 3 usuarios seed
- ✅ Endpoints: `/api/auth/login`, `/api/auth/me`

**Use Cases:**
- ✅ LoginUsuario

**Usuarios de Prueba Configurados:**
| Email | Password | Rol |
|-------|----------|-----|
| admin@edugestio.cl | admin123 | ADMIN |
| profesor@edugestio.cl | prof123 | PROFESOR |
| apoderado@edugestio.cl | apod123 | APODERADO |

**Criterio de éxito:**
- ✅ Login via API retorna JWT válido
- ✅ Endpoint `/api/auth/me` funciona con token
- ✅ 3 usuarios pueden hacer login
- ✅ Compila y ejecuta sin errores
- ✅ Conectado a Supabase PostgreSQL

---

### FASE 2 - Catálogo Base ✅ COMPLETADA

**Objetivo**: Crear catálogo base del sistema (Años Escolares, Grados, Materias)

**Backend:**
- ✅ Tablas: `ano_escolar`, `grado`, `materia`, `materia_grado`
- ✅ Seed data con IDs compatibles con frontend
- ✅ Entidades JPA con relaciones
- ✅ Repositorios con métodos de consulta
- ✅ Endpoints CRUD protegidos con @PreAuthorize("hasRole('ADMIN')")
- ✅ DTOs con validación Bean Validation

**Nota importante (Refactorización Febrero 2026):**
El sistema de estados de Año Escolar fue refactorizado. Anteriormente existía un campo `activo` booleano y un use case `ActivarAnoEscolar` para activar/desactivar manualmente. Ahora el estado se calcula automáticamente basado en fechas:
- Campo `fecha_inicio_planificacion` agregado (3 meses antes del inicio)
- Estados calculados: FUTURO, PLANIFICACION, ACTIVO, CERRADO
- Endpoint PATCH `/activar` eliminado
- Endpoint GET `/activo` agregado (obtiene año activo por fecha)
- Validaciones de negocio: años CERRADOS son inmutables, no solapamiento de fechas

**Migraciones ejecutadas en Supabase:**
- ✅ V3__create_catalogo_base.sql
- ✅ V4__seed_catalogo_base.sql

**Endpoints implementados:**
- ✅ GET /api/anos-escolares - Listar años (ordenados desc)
- ✅ GET /api/anos-escolares/{id} - Obtener año por ID
- ✅ POST /api/anos-escolares - Crear año
- ✅ PUT /api/anos-escolares/{id} - Actualizar año
- ✅ GET /api/anos-escolares/activo - Obtener año activo actual
- ✅ GET /api/grados - Listar grados (ordenados asc)
- ✅ GET /api/grados/{id} - Obtener grado por ID
- ✅ GET /api/materias - Listar materias (ordenadas asc)
- ✅ GET /api/materias/{id} - Obtener materia por ID
- ✅ POST /api/materias - Crear materia
- ✅ PUT /api/materias/{id} - Actualizar materia
- ✅ DELETE /api/materias/{id} - Eliminar materia

**Datos de prueba cargados:**
- ✅ 3 años escolares (2025, 2026 activo, 2027)
- ✅ 8 grados (1° a 8° Básico)
- ✅ 11 materias con iconos Lucide
- ✅ Relaciones materia-grado (Religión solo en 3°-8°)

**Criterio de éxito:**
- ✅ Todos los endpoints GET funcionan correctamente
- ✅ POST/PUT/PATCH/DELETE operativos
- ✅ Validación de datos con @Valid
- ✅ Protección por rol ADMIN
- ✅ Compila y ejecuta sin errores

---

### FASE 1 - Auth en Frontend ✅ COMPLETADA

**Objetivo**: Login real desde React

**Frontend:**
- ✅ Servicio HTTP con JWT
- ✅ AuthContext consume API en vez de mockUsers
- ✅ Almacenar token en localStorage
- ✅ Incluir token en headers de requests

**Criterio de éxito:**
- ✅ Login contra backend real
- ✅ Sesión persiste con JWT
- ✅ 3 roles redirigen correctamente

---

### FASE 2 - Catálogo Base ✅ COMPLETADA

**Objetivo**: Años, grados, materias desde BD

**Backend:**
- ✅ Tablas: `ano_escolar`, `grado`, `materia`, `materia_grado`
- ✅ Seed data con IDs compatibles con frontend
- ✅ Endpoints CRUD protegidos con ADMIN
- ✅ Estados de Año Escolar calculados automáticamente por fechas

**Entidades creadas:**
- AnoEscolar: id, ano, fechaInicioPlanificacion, fechaInicio, fechaFin, estado (calculado)
- Grado: id, nombre, nivel
- Materia: id, nombre, icono, gradoIds (relación)

**Endpoints implementados:**
- GET/POST/PUT `/api/anos-escolares`
- GET `/api/anos-escolares/activo` (obtiene año activo por fecha actual)
- GET `/api/grados`
- GET/POST/PUT/DELETE `/api/materias`

**Estados de Año Escolar (calculados automáticamente):**
- FUTURO: Hoy < fecha_inicio_planificacion
- PLANIFICACION: fecha_inicio_planificacion <= hoy < fecha_inicio
- ACTIVO: fecha_inicio <= hoy <= fecha_fin
- CERRADO: hoy > fecha_fin

**Reglas de Negocio:**
- Solo un año ACTIVO a la vez (garantizado por fechas sin solapamiento)
- Años CERRADOS son inmutables
- Validaciones: planificacion < inicio < fin, no solapamiento

**Frontend:**
- ⏳ Hooks TanStack Query reemplazan DataContext
- ⏳ Migrar páginas: Años Escolares, Grados, Materias

---

### FASE 3 - Profesores y Cursos ⏳ PENDIENTE

**Backend:**
- Tablas: `profesor`, `curso`
- Relaciones y constraints

**Use Cases:**
- ObtenerDetalleCurso
- ObtenerDetalleProfesor

---

### FASE 4 - Alumnos ⏳ PENDIENTE

**Backend:**
- Tabla: `alumno`
- Vinculación apoderado-alumno

**Use Cases:**
- ObtenerDetalleAlumno

---

### FASE 5 - Asignaciones y Horarios ⏳ PENDIENTE

**Backend:**
- Tabla: `asignacion` (horarios)

**Use Cases:**
- ObtenerClasesHoyProfesor

---

### FASE 6 - Asistencia ⭐ ⏳ PENDIENTE

**Backend:**
- Tablas: `asistencia_clase`, `registro_asistencia`

**Use Cases:**
- GuardarAsistenciaClase
- ObtenerAsistenciaClase
- ObtenerAsistenciaMensualAlumno
- ObtenerResumenAsistenciaAlumno
- ObtenerAsistenciaPromedioCurso

---

### FASE 7 - Reportes ⏳ PENDIENTE

**Backend:**
- Tabla: `reporte`

**Use Cases:**
- CrearReporte
- CambiarEstadoReporte

---

### FASE 8 - Dashboards ⏳ PENDIENTE

**Use Cases:**
- ObtenerDashboardAdmin
- ObtenerDashboardProfesor
- ObtenerDashboardApoderado

---

### FASE 9 - Limpieza ⏳ PENDIENTE

- Eliminar DataContext.tsx del frontend
- Eliminar datos mock
- Agregar indexes en BD
- Logging y monitoreo
- Tests de integración

---

## 11. CONVENCIONES DE CÓDIGO

### 11.1 Nomenclatura

| Tipo | Convención | Ejemplo |
|------|------------|---------|
| Clases | PascalCase | `LoginUsuario`, `AuthController` |
| Métodos | camelCase | `execute()`, `findByEmail()` |
| Variables | camelCase | `usuarioRepository`, `jwtConfig` |
| Constantes | UPPER_SNAKE_CASE | `JWT_EXPIRATION` |
| Paquetes | lowercase | `com.schoolmate.api.usecase.auth` |
| Tablas BD | snake_case | `usuario`, `asistencia_clase` |
| Columnas BD | snake_case | `password_hash`, `created_at` |

### 11.2 Estructura de Clases

```java
// 1. Package
package com.schoolmate.api.usecase.auth;

// 2. Imports
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 3. Anotaciones
@Component
@RequiredArgsConstructor
public class LoginUsuario {

    // 4. Dependencias (inyectadas)
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    // 5. Método execute (único método público)
    public AuthResponse execute(LoginRequest request) {
        // Lógica del use case
    }
    
    // 6. Métodos privados auxiliares
    private void validarUsuario(Usuario usuario) {
        // ...
    }
}
```

### 11.3 Commits Git

```
tipo: descripción corta

cuerpo opcional con más detalles

- feat: nueva característica
- fix: corrección de bug
- docs: documentación
- style: formato, sin cambios de código
- refactor: refactorización
- test: tests
- chore: tareas de mantenimiento

Ejemplos:
feat: agregar endpoint para crear cursos
fix: corregir validación de JWT expirado
docs: actualizar README con instrucciones de instalación
```

---

## 12. REFERENCIAS RÁPIDAS

### 12.1 URLs Importantes

| URL | Descripción |
|-----|-------------|
| `http://localhost:8080` | API Base |
| `http://localhost:8080/api/auth/login` | Login |
| `http://localhost:8080/api/auth/me` | Datos del usuario |
| `http://localhost:8080/api/anos-escolares` | Años escolares |
| `http://localhost:8080/api/grados` | Grados |
| `http://localhost:8080/api/materias` | Materias |
| `http://localhost:8080/h2-console` | Consola H2 (dev) |
| `https://github.com/fmandres92/schoolmate-backend` | Repositorio GitHub |

### 12.2 Comandos Útiles

```bash
# Maven
mvn clean compile          # Compilar
mvn spring-boot:run        # Ejecutar
mvn test                   # Tests
mvn clean package          # Crear JAR

# Git
git add .                  # Agregar cambios
git commit -m "mensaje"    # Commit
git push origin main       # Push a GitHub

# cURL - Probar API
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@edugestio.cl","password":"admin123"}'

# Guardar token en variable
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@edugestio.cl","password":"admin123"}' | jq -r '.token')

# Usar token
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"

# Probar endpoints de catálogo
curl http://localhost:8080/api/anos-escolares -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/api/grados -H "Authorization: Bearer $TOKEN"
curl http://localhost:8080/api/materias -H "Authorization: Bearer $TOKEN"

# Crear nuevo año escolar
curl -X POST http://localhost:8080/api/anos-escolares \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ano":2028,"fechaInicio":"2028-03-01","fechaFin":"2028-12-15"}'

# Activar año escolar
curl -X PATCH http://localhost:8080/api/anos-escolares/1/activar \
  -H "Authorization: Bearer $TOKEN"

# Crear nueva materia
curl -X POST http://localhost:8080/api/materias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Filosofía","icono":"Brain","gradoIds":["7","8"]}'
```

### 12.3 Documentación Relacionada

- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [JJWT](https://github.com/jwtk/jjwt)
- [Flyway](https://documentation.red-gate.com/flyway)

---

**Fin de la Documentación**

*Documento actualizado para SchoolMate Hub API v0.2.0 - Febrero 2026*
