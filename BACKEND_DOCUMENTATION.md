# BACKEND_DOCUMENTATION.md

## SECCIÓN 1: RESUMEN EJECUTIVO

`schoolmate-hub-api` es un backend REST para gestión escolar (catálogos académicos, cursos, profesores, alumnos, matrícula y jornada escolar por curso). La API está construida en Spring Boot con seguridad JWT stateless y acceso a PostgreSQL en Supabase usando JPA + Flyway. La implementación actual está fuertemente orientada a operación administrativa (`ADMIN`), con endpoints CRUD y casos de uso explícitos para reglas de negocio críticas (login, matrícula, jornada). El diseño separa catálogos estables (`materia`, `grado`, `año`) de vínculos temporales (`malla_curricular`, `matricula`) y bloques de jornada (`bloque_horario`).

### Stack tecnológico (versiones exactas verificadas)

Fuente: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/pom.xml` + `mvn dependency:tree`.

| Componente | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.2 |
| Spring Framework | 7.0.3 (transitivo) |
| Spring Security | 7.0.2 (transitivo) |
| Spring Data JPA | 4.0.2 (starter de Boot 4.0.2) |
| Hibernate ORM | 7.2.1.Final |
| PostgreSQL JDBC Driver | 42.7.9 |
| Flyway | 11.14.1 |
| JJWT | 0.12.6 |
| Lombok | 1.18.36 |
| Tomcat embebido | 11.0.15 |
| H2 (tests/runtime) | 2.4.240 |

### Base de datos

- Motor: PostgreSQL (host Supabase).
- URL configurada: `jdbc:postgresql://db.suoiyaaswcibsbrvpjxa.supabase.co:5432/postgres`.
- Dialecto JPA: `org.hibernate.dialect.PostgreSQLDialect`.
- Esquema lógico usado: `public` (no hay `schema` explícito en configuración).
- Versión exacta del servidor PostgreSQL de Supabase: no está declarada en el repositorio ni en migraciones.

### Estado general

| Módulo | Estado |
|---|---|
| Auth JWT | ✅ operativo |
| Años escolares | ✅ operativo |
| Grados | ✅ operativo |
| Materias | ✅ operativo |
| Malla curricular | ✅ operativo |
| Cursos | ✅ operativo |
| Profesores | ✅ operativo |
| Alumnos | ✅ operativo |
| Matrículas | ✅ operativo |
| Jornada escolar por curso | ✅ operativo |
| Ownership por profesor/apoderado | ❌ no implementado |
| Asistencia, reportes, dashboards | ❌ no implementado |

---

## SECCIÓN 2: ARQUITECTURA Y PRINCIPIOS

### Diagrama ASCII de capas

```text
HTTP Request
   |
   v
Controller (@RestController)
   |  \ 
   |   \--> (CRUD directo) Repository (JpaRepository)
   |
   +-----> UseCase (@Component, @Transactional cuando aplica)
              |
              v
         Repository
              |
              v
      PostgreSQL (Supabase)
```

### Regla Use Case vs CRUD directo

Regla observada en código:

- `Controller -> Repository` directo para CRUD y validaciones simples de unicidad/existencia.
- `Controller -> UseCase` cuando hay reglas transaccionales o validaciones cruzadas entre múltiples agregados.

Ejemplos reales:

- CRUD directo:
  - `/api/materias` en `MateriaController`.
  - `/api/profesores` en `ProfesorController`.
  - `/api/cursos` en `CursoController` (aunque con lógica de letra automática, sigue en controller).
- Use case explícito:
  - `LoginUsuario` para autenticación JWT.
  - `MatricularAlumno` y `CambiarEstadoMatricula` para reglas de matrícula.
  - `GuardarJornadaDia`, `CopiarJornadaDia`, `EliminarJornadaDia` y `ObtenerJornadaCurso` para reglas de jornada.

### Principios de diseño observados

- No existe capa `Service` genérica transversal.
- No hay interfaces de caso de uso innecesarias (use cases concretos como clases).
- IDs en entidades: `String` (normalmente UUID generado en `@PrePersist`, aunque también hay IDs semánticos seed como `p1`, `c1`, `mc-...`).
- Borrado lógico en algunos dominios (`activo=false`): malla curricular y bloques de jornada.

### Manejo centralizado de excepciones (`GlobalExceptionHandler`)

Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/exception/GlobalExceptionHandler.java`

Captura y normaliza:

- `BadCredentialsException` -> `AUTH_BAD_CREDENTIALS` (401)
- `ResourceNotFoundException` -> `RESOURCE_NOT_FOUND` (404)
- `BusinessException` -> `BUSINESS_RULE` (400)
- `ApiException` -> `errorCode` específico (status según `ErrorCode`)
- `MethodArgumentNotValidException` -> `VALIDATION_FAILED` (400) con `details`
- `AccessDeniedException` -> `ACCESS_DENIED` (403)
- `DataIntegrityViolationException` -> `DATA_INTEGRITY` (409)
- fallback `Exception` -> `INTERNAL_SERVER_ERROR` (500)

### Estructura de error de la API

Clase: `ApiErrorResponse`

| Campo | Tipo |
|---|---|
| `code` | `String` |
| `message` | `String` |
| `status` | `Integer` |
| `field` | `String` |
| `path` | `String` |
| `timestamp` | `LocalDateTime` |
| `details` | `Map<String,String>` |

---

## SECCIÓN 3: ESTRUCTURA DE CARPETAS

### Árbol de directorios (actual)

```text
/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api
├── pom.xml
├── src/main/java/com/schoolmate/api
│   ├── config                # seguridad HTTP y CORS
│   ├── controller            # endpoints REST
│   ├── dto/request           # contratos de entrada
│   ├── dto/response          # contratos de salida
│   ├── entity                # entidades JPA
│   ├── enums                 # enums de dominio
│   ├── exception             # errores y handler global
│   ├── repository            # repositorios JPA
│   ├── security              # JWT + principal + filter
│   ├── specification         # filtros dinámicos JPA
│   ├── usecase/auth          # caso de uso login
│   ├── usecase/jornada       # casos de uso jornada escolar
│   └── usecase/matricula     # casos de uso matrícula
└── src/main/resources
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    ├── messages_es.properties
    └── db/migration          # migraciones Flyway V1..V12
```

### TODAS las clases por paquete

- `com.schoolmate.api`
  - `SchoolmateApiApplication`
- `com.schoolmate.api.config`
  - `CorsConfig`
  - `SecurityConfig`
- `com.schoolmate.api.controller`
  - `AlumnoController`
  - `AnoEscolarController`
  - `AuthController`
  - `CursoController`
  - `GradoController`
  - `JornadaController`
  - `MallaCurricularController`
  - `MateriaController`
  - `MatriculaController`
  - `ProfesorController`
- `com.schoolmate.api.dto.request`
  - `AlumnoRequest`
  - `AnoEscolarRequest`
  - `BloqueRequest`
  - `CopiarJornadaRequest`
  - `CursoRequest`
  - `JornadaDiaRequest`
  - `LoginRequest`
  - `MallaCurricularBulkRequest`
  - `MallaCurricularRequest`
  - `MateriaRequest`
  - `MatriculaRequest`
  - `ProfesorRequest`
- `com.schoolmate.api.dto.response`
  - `AlumnoPageResponse`
  - `AlumnoResponse`
  - `AnoEscolarResponse`
  - `ApiErrorResponse`
  - `BloqueHorarioResponse`
  - `AuthResponse`
  - `CursoResponse`
  - `JornadaCursoResponse`
  - `JornadaDiaResponse`
  - `JornadaResumenResponse`
  - `MallaCurricularResponse`
  - `MateriaPageResponse`
  - `MateriaResponse`
  - `MatriculaResponse`
  - `ProfesorResponse`
- `com.schoolmate.api.entity`
  - `Alumno`
  - `AnoEscolar`
  - `BloqueHorario`
  - `Curso`
  - `Grado`
  - `MallaCurricular`
  - `Materia`
  - `Matricula`
  - `Profesor`
  - `SeccionCatalogo`
  - `Usuario`
- `com.schoolmate.api.enums`
  - `EstadoAnoEscolar`
  - `EstadoMatricula`
  - `Rol`
  - `TipoBloque`
- `com.schoolmate.api.exception`
  - `ApiException`
  - `BusinessException`
  - `ErrorCode`
  - `GlobalExceptionHandler`
  - `ResourceNotFoundException`
  - `UnauthorizedException`
- `com.schoolmate.api.repository`
  - `AlumnoRepository`
  - `AnoEscolarRepository`
  - `BloqueHorarioRepository`
  - `CursoRepository`
  - `GradoRepository`
  - `MallaCurricularRepository`
  - `MateriaRepository`
  - `MatriculaRepository`
  - `ProfesorRepository`
  - `SeccionCatalogoRepository`
  - `UsuarioRepository`
- `com.schoolmate.api.security`
  - `CustomUserDetailsService`
  - `JwtAuthenticationFilter`
  - `JwtConfig`
  - `JwtTokenProvider`
  - `UserPrincipal`
- `com.schoolmate.api.specification`
  - `AlumnoSpecifications`
- `com.schoolmate.api.usecase.auth`
  - `LoginUsuario`
- `com.schoolmate.api.usecase.jornada`
  - `CopiarJornadaDia`
  - `EliminarJornadaDia`
  - `GuardarJornadaDia`
  - `ObtenerJornadaCurso`
- `com.schoolmate.api.usecase.matricula`
  - `CambiarEstadoMatricula`
  - `MatricularAlumno`

---

## SECCIÓN 4: MODELO DE DATOS (ENTIDADES JPA)

> Nota: donde hay divergencia entre entidad y migraciones SQL, se marca explícitamente.

### `Usuario` (`usuario`)

| Campo Java | Tipo Java | Columna BD | Tipo BD (migración) | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `email` | `String` | `email` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `passwordHash` | `String` | `password_hash` | `VARCHAR(255)` | NOT NULL |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` | NOT NULL |
| `apellido` | `String` | `apellido` | `VARCHAR(100)` | NOT NULL |
| `rol` | `Rol` | `rol` | `VARCHAR(20)` | NOT NULL |
| `profesorId` | `String` | `profesor_id` | `VARCHAR(36)` | nullable |
| `alumnoId` | `String` | `alumno_id` | `VARCHAR(36)` | nullable |
| `activo` | `Boolean` | `activo` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones JPA: no hay relaciones `@ManyToOne`; `profesorId/alumnoId` son campos planos.

Índices: `idx_usuario_email`, `idx_usuario_rol`.

### `AnoEscolar` (`ano_escolar`)

| Campo Java | Tipo Java | Columna BD | Tipo BD esperado |
|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` |
| `ano` | `Integer` | `ano` | `INTEGER` |
| `fechaInicioPlanificacion` | `LocalDate` | `fecha_inicio_planificacion` | `DATE` |
| `fechaInicio` | `LocalDate` | `fecha_inicio` | `DATE` |
| `fechaFin` | `LocalDate` | `fecha_fin` | `DATE` |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` |

Constraints en código: `ano` marcado como `unique=true`.

Divergencia: `V3` crea `ano_escolar` con columna `activo` y **sin** `fecha_inicio_planificacion`; el código actual exige `fecha_inicio_planificacion` y no mapea `activo`.

### `Grado` (`grado`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `nombre` | `String` | `nombre` | `VARCHAR(50)` | NOT NULL |
| `nivel` | `Integer` | `nivel` | `INTEGER` | NOT NULL |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Índice: `idx_grado_nivel`.

### `Materia` (`materia`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` | NOT NULL |
| `icono` | `String` | `icono` | `VARCHAR(50)` | nullable |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

### `Profesor` (`profesor`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `rut` | `String` | `rut` | `VARCHAR(20)` | NOT NULL, UNIQUE |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` | NOT NULL |
| `apellido` | `String` | `apellido` | `VARCHAR(100)` | NOT NULL |
| `email` | `String` | `email` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `telefono` | `String` | `telefono` | `VARCHAR(30)` | nullable |
| `fechaContratacion` | `LocalDate` | `fecha_contratacion` | `DATE` | NOT NULL |
| `activo` | `Boolean` | `activo` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToMany` con `Materia` vía `profesor_materia(profesor_id, materia_id)`.

Índices: `idx_profesor_email`, `idx_profesor_activo`.

### `Curso` (`curso`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `nombre` | `String` | `nombre` | `VARCHAR(50)` | NOT NULL |
| `letra` | `String` | `letra` | `VARCHAR(5)` (V5), validado a 1 char por V10 | NOT NULL |
| `grado` | `Grado` | `grado_id` | `VARCHAR(36)` | FK NOT NULL |
| `anoEscolar` | `AnoEscolar` | `ano_escolar_id` | `VARCHAR(36)` | FK NOT NULL |
| `activo` | `Boolean` | `activo` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToOne` -> `Grado`
- `@ManyToOne` -> `AnoEscolar`

Índices: `idx_curso_grado`, `idx_curso_ano_escolar`, `idx_curso_activo`, `uq_curso_grado_ano_letra`.

### `SeccionCatalogo` (`seccion_catalogo`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `letra` | `String` | `letra` | `VARCHAR(1)` | PK |
| `orden` | `Short` | `orden` | `SMALLINT` | NOT NULL, UNIQUE |
| `activo` | `Boolean` | `activo` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL DEFAULT NOW |

### `Alumno` (`alumno`)

| Campo Java | Tipo Java | Columna BD esperada | Tipo BD esperado |
|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` |
| `rut` | `String` | `rut` | `VARCHAR(20)` UNIQUE |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` |
| `apellido` | `String` | `apellido` | `VARCHAR(100)` |
| `fechaNacimiento` | `LocalDate` | `fecha_nacimiento` | `DATE` |
| `apoderadoNombre` | `String` | `apoderado_nombre` | `VARCHAR(100)` |
| `apoderadoApellido` | `String` | `apoderado_apellido` | `VARCHAR(100)` |
| `apoderadoEmail` | `String` | `apoderado_email` | `VARCHAR(255)` |
| `apoderadoTelefono` | `String` | `apoderado_telefono` | `VARCHAR(30)` |
| `apoderadoVinculo` | `String` | `apoderado_vinculo` | `VARCHAR(20)` |
| `activo` | `Boolean` | `activo` | `BOOLEAN` |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` |

Divergencia/migraciones: `V7__create_alumnos.sql` y `V8__seed_alumnos.sql` están vacíos (tracking), por lo que el DDL exacto de creación inicial no está versionado en Flyway del repo.

### `Matricula` (`matricula`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `alumno` | `Alumno` | `alumno_id` | `VARCHAR(36)` | FK NOT NULL |
| `curso` | `Curso` | `curso_id` | `VARCHAR(36)` | FK NOT NULL |
| `anoEscolar` | `AnoEscolar` | `ano_escolar_id` | `VARCHAR(36)` | FK NOT NULL |
| `fechaMatricula` | `LocalDate` | `fecha_matricula` | `DATE` | NOT NULL |
| `estado` | `EstadoMatricula` | `estado` | `VARCHAR(20)` | NOT NULL DEFAULT ACTIVA |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToOne` -> `Alumno`
- `@ManyToOne` -> `Curso`
- `@ManyToOne` -> `AnoEscolar`

Índices: `idx_matricula_alumno`, `idx_matricula_curso`, `idx_matricula_ano_escolar`, `idx_matricula_estado`, `uq_matricula_alumno_ano_activa` (parcial por estado ACTIVA).

### `MallaCurricular` (`malla_curricular`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `materia` | `Materia` | `materia_id` | `VARCHAR(36)` | FK NOT NULL |
| `grado` | `Grado` | `grado_id` | `VARCHAR(36)` | FK NOT NULL |
| `anoEscolar` | `AnoEscolar` | `ano_escolar_id` | `VARCHAR(36)` | FK NOT NULL |
| `horasPedagogicas` | `Integer` | `horas_pedagogicas` | `INTEGER` | NOT NULL DEFAULT 2 |
| `activo` | `Boolean` | `activo` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToOne` -> `Materia`
- `@ManyToOne` -> `Grado`
- `@ManyToOne` -> `AnoEscolar`

Constraint: `uq_malla_materia_grado_ano`.

### `BloqueHorario` (`bloque_horario`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `curso` | `Curso` | `curso_id` | `VARCHAR(36)` | FK NOT NULL |
| `diaSemana` | `Integer` | `dia_semana` | `INTEGER` | CHECK 1..5 |
| `numeroBloque` | `Integer` | `numero_bloque` | `INTEGER` | NOT NULL |
| `horaInicio` | `LocalTime` | `hora_inicio` | `TIME` | NOT NULL |
| `horaFin` | `LocalTime` | `hora_fin` | `TIME` | NOT NULL, > inicio |
| `tipo` | `TipoBloque` | `tipo` | `VARCHAR(20)` | NOT NULL (`CLASE`,`RECREO`,`ALMUERZO`) |
| `profesor` | `Profesor` | `profesor_id` | `VARCHAR(36)` | FK nullable |
| `materia` | `Materia` | `materia_id` | `VARCHAR(36)` | FK nullable |
| `activo` | `Boolean` | `activo` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToOne` -> `Curso`
- `@ManyToOne` -> `Profesor` (nullable)
- `@ManyToOne` -> `Materia` (nullable)

Constraints DB adicionales:

- jornada modelada por día (`dia_semana`) y secuencia (`numero_bloque`).
- `materia_id` y `profesor_id` se mantienen null al configurar estructura base.

### Campos de auditoría

Presente en todas las entidades excepto sin `updatedAt` en `SeccionCatalogo`.

- `created_at` + `updated_at`: `Usuario`, `AnoEscolar`, `Grado`, `Materia`, `Profesor`, `Curso`, `Alumno`, `Matricula`, `MallaCurricular`, `BloqueHorario`.
- Solo `created_at`: `SeccionCatalogo`.

### Enums usados y valores

- `Rol`: `ADMIN`, `PROFESOR`, `APODERADO`
- `EstadoAnoEscolar`: `FUTURO`, `PLANIFICACION`, `ACTIVO`, `CERRADO`
- `EstadoMatricula`: `ACTIVA`, `RETIRADO`, `TRASLADADO`
- `TipoBloque`: `CLASE`, `RECREO`, `ALMUERZO`

### Diagrama ER ASCII (modelo lógico actual)

```text
usuario
  (profesor_id, alumno_id como string sin FK JPA)

profesor --< profesor_materia >-- materia

grado 1---* curso *---1 ano_escolar
                |
                | 1
                *
            matricula *---1 alumno
                |
                *---1 ano_escolar

materia 1---* malla_curricular *---1 grado
                      |
                      *---1 ano_escolar

curso 1---* bloque_horario *---0..1 profesor
                        |
                        0..1
                      materia

seccion_catalogo 1---* curso (por letra)
```

---

## SECCIÓN 5: MIGRACIONES FLYWAY

### Lista completa en orden

1. `V1__create_usuario_table.sql`
2. `V2__seed_usuarios.sql`
3. `V3__create_catalogo_base.sql`
4. `V4__seed_catalogo_base.sql`
5. `V5__create_profesores_cursos.sql`
6. `V6__seed_profesores_cursos.sql`
7. `V7__create_alumnos.sql`
8. `V8__seed_alumnos.sql`
9. `V9__create_malla_curricular.sql`
10. `V10__create_seccion_catalogo.sql`
11. `V11__create_matricula_refactor_alumno.sql`
12. `V12__create_asignacion.sql`
13. `V13__rename_horas_semanales_to_horas_pedagogicas.sql`

### Qué hace cada migración

| Migración | Cambios |
|---|---|
| `V1` | Crea `usuario`, índices por email y rol |
| `V2` | Seed usuarios (`ADMIN`, `PROFESOR`, `APODERADO`) |
| `V3` | Crea `ano_escolar`, `grado`, `materia`, `materia_grado` |
| `V4` | Seed de años, grados, materias y mapeo materia-grado |
| `V5` | Crea `profesor`, `profesor_materia`, `curso` + índices |
| `V6` | Seed de profesores, relación profesor-materia y cursos |
| `V7` | Placeholder: sin DDL (tabla alumno creada fuera de Flyway) |
| `V8` | Placeholder: sin DML (seed alumnos ejecutado fuera de Flyway) |
| `V9` | Elimina `materia_grado`, crea `malla_curricular` + índices + seed malla |
| `V10` | Crea `seccion_catalogo`, seed A..F, unique de curso por grado/año/letra, FK `curso.letra`, check formato letra |
| `V11` | Crea `matricula`, migra datos desde `alumno.curso_id`, elimina `curso_id` y `fecha_inscripcion` en `alumno` |
| `V12` | (Histórico) creaba `asignacion`; hoy esa tabla fue eliminada del entorno Supabase actual |
| `V13` | Renombra columna `malla_curricular.horas_semanales` a `horas_pedagogicas` (sin alterar valores) |

### Estado resultante del esquema (según migraciones + código)

- Modelo oficial en código y queries usa: `usuario`, `ano_escolar`, `grado`, `materia`, `profesor`, `profesor_materia`, `curso`, `seccion_catalogo`, `malla_curricular`, `alumno`, `matricula`, `bloque_horario`.
- Riesgo de drift:
  - `ano_escolar.fecha_inicio_planificacion` requerido por código pero no aparece en `V3`.
  - `alumno` no está versionado explícitamente en repo (V7/V8 placeholders).
  - `bloque_horario` existe en Supabase pero no está creada por migración Flyway versionada en el repo.

### Convenciones para nuevas migraciones

Convenciones observadas:

- Prefijo secuencial `V{n}__descripcion.sql`.
- Uso frecuente de `IF EXISTS / IF NOT EXISTS` para idempotencia parcial.
- Seeds en migración versionada cuando aplica.
- Restricciones en DB para reglas críticas (unicidad parcial/checks).

Recomendación alineada al proyecto:

1. No editar migraciones históricas.
2. Crear nueva `V13+` para cualquier ajuste de esquema.
3. Incluir `ALTER` explícito para cerrar drift (`ano_escolar`, `alumno`) si existe.

---

## SECCIÓN 6: SEGURIDAD Y AUTENTICACIÓN

### Flujo JWT completo

1. `POST /api/auth/login` recibe `LoginRequest(email,password)`.
2. `LoginUsuario`:
   - busca usuario por email (`UsuarioRepository`).
   - valida `activo=true`.
   - valida password con `PasswordEncoder` BCrypt.
   - construye `UserPrincipal` y genera JWT (`JwtTokenProvider`).
3. Respuesta `AuthResponse` con `token`, `tipo=Bearer` y datos de usuario.
4. En cada request:
   - `JwtAuthenticationFilter` toma header `Authorization: Bearer <token>`.
   - valida token (`validateToken`).
   - extrae `email` del claim `subject`.
   - carga usuario por email (`CustomUserDetailsService`).
   - setea `SecurityContext` con authorities `ROLE_*`.

### `SecurityConfig`

Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/config/SecurityConfig.java`

- CSRF deshabilitado.
- CORS habilitado por bean.
- Session policy: `STATELESS`.
- `permitAll`:
  - `/api/auth/**`
  - `/h2-console/**`
  - `/error`
- Resto: autenticado.

### `JwtAuthenticationFilter`

- Extiende `OncePerRequestFilter`.
- Si token inválido o excepción, solo logea error y continúa cadena.
- No corta explícitamente con 401 en filter; la autorización posterior decide.

### `UserPrincipal`

Campos:

- `id`, `email`, `password`, `rol`, `profesorId`, `alumnoId`, `nombre`, `apellido`.

Authorities:

- una autoridad: `ROLE_<ROL>`.

Datos en token (`JwtTokenProvider`):

- `sub=email`, claims: `id`, `rol`, `profesorId`, `alumnoId`, `nombre`, `apellido`, `iat`, `exp`.

### Manejo de roles (`ADMIN`, `PROFESOR`, `APODERADO`)

Estado actual de autorización por código:

- Casi todos los controllers de dominio: `@PreAuthorize("hasRole('ADMIN')")` a nivel clase o método.
- `AnoEscolarController.obtenerActivo`: `isAuthenticated()`.
- No hay endpoints funcionales específicos de `PROFESOR` o `APODERADO`.

### Uso de `@PreAuthorize`

Sí existe en controllers de:

- Alumnos, Años, Cursos, Grados, Jornada, Malla, Materias, Matrículas, Profesores.

### Regla de ownership (profesor solo sus datos)

No está implementada en el estado actual.

- No hay validación por `principal.profesorId` en controllers/use cases.
- No hay endpoints con `hasRole('PROFESOR')` que apliquen ownership.

---

## SECCIÓN 7: ENDPOINTS API (CATÁLOGO COMPLETO)

> Todos los endpoints listados provienen de controladores actuales.

### Dominio: Auth

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/auth/login` | Login y emisión JWT | Público | Body JSON | `LoginRequest {email,password}` | `AuthResponse {token,tipo,id,email,nombre,apellido,rol,profesorId,alumnoId}` | `LoginUsuario` | `AUTH_BAD_CREDENTIALS`, `VALIDATION_FAILED` |
| `GET /api/auth/me` | Datos del usuario actual | Público por `permitAll` (intención: autenticado) | Header `Authorization` esperado | - | `Map{id,email,nombre,apellido,rol,profesorId,alumnoId}` | directo | sin token puede terminar en `INTERNAL_SERVER_ERROR` (NPE) |

### Dominio: Años Escolares

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/anos-escolares` | Lista años con estado calculado | `ADMIN` | - | - | `List<AnoEscolarResponse>` | directo repository | `ACCESS_DENIED` |
| `GET /api/anos-escolares/{id}` | Obtiene año por id | `ADMIN` | Path `id` | - | `AnoEscolarResponse` | directo | `RESOURCE_NOT_FOUND` |
| `GET /api/anos-escolares/activo` | Año activo para fecha actual | autenticado | - | - | `AnoEscolarResponse` | directo | `RESOURCE_NOT_FOUND` |
| `POST /api/anos-escolares` | Crea año | `ADMIN` | Body | `AnoEscolarRequest {ano,fechaInicioPlanificacion,fechaInicio,fechaFin}` | `AnoEscolarResponse` | directo con reglas | `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `PUT /api/anos-escolares/{id}` | Actualiza año | `ADMIN` | Path `id` + Body | `AnoEscolarRequest` | `AnoEscolarResponse` | directo con reglas | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |

### Dominio: Grados

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/grados` | Lista grados por nivel | `ADMIN` | - | - | `List<Grado>` (entidad directa) | directo | `ACCESS_DENIED` |
| `GET /api/grados/{id}` | Obtiene grado por id | `ADMIN` | Path `id` | - | `Grado` | directo | `RESOURCE_NOT_FOUND` |

### Dominio: Materias

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/materias` | Lista paginada y ordenable | `ADMIN` | Query: `page,size,sortBy,sortDir` | - | `MateriaPageResponse` | directo | `ACCESS_DENIED` |
| `GET /api/materias/{id}` | Obtiene materia | `ADMIN` | Path `id` | - | `MateriaResponse` | directo | `RESOURCE_NOT_FOUND` |
| `POST /api/materias` | Crea materia | `ADMIN` | Body | `MateriaRequest {nombre,icono}` | `MateriaResponse` | directo | `VALIDATION_FAILED` |
| `PUT /api/materias/{id}` | Actualiza materia | `ADMIN` | Path + Body | `MateriaRequest` | `MateriaResponse` | directo | `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |
| `DELETE /api/materias/{id}` | Elimina materia (físico) | `ADMIN` | Path | - | `204` | directo | `RESOURCE_NOT_FOUND`, `DATA_INTEGRITY` |

### Dominio: Malla Curricular

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/malla-curricular` | Lista malla activa por año | `ADMIN` | Query: `anoEscolarId` | - | `List<MallaCurricularResponse>` | directo | `ACCESS_DENIED` |
| `GET /api/malla-curricular/materia/{materiaId}` | Lista malla por materia y año | `ADMIN` | Path + Query `anoEscolarId` | - | `List<MallaCurricularResponse>` | directo | - |
| `GET /api/malla-curricular/grado/{gradoId}` | Lista malla por grado y año | `ADMIN` | Path + Query `anoEscolarId` | - | `List<MallaCurricularResponse>` | directo | - |
| `POST /api/malla-curricular` | Crea registro malla | `ADMIN` | Body | `MallaCurricularRequest {materiaId,gradoId,anoEscolarId,horasPedagogicas}` | `MallaCurricularResponse` | directo transaccional | `409` conflict (duplicate), `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |
| `PUT /api/malla-curricular/{id}` | Actualiza horas/activo | `ADMIN` | Path + Body | `MallaCurricularUpdateRequest {horasPedagogicas,activo}` (inner class) | `MallaCurricularResponse` | directo transaccional | `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |
| `POST /api/malla-curricular/bulk` | Upsert masivo por materia-año | `ADMIN` | Body | `MallaCurricularBulkRequest` | `List<MallaCurricularResponse>` | directo transaccional | `BAD_REQUEST` grados duplicados, `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |
| `DELETE /api/malla-curricular/{id}` | Baja lógica (`activo=false`) | `ADMIN` | Path | - | `204` | directo transaccional | `RESOURCE_NOT_FOUND` |

### Dominio: Cursos

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/cursos` | Lista cursos (filtro opcional por año/grado) + matriculados | `ADMIN` | Query opcional: `anoEscolarId`, `gradoId` | - | `List<CursoResponse>` | directo | - |
| `GET /api/cursos/{id}` | Detalle enriquecido (malla + conteos) | `ADMIN` | Path `id` | - | `CursoResponse` con `materias`, `cantidadMaterias`, `totalHorasPedagogicas`, `alumnosMatriculados` | directo | `RESOURCE_NOT_FOUND` |
| `POST /api/cursos` | Crea curso con letra automática | `ADMIN` | Body | `CursoRequest {gradoId,anoEscolarId}` | `CursoResponse` | directo transaccional | `RESOURCE_NOT_FOUND`, `CURSO_SIN_SECCION_DISPONIBLE` |
| `PUT /api/cursos/{id}` | Reasigna curso (recalcula letra si cambia grado/año) | `ADMIN` | Path + Body | `CursoRequest` | `CursoResponse` | directo transaccional | `RESOURCE_NOT_FOUND`, `CURSO_SIN_SECCION_DISPONIBLE` |

### Dominio: Profesores

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/profesores` | Lista profesores ordenados por apellido | `ADMIN` | - | - | `List<ProfesorResponse>` | directo | - |
| `GET /api/profesores/{id}` | Obtiene profesor | `ADMIN` | Path | - | `ProfesorResponse` | directo | `RESOURCE_NOT_FOUND` |
| `POST /api/profesores` | Crea profesor con materias | `ADMIN` | Body | `ProfesorRequest` | `ProfesorResponse` | directo | `PROFESOR_RUT_DUPLICADO`, `PROFESOR_EMAIL_DUPLICADO`, `PROFESOR_TELEFONO_DUPLICADO`, `MATERIAS_NOT_FOUND`, `VALIDATION_FAILED` |
| `PUT /api/profesores/{id}` | Actualiza profesor; RUT inmutable | `ADMIN` | Path + Body | `ProfesorRequest` | `ProfesorResponse` | directo | `RESOURCE_NOT_FOUND`, `PROFESOR_RUT_INMUTABLE`, duplicados, `MATERIAS_NOT_FOUND`, `VALIDATION_FAILED` |

### Dominio: Alumnos

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/alumnos` | Lista paginada con búsqueda y enriquecimiento opcional por matrícula | `ADMIN` | Query: `page,size,sortBy,sortDir,anoEscolarId,cursoId,gradoId,q` | - | `AlumnoPageResponse` | directo + `AlumnoSpecifications` | - |
| `GET /api/alumnos/{id}` | Obtiene alumno; opcional matrícula activa por año | `ADMIN` | Path + Query opcional `anoEscolarId` | - | `AlumnoResponse` | directo | `RESOURCE_NOT_FOUND` |
| `POST /api/alumnos` | Crea alumno | `ADMIN` | Body | `AlumnoRequest` | `AlumnoResponse` | directo | `409` (RUT duplicado vía `ResponseStatusException`), `VALIDATION_FAILED` |
| `PUT /api/alumnos/{id}` | Actualiza alumno | `ADMIN` | Path + Body | `AlumnoRequest` | `AlumnoResponse` | directo | `RESOURCE_NOT_FOUND`, `409` (RUT duplicado), `VALIDATION_FAILED` |

### Dominio: Matrículas

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/matriculas` | Matricula alumno en curso/año | `ADMIN` | Body | `MatriculaRequest {alumnoId,cursoId,anoEscolarId,fechaMatricula?}` | `MatriculaResponse` | `MatricularAlumno` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/matriculas/curso/{cursoId}` | Matrículas activas por curso | `ADMIN` | Path `cursoId` | - | `List<MatriculaResponse>` | directo | - |
| `GET /api/matriculas/alumno/{alumnoId}` | Historial de matrículas por alumno | `ADMIN` | Path `alumnoId` | - | `List<MatriculaResponse>` | directo | - |
| `PATCH /api/matriculas/{id}/estado` | Cambia estado (`ACTIVA/RETIRADO/TRASLADADO`) | `ADMIN` | Path + body map `{estado}` | `Map<String,String>` | `MatriculaResponse` | `CambiarEstadoMatricula` | `400` por body inválido, `RESOURCE_NOT_FOUND`, `BUSINESS_RULE` |

### Dominio: Jornada

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `PUT /api/cursos/{cursoId}/jornada/{diaSemana}` | Guarda/reemplaza la jornada de un día | `ADMIN` | Path `cursoId`,`diaSemana`; Body | `JornadaDiaRequest` | `JornadaDiaResponse` | `GuardarJornadaDia` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/cursos/{cursoId}/jornada` | Obtiene jornada completa; opcionalmente filtra por día | `ADMIN` | Path `cursoId`; Query opcional `diaSemana` | - | `JornadaCursoResponse` | `ObtenerJornadaCurso` | `RESOURCE_NOT_FOUND` |
| `GET /api/cursos/{cursoId}/jornada/resumen` | Obtiene solo resumen semanal | `ADMIN` | Path `cursoId` | - | `JornadaResumenResponse` | `ObtenerJornadaCurso` (wrapper) | `RESOURCE_NOT_FOUND` |
| `POST /api/cursos/{cursoId}/jornada/{diaSemanaOrigen}/copiar` | Copia estructura de un día a días destino | `ADMIN` | Path `cursoId`,`diaSemanaOrigen`; Body | `CopiarJornadaRequest` | `JornadaCursoResponse` | `CopiarJornadaDia` | `BUSINESS_RULE`, `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |
| `DELETE /api/cursos/{cursoId}/jornada/{diaSemana}` | Elimina lógicamente jornada de un día | `ADMIN` | Path `cursoId`,`diaSemana` | - | `204` | `EliminarJornadaDia` | `BUSINESS_RULE`, `RESOURCE_NOT_FOUND` |

---

## SECCIÓN 8: USE CASES

### `com.schoolmate.api.usecase.auth.LoginUsuario`

- Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/usecase/auth/LoginUsuario.java`
- Función: autenticar usuario y emitir JWT.
- Repositorios/dependencias:
  - `UsuarioRepository`
  - `PasswordEncoder`
  - `JwtTokenProvider`
- Validaciones:
  - email existe
  - usuario activo
  - password BCrypt coincide
- Flujo `execute()`:
  1. `findByEmail`
  2. valida `activo`
  3. valida password
  4. construye `UserPrincipal`
  5. genera token
  6. retorna `AuthResponse`
- Errores:
  - `BadCredentialsException` (credenciales inválidas / usuario desactivado)
- `@Transactional`: no.

### `com.schoolmate.api.usecase.matricula.MatricularAlumno`

- Función: crear matrícula activa de alumno.
- Repositorios:
  - `AlumnoRepository`, `CursoRepository`, `AnoEscolarRepository`, `MatriculaRepository`
- Validaciones:
  - alumno/curso/año existen
  - curso pertenece al año indicado
  - alumno no tiene matrícula activa en ese año
- Flujo:
  1. carga entidades
  2. valida pertenencia curso-año
  3. valida unicidad de matrícula activa
  4. define fecha (`request.fechaMatricula` o `LocalDate.now()`)
  5. guarda `Matricula(estado=ACTIVA)`
- Errores:
  - `ResourceNotFoundException`
  - `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.matricula.CambiarEstadoMatricula`

- Función: transición de estado de matrícula.
- Repositorio: `MatriculaRepository`.
- Validaciones:
  - matrícula existe
  - transición válida:
    - `ACTIVA -> RETIRADO|TRASLADADO`
    - `RETIRADO|TRASLADADO -> ACTIVA`
  - no repetir mismo estado
- Flujo:
  1. carga matrícula
  2. valida transición
  3. setea nuevo estado
  4. guarda
- Errores:
  - `ResourceNotFoundException`
  - `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.jornada.GuardarJornadaDia`

- Función: guardar/reemplazar la estructura diaria de jornada de un curso.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`
- Validaciones:
  - curso existe y su año no está `CERRADO`
  - `diaSemana` entre 1 y 5
  - bloques secuenciales desde `numeroBloque=1`
  - horas válidas y continuas (sin gaps)
  - `horaFin > horaInicio`
  - rango permitido `07:00` a `18:00`
  - máximo 1 `ALMUERZO`
  - al menos un bloque `CLASE`
  - primer y último bloque deben ser `CLASE`
- Flujo:
  1. valida curso y estado de año
  2. valida día y bloques
  3. desactiva bloques activos previos del día
  4. crea nuevos bloques
  5. arma `JornadaDiaResponse`
- Errores:
  - `BusinessException`
  - `ResourceNotFoundException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.jornada.ObtenerJornadaCurso`

- Función: obtener jornada activa de un curso (completa o filtrada por día) y su resumen.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`
- Flujo:
  1. valida existencia de curso
  2. carga bloques activos (todos o por día)
  3. agrupa por `diaSemana`
  4. construye `JornadaCursoResponse` + `JornadaResumenResponse`
- Errores:
  - `ResourceNotFoundException`
- `@Transactional`: no.

### `com.schoolmate.api.usecase.jornada.CopiarJornadaDia`

- Función: copiar estructura (horas + tipo) de un día origen a varios días destino.
- Repositorios/dependencias:
  - `BloqueHorarioRepository`, `GuardarJornadaDia`, `ObtenerJornadaCurso`
- Reglas:
  - día origen debe tener bloques configurados
  - destino entre 1 y 5
  - origen no puede estar en destino
  - no copia `materia` ni `profesor`
- Flujo:
  1. carga bloques origen
  2. arma `JornadaDiaRequest` con estructura origen
  3. aplica `GuardarJornadaDia` por cada destino
  4. retorna jornada completa actualizada
- Errores:
  - `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.jornada.EliminarJornadaDia`

- Función: baja lógica de jornada diaria (`activo=false` por curso+día).
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`
- Flujo:
  1. valida curso y año no cerrado
  2. valida día (1..5)
  3. desactiva bloques activos del día
  4. si no hubo cambios, retorna error de negocio
- Errores:
  - `BusinessException`
  - `ResourceNotFoundException`
- `@Transactional`: sí.

---

## SECCIÓN 9: REPOSITORIOS

| Repositorio | Entidad | Métodos derivados destacados | `@Query` custom | Specifications |
|---|---|---|---|---|
| `AlumnoRepository` | `Alumno` | `existsByRut`, `existsByRutAndIdNot` | no | sí, vía `JpaSpecificationExecutor` |
| `AnoEscolarRepository` | `AnoEscolar` | `findAllByOrderByAnoDesc`, `findByAno`, `existsByAno`, `findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual` | no | no |
| `BloqueHorarioRepository` | `BloqueHorario` | `findByCursoIdAndActivoTrueOrderByDiaSemanaAscNumeroBloqueAsc`, `findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc` | `desactivarBloquesDia`, `findDiasConfigurados` | no |
| `CursoRepository` | `Curso` | `findByAnoEscolarIdOrderByNombreAsc`, `findByAnoEscolarIdAndGradoIdOrderByLetraAsc`, `findByActivoTrueAndAnoEscolarIdOrderByNombreAsc` | `findLetrasUsadasByGradoIdAndAnoEscolarId` | no |
| `GradoRepository` | `Grado` | `findAllByOrderByNivelAsc` | no | no |
| `MallaCurricularRepository` | `MallaCurricular` | múltiples `findBy...` y `existsBy...` combinando materia/grado/año/activo | no | no |
| `MateriaRepository` | `Materia` | `findAllByOrderByNombreAsc`, `existsByNombre` | no | no |
| `MatriculaRepository` | `Matricula` | `findByAlumnoId`, `findByCursoIdAndEstado`, `findByAlumnoIdAndAnoEscolarIdAndEstado`, `existsByAlumnoIdAndAnoEscolarIdAndEstado`, etc. | `countActivasByCursoIds` | no |
| `ProfesorRepository` | `Profesor` | unicidad por rut/email/teléfono + listas ordenadas | no | no |
| `SeccionCatalogoRepository` | `SeccionCatalogo` | `findByActivoTrueOrderByOrdenAsc` | no | no |
| `UsuarioRepository` | `Usuario` | `findByEmail`, `existsByEmail` | no | no |

### Specifications existentes

Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/specification/AlumnoSpecifications.java`

- `activoTrue()`
- `searchByNombre(q)`
- `searchByRutDigits(digits)`
- `byIdIn(ids)`

---

## SECCIÓN 10: DTOs (REQUEST Y RESPONSE)

### Request DTOs

| DTO | Campos | Validaciones | Lombok |
|---|---|---|---|
| `LoginRequest` | `email`, `password` | `@NotBlank`, `@Email` | `@Data` |
| `AnoEscolarRequest` | `ano`, `fechaInicioPlanificacion`, `fechaInicio`, `fechaFin` | `@NotNull` | `@Data` |
| `MateriaRequest` | `nombre`, `icono` | `@NotBlank(nombre)` | `@Data` |
| `ProfesorRequest` | `rut,nombre,apellido,email,telefono,fechaContratacion,materiaIds` | `@NotBlank`, `@Size`, `@Email`, `@NotNull`, `@NotEmpty` | `@Data` |
| `CursoRequest` | `gradoId`, `anoEscolarId` | `@NotBlank` | `@Data` |
| `AlumnoRequest` | `rut,nombre,apellido,fechaNacimiento,apoderadoNombre,apoderadoApellido,apoderadoEmail,apoderadoTelefono,apoderadoVinculo` | `@NotBlank`, `@Size`, `@Email` | `@Data @Builder` |
| `MatriculaRequest` | `alumnoId,cursoId,anoEscolarId,fechaMatricula?` | `@NotBlank` (except fecha) | `@Data @Builder` |
| `MallaCurricularRequest` | `materiaId,gradoId,anoEscolarId,horasPedagogicas` | `@NotBlank`, `@NotNull`, `@Min(1)`, `@Max(15)` | `@Data` |
| `MallaCurricularBulkRequest` | `materiaId,anoEscolarId,grados[]` | `@NotBlank`, `@NotEmpty`, `@Valid` | `@Data` |
| `MallaCurricularBulkRequest.GradoHoras` | `gradoId,horasPedagogicas` | `@NotBlank`, `@NotNull`, `@Min(1)`, `@Max(15)` | `@Data` |
| `BloqueRequest` | `numeroBloque,horaInicio,horaFin,tipo` | `@NotNull`, `@Min(1)`, `@Pattern` para hora `HH:mm` y tipo | `@Getter/@Setter` |
| `JornadaDiaRequest` | `bloques[]` | `@NotNull`, `@Size(min=1)`, `@Valid` | `@Getter/@Setter` |
| `CopiarJornadaRequest` | `diasDestino[]` | `@NotNull`, `@Size(min=1)`, elementos `@Min(1) @Max(5)` | `@Getter/@Setter` |

### Response DTOs

| DTO | Campos principales | Builder/Lombok |
|---|---|---|
| `AuthResponse` | `token,tipo,id,email,nombre,apellido,rol,profesorId,alumnoId` | `@Data @Builder` |
| `ApiErrorResponse` | `code,message,status,field,path,timestamp,details` | `@Data @Builder` |
| `AnoEscolarResponse` | `id,ano,fechaInicioPlanificacion,fechaInicio,fechaFin,estado,createdAt,updatedAt` | `@Data @Builder` |
| `MateriaResponse` | `id,nombre,icono,createdAt,updatedAt` | `@Data @Builder` |
| `MateriaPageResponse` | `content,page,size,totalElements,totalPages,sortBy,sortDir,hasNext,hasPrevious` | `@Data @Builder` |
| `ProfesorResponse` | `id,rut,nombre,apellido,email,telefono,fechaContratacion,activo,materias,createdAt,updatedAt` | `@Data @Builder` |
| `ProfesorResponse.MateriaInfo` | `id,nombre,icono` | `@Data @Builder` |
| `CursoResponse` | `id,nombre,letra,gradoId,gradoNombre,anoEscolarId,anoEscolar,activo,alumnosMatriculados,cantidadMaterias,totalHorasPedagogicas,materias,createdAt,updatedAt` | `@Data @Builder` |
| `CursoResponse.MateriaCargaResponse` | `materiaId,materiaNombre,materiaIcono,horasPedagogicas` | `@Data @Builder` |
| `AlumnoResponse` | datos personales + apoderado + auditoría + matrícula opcional (`matriculaId,cursoId,cursoNombre,gradoNombre,estadoMatricula,fechaMatricula`) | `@Data @Builder` |
| `AlumnoPageResponse` | contrato paginado equivalente a materias | `@Data @Builder` |
| `MallaCurricularResponse` | `id,materiaId,materiaNombre,materiaIcono,gradoId,gradoNombre,gradoNivel,anoEscolarId,anoEscolar,horasPedagogicas,activo,createdAt,updatedAt` | `@Data @Builder` |
| `MatriculaResponse` | `id,alumno*,curso*,gradoNombre,anoEscolar*,fechaMatricula,estado,createdAt,updatedAt` | `@Data @Builder` |
| `BloqueHorarioResponse` | `id,numeroBloque,horaInicio,horaFin,tipo,materia*,profesor*` | `@Getter/@Setter @Builder` |
| `JornadaDiaResponse` | `diaSemana,nombreDia,bloques,totalBloquesClase,horaInicio,horaFin` | `@Getter/@Setter @Builder` |
| `JornadaCursoResponse` | `cursoId,cursoNombre,dias,resumen` | `@Getter/@Setter @Builder` |
| `JornadaResumenResponse` | `cursoId,diasConfigurados,bloquesClasePorDia,totalBloquesClaseSemana` | `@Getter/@Setter @Builder` |

---

## SECCIÓN 11: DECISIONES DE NEGOCIO IMPLEMENTADAS

### Estados de Año Escolar

Implementado en método transiente `AnoEscolar.getEstado()`:

```java
if (hoy.isBefore(fechaInicioPlanificacion)) return FUTURO;
else if (hoy.isBefore(fechaInicio)) return PLANIFICACION;
else if (!hoy.isAfter(fechaFin)) return ACTIVO;
else return CERRADO;
```

Reglas adicionales en controller:

- `planificacion < inicio < fin`
- `ano == fechaInicio.year`
- no solapamiento entre rangos de años
- no editar año `CERRADO`

### Generación automática de curso (nombre + letra)

`CursoController`:

- busca letras ocupadas por `(gradoId, anoEscolarId)`.
- toma primera letra activa disponible de `seccion_catalogo` ordenada por `orden`.
- nombre resultante: `"<nombreGrado> <letra>"`.
- si no hay letra: `ApiException(CURSO_SIN_SECCION_DISPONIBLE)`.

### Matrícula (alumno desacoplado de curso)

- `Alumno` no guarda curso actual.
- `Matricula` vincula alumno-curso-año con estado.
- Restricción fuerte: una sola matrícula `ACTIVA` por alumno y año (índice parcial + validación de use case).

### Malla curricular (materia-grado-año)

- `malla_curricular` define oferta académica por año.
- Campo horario oficial: `horasPedagogicas` (`horas_pedagogicas` en BD).
- Referencia de negocio: 1 hora pedagógica = 45 minutos.
- Validación actual en API: mínimo 1, máximo 15.
- Unicidad por combinación `(materia, grado, año)`.
- `bulk` hace upsert y desactiva los grados omitidos para esa materia-año.

### Validaciones de RUT, email, teléfono duplicados

Implementadas explícitamente en `ProfesorController`:

- RUT duplicado en create/update
- Email duplicado en create/update
- Teléfono duplicado (si informado) en create/update
- RUT inmutable en update (`PROFESOR_RUT_INMUTABLE`)

Para alumnos:

- solo unicidad de `rut` validada en controller.

### Otras reglas relevantes

- Jornada escolar por día:
  - `diaSemana` entre 1 y 5
  - bloques secuenciales desde 1
  - sin gaps (`horaInicio[n] == horaFin[n-1]`)
  - rango horario permitido `07:00 - 18:00`
  - máximo 1 bloque `ALMUERZO`
  - primer y último bloque deben ser `CLASE`
  - al menos 1 bloque `CLASE`
  - no se modifica jornada si el año escolar del curso está `CERRADO`
- Transiciones de matrícula restringidas (`ACTIVA<->RETIRADO/TRASLADADO`).

---

## SECCIÓN 12: CONTRATOS CLAVE PARA FRONTEND

### Respuestas enriquecidas

- `GET /api/cursos/{id}` retorna:
  - datos del curso
  - `alumnosMatriculados`
  - `cantidadMaterias`
  - `totalHorasPedagogicas`
  - `materias[]` con carga horaria por materia

- `GET /api/alumnos` y `GET /api/alumnos/{id}`:
  - si se envía `anoEscolarId`, agrega datos de matrícula activa (`curso`, `grado`, `estado`, `fechaMatricula`).

### Paginación

Contratos usados (`MateriaPageResponse`, `AlumnoPageResponse`):

- `content`
- `page`
- `size`
- `totalElements`
- `totalPages`
- `sortBy`
- `sortDir`
- `hasNext`
- `hasPrevious`

### Filtros

- `GET /api/alumnos`:
  - búsqueda `q`
  - filtros `anoEscolarId`, `cursoId`, `gradoId`
- `GET /api/cursos`:
  - `anoEscolarId`, `gradoId`
- `GET /api/malla-curricular`:
  - `anoEscolarId` obligatorio
- `GET /api/malla-curricular/materia/{materiaId}`:
  - `anoEscolarId`
- `GET /api/malla-curricular/grado/{gradoId}`:
  - `anoEscolarId`
- `GET /api/cursos/{cursoId}/jornada`:
  - filtro opcional `diaSemana`

### Uso de `anoEscolarId` para enriquecer respuestas

Regla práctica actual:

- En alumnos, `anoEscolarId` activa la resolución de matrícula y permite filtrar por curso/grado.
- Sin `anoEscolarId`, el backend devuelve solo ficha personal del alumno.

---

## SECCIÓN 13: CONFIGURACIÓN

Archivos:

- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/resources/application.yml`
- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/resources/application-dev.yml`
- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/resources/application-prod.yml`

### Propiedades relevantes

- `spring.profiles.active=dev`
- `server.port=8080`
- datasource Postgres Supabase (`url`, `username`, `password`, `driver-class-name`)
- JPA:
  - `ddl-auto=validate`
  - dialect PostgreSQL
  - `show-sql` true en dev / false en prod
- Flyway:
  - enabled
  - location `classpath:db/migration`
  - en dev: `baseline-on-migrate=true`, `validate-on-migrate=true`

### JWT

- `jwt.secret`
- `jwt.expiration=86400000` (24h)

### CORS

`CorsConfig` permite orígenes:

- `http://localhost:5173`
- `http://localhost:8080`
- `http://localhost:3000`

Métodos permitidos: `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

### Variables de entorno necesarias

No hay uso explícito de `${ENV_VAR}` en YAML actual; credenciales y secretos están hardcodeados en archivos de configuración.

---

## SECCIÓN 14: ÍNDICES Y PERFORMANCE

### Índices existentes (migraciones)

- `usuario`: `idx_usuario_email`, `idx_usuario_rol`
- `ano_escolar`: `idx_ano_escolar_activo`, `idx_ano_escolar_ano`
- `grado`: `idx_grado_nivel`
- `profesor`: `idx_profesor_email`, `idx_profesor_activo`
- `curso`: `idx_curso_grado`, `idx_curso_ano_escolar`, `idx_curso_activo`, `uq_curso_grado_ano_letra`
- `malla_curricular`: `idx_malla_curricular_ano_escolar`, `idx_malla_curricular_materia_ano`, `idx_malla_curricular_grado_ano`, `idx_malla_curricular_activo`
- `matricula`: `idx_matricula_alumno`, `idx_matricula_curso`, `idx_matricula_ano_escolar`, `idx_matricula_estado`, `uq_matricula_alumno_ano_activa`
- `bloque_horario`: tabla existente en Supabase; índices/constraints no versionados en migraciones del repo

### Índices recomendados pendientes

- Compuesto para query frecuente de matrículas activas por curso:
  - `(curso_id, estado)` en `matricula`.
- Compuesto para filtrado por año + estado en `matricula`:
  - `(ano_escolar_id, estado)` si crece volumen.
- Si se intensifica búsqueda por RUT parcial en alumnos, considerar estrategia específica (índice funcional/GIN según patrón real).

### Queries potencialmente problemáticas a escala

- `AlumnoController.getMatriculaMap`: carga todas las matrículas activas de un año y luego filtra en memoria por `alumnoIds`.
- Filtrado por grado en alumnos (`getAlumnoIdsByMatriculaFilters`) hace parte en memoria tras consulta por año.
- `CursoController.obtener`: calcula malla y agregados por request; puede crecer en costo si se vuelve endpoint masivo.

---

## SECCIÓN 15: ESTADO ACTUAL Y LO QUE FALTA

### Estado por módulo

| Módulo | Estado | Endpoints implementados |
|---|---|---|
| Auth | ✅ | `/api/auth/login`, `/api/auth/me` |
| Años Escolares | ✅ | list/get/get activo/create/update |
| Grados | ✅ | list/get |
| Materias | ✅ | list/get/create/update/delete |
| Malla Curricular | ✅ | list por año, por materia, por grado, create, update, bulk, delete lógico |
| Cursos | ✅ | list/get/create/update |
| Profesores | ✅ | list/get/create/update |
| Alumnos | ✅ | list/get/create/update |
| Matrículas | ✅ | create, list por curso, historial por alumno, cambio estado |
| Jornada escolar por curso | ✅ | guardar día, obtener jornada/resumen, copiar día, eliminar día |
| Ownership por rol no-admin | ❌ | sin endpoints efectivos |
| Asistencia | ❌ | no existe |
| Reportes | ❌ | no existe |
| Dashboards | ❌ | no existe |

### Qué falta para que frontend deje DataContext completamente

- Endpoints orientados a rol `PROFESOR`/`APODERADO` (actualmente casi todo es `ADMIN`).
- Ownership en backend (filtro por `principal.profesorId/alumnoId`).
- Endpoints de agregación operacional (dashboard, indicadores) si frontend hoy los calcula localmente.
- Cobertura de módulos pendientes (asistencia/reportes) que normalmente DataContext simula.

### Próximos módulos lógicos

1. Asignación de materia/profesor sobre bloques de jornada (pendiente; hoy la estructura se guarda sin `materia_id` ni `profesor_id`).
2. Asistencia (registro por bloque/alumno, consolidado diario/mensual).
3. Reportes (académico, matrícula, carga docente).
4. Dashboards (KPIs por año/curso/docente).

---

## SECCIÓN 16: CONVENCIONES DEL PROYECTO

### Nomenclatura

- Entidades: singular en español (`Alumno`, `Profesor`, `AnoEscolar`).
- Tablas: snake_case singular (`alumno`, `malla_curricular`).
- Repositorios: `{Entidad}Repository`.
- Use cases: verbo + sustantivo (`MatricularAlumno`, `GuardarJornadaDia`).
- Controllers: `{Dominio}Controller`.
- DTOs: `{Dominio}Request` / `{Dominio}Response`.

### Estilo de API

- Rutas en español y kebab-case: `/api/anos-escolares`, `/api/malla-curricular`.
- JSON para request/response.
- Fechas:
  - requests mixtos (algunas `String`, algunas `LocalDate` serializadas).
  - respuestas mayormente ISO-8601 (`toString()` de fechas/DateTime).

### Cómo agregar una nueva entidad (paso a paso)

1. Crear migración Flyway nueva `V{n}__...sql` con tabla/índices/constraints.
2. Crear entidad JPA en `entity/` con mapping exacto a columnas.
3. Agregar `Repository`.
4. Definir DTOs request/response.
5. Crear controller endpoint(s).
6. Si hay reglas de negocio complejas: crear `usecase/` dedicado.
7. Incorporar manejo de errores con `BusinessException`/`ApiException`.

### Cómo agregar un nuevo endpoint

1. Definir contrato DTO.
2. Implementar método en controller con `@*Mapping`.
3. Añadir `@PreAuthorize` acorde al rol.
4. Reusar repository o crear use case si hay orquestación.
5. Asegurar errores consistentes para `GlobalExceptionHandler`.

### Cómo agregar un nuevo use case

1. Crear clase en `usecase/{dominio}` como `@Component`.
2. Inyectar repositorios necesarios.
3. Encapsular reglas y validaciones.
4. Usar `@Transactional` si cambia estado en más de una operación.
5. Invocar desde controller.

### Cómo agregar nueva migración Flyway

1. Crear `src/main/resources/db/migration/V{n}__descripcion.sql`.
2. No editar `V` anteriores.
3. Incluir constraints/índices para invariantes de negocio.
4. Ejecutar arranque con Flyway validate/migrate.

---

## SECCIÓN FINAL: CÓDIGO MUERTO, INCONSISTENCIAS Y RIESGOS

### Hallazgos críticos

1. Seguridad de `/api/auth/me`:
- `SecurityConfig` tiene `permitAll("/api/auth/**")`, por lo que `/api/auth/me` queda público.
- `AuthController.me` asume `@AuthenticationPrincipal UserPrincipal user` no nulo y usa `user.get...`; sin token puede causar `NullPointerException` y terminar en 500.

2. Drift esquema/código en `ano_escolar`:
- Entidad y DTO usan `fecha_inicio_planificacion`.
- `V3` no crea esa columna y sí crea `activo` que no se usa en entidad.

3. `alumno` fuera de versionado explícito:
- `V7` y `V8` son placeholders sin DDL/DML real.
- Dificulta bootstrap reproducible de una BD limpia solo con el repositorio.

### Hallazgos importantes

4. `UnauthorizedException` existe pero no se usa en ningún flujo.

5. Import duplicado en `CursoController` (`EstadoMatricula` importado dos veces).

6. Reglas de ownership no implementadas:
- existen roles `PROFESOR` y `APODERADO` en auth, pero casi todo el dominio exige `ADMIN`.

7. Secretos hardcodeados:
- datasource password y `jwt.secret` están en YAML versionado.

### Impacto operativo

- Riesgo de errores 500 evitables en `/api/auth/me`.
- Riesgo de fallas de arranque en entornos nuevos por desalineación de esquema.
- Riesgo de seguridad por exposición de secretos en repositorio.
