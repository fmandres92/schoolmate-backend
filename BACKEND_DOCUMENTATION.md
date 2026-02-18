# BACKEND_DOCUMENTATION.md

## SECCIÓN 1: RESUMEN EJECUTIVO

`schoolmate-hub-api` es un backend REST para gestión escolar (catálogos académicos, cursos, profesores, alumnos, matrícula, jornada escolar por curso y portal de apoderado). La API está construida en Spring Boot con seguridad JWT stateless y acceso a PostgreSQL en Supabase usando JPA + Flyway. La implementación actual mantiene foco administrativo (`ADMIN`), pero ya incorpora flujos de `APODERADO` para consultar hijos, asistencia y jornada con ownership. El diseño separa catálogos estables (`materia`, `grado`, `año`) de vínculos temporales (`malla_curricular`, `matricula`) y bloques de jornada (`bloque_horario`).

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
| Ownership por profesor/apoderado | ⚠️ parcial (profesor + apoderado en dominios acotados) |
| Asistencia, reportes, dashboards | ⚠️ parcial (asistencia operativa + consulta apoderado; reportes/dashboards pendientes) |

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
- Tiempo centralizado: `ClockProvider` + `TimeContext` permiten controlar `today/now` en entorno `dev` sin afectar `prod`.

### Manejo centralizado de excepciones (`GlobalExceptionHandler`)

Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/exception/GlobalExceptionHandler.java`

Captura y normaliza:

- `BadCredentialsException` -> `AUTH_BAD_CREDENTIALS` (401)
- `ResourceNotFoundException` -> `RESOURCE_NOT_FOUND` (404)
- `BusinessException` -> `BUSINESS_RULE` (400), con soporte opcional de `details` (`Map<String,String>`)
- `ConflictException` -> `CONFLICT` (409), con mensaje de negocio explícito
- `ApiException` -> `errorCode` específico (status según `ErrorCode`)
  - soporta mensaje personalizado y `details` (`Map<String,String>`) en errores de negocio avanzados
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
│   ├── common/rut            # utilidades de normalización de RUT
│   ├── common/time           # proveedor centralizado de fecha/hora
│   ├── controller            # endpoints REST
│   ├── dto                   # DTOs de portal apoderado y proyecciones
│   ├── dto/request           # contratos de entrada
│   ├── dto/response          # contratos de salida
│   ├── entity                # entidades JPA
│   ├── enums                 # enums de dominio
│   ├── exception             # errores y handler global
│   ├── repository            # repositorios JPA
│   ├── security              # JWT + principal + filter
│   ├── specification         # filtros dinámicos JPA
│   ├── usecase/asistencia    # casos de uso de asistencia por bloque
│   ├── usecase/apoderado     # casos de uso portal/gestión apoderado
│   ├── usecase/auth          # caso de uso login
│   ├── usecase/jornada       # casos de uso jornada escolar
│   ├── usecase/matricula     # casos de uso matrícula
│   └── usecase/profesor      # casos de uso de profesor
└── src/main/resources
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    ├── messages_es.properties
    └── db/migration          # migraciones Flyway V1..V17
```

### TODAS las clases por paquete

- `com.schoolmate.api`
  - `SchoolmateApiApplication`
- `com.schoolmate.api.config`
  - `CorsConfig`
  - `SecurityConfig`
- `com.schoolmate.api.common.time`
  - `ClockProvider`
  - `TimeContext`
- `com.schoolmate.api.common.rut`
  - `RutNormalizer`
- `com.schoolmate.api.controller`
  - `AlumnoController`
  - `AnoEscolarController`
  - `ApoderadoController`
  - `ApoderadoPortalController`
  - `AuthController`
  - `CursoController`
  - `DevToolsController` (solo perfil `dev`)
  - `AsistenciaController`
  - `GradoController`
  - `JornadaController`
  - `MallaCurricularController`
  - `MateriaController`
  - `MatriculaController`
  - `ProfesorController`
  - `ProfesorHorarioController`
  - `ProfesorMeController`
- `com.schoolmate.api.dto`
  - `AlumnoApoderadoResponse`
  - `ApoderadoBuscarResponse`
  - `ApoderadoRequest`
  - `ApoderadoResponse`
  - `AsistenciaDiaResponse`
  - `AsistenciaMensualResponse`
  - `RegistroConFecha`
  - `ResumenAsistenciaResponse`
- `com.schoolmate.api.dto.request`
  - `AlumnoRequest`
  - `AsignarMateriaRequest`
  - `AsignarProfesorRequest`
  - `AnoEscolarRequest`
  - `BloqueRequest`
  - `GuardarAsistenciaRequest`
  - `CopiarJornadaRequest`
  - `CursoRequest`
  - `JornadaDiaRequest`
  - `LoginRequest`
  - `MallaCurricularBulkRequest`
  - `MallaCurricularRequest`
  - `MateriaRequest`
  - `MatriculaRequest`
  - `ProfesorRequest`
  - `RegistroAlumnoRequest`
- `com.schoolmate.api.dto.response`
  - `AlumnoPageResponse`
  - `AlumnoResponse`
  - `AsignacionMateriaResumenResponse`
  - `AsignacionProfesoresResumenResponse`
  - `AnoEscolarResponse`
  - `ApiErrorResponse`
  - `AsistenciaClaseResponse`
  - `BloqueHorarioResponse`
  - `BloquePendienteProfesorResponse`
  - `BloqueProfesorResumenResponse`
  - `ClaseHoyResponse`
  - `ClasesHoyResponse`
  - `ConflictoHorarioResponse`
  - `EstadoClaseHoy`
  - `AuthResponse`
  - `CursoResponse`
  - `JornadaCursoResponse`
  - `JornadaDiaResponse`
  - `JornadaResumenResponse`
  - `MateriaDisponibleResponse`
  - `MallaCurricularResponse`
  - `MateriaPageResponse`
  - `MateriaResponse`
  - `MatriculaResponse`
  - `ProfesorDisponibleResponse`
  - `ProfesorHorarioResponse`
  - `ProfesorResumenAsignacionResponse`
  - `ProfesoresDisponiblesResponse`
  - `ProfesorResponse`
  - `RegistroAsistenciaResponse`
- `com.schoolmate.api.entity`
  - `Apoderado`
  - `ApoderadoAlumno`
  - `ApoderadoAlumnoId`
  - `Alumno`
  - `AnoEscolar`
  - `AsistenciaClase`
  - `BloqueHorario`
  - `Curso`
  - `Grado`
  - `MallaCurricular`
  - `Materia`
  - `Matricula`
  - `Profesor`
  - `RegistroAsistencia`
  - `SeccionCatalogo`
  - `Usuario`
- `com.schoolmate.api.enums`
  - `EstadoAnoEscolar`
  - `EstadoAsistencia`
  - `EstadoMatricula`
  - `Rol`
  - `TipoBloque`
- `com.schoolmate.api.exception`
  - `ApiException`
  - `BusinessException`
  - `ConflictException`
  - `ErrorCode`
  - `GlobalExceptionHandler`
  - `ResourceNotFoundException`
  - `UnauthorizedException`
- `com.schoolmate.api.repository`
  - `ApoderadoAlumnoRepository`
  - `ApoderadoRepository`
  - `AlumnoRepository`
  - `AnoEscolarRepository`
  - `AsistenciaClaseRepository`
  - `BloqueHorarioRepository`
  - `CursoRepository`
  - `GradoRepository`
  - `MallaCurricularRepository`
  - `MateriaRepository`
  - `MatriculaRepository`
  - `ProfesorRepository`
  - `RegistroAsistenciaRepository`
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
- `com.schoolmate.api.usecase.apoderado`
  - `CrearApoderadoConUsuario`
  - `ObtenerAlumnosApoderado`
  - `ObtenerAsistenciaMensualAlumno`
  - `ObtenerResumenAsistenciaAlumno`
- `com.schoolmate.api.usecase.auth`
  - `LoginUsuario`
- `com.schoolmate.api.usecase.asistencia`
  - `GuardarAsistenciaClase`
  - `ObtenerAsistenciaClase`
- `com.schoolmate.api.usecase.jornada`
  - `AsignarMateriaBloque`
  - `AsignarProfesorBloque`
  - `CopiarJornadaDia`
  - `EliminarJornadaDia`
  - `GuardarJornadaDia`
  - `ObtenerJornadaCurso`
  - `ObtenerMateriasDisponibles`
  - `ObtenerProfesoresDisponibles`
  - `ObtenerResumenAsignacionMaterias`
  - `ObtenerResumenAsignacionProfesores`
  - `QuitarMateriaBloque`
  - `QuitarProfesorBloque`
- `com.schoolmate.api.usecase.matricula`
  - `CambiarEstadoMatricula`
  - `MatricularAlumno`
  - `ValidarAccesoMatriculasCursoProfesor`
- `com.schoolmate.api.usecase.profesor`
  - `CrearProfesorConUsuario`
  - `ObtenerClasesHoyProfesor`

---

## SECCIÓN 4: MODELO DE DATOS (ENTIDADES JPA)

> Nota: donde hay divergencia entre entidad y migraciones SQL, se marca explícitamente.

### `Usuario` (`usuario`)

| Campo Java | Tipo Java | Columna BD | Tipo BD (migración) | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `email` | `String` | `email` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `rut` | `String` | `rut` | `VARCHAR(20)` | nullable, UNIQUE parcial (`rut IS NOT NULL`) |
| `passwordHash` | `String` | `password_hash` | `VARCHAR(255)` | NOT NULL |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` | NOT NULL |
| `apellido` | `String` | `apellido` | `VARCHAR(100)` | NOT NULL |
| `rol` | `Rol` | `rol` | `VARCHAR(20)` | NOT NULL |
| `profesorId` | `String` | `profesor_id` | `VARCHAR(36)` | nullable |
| `apoderadoId` | `String` | `apoderado_id` | `VARCHAR(36)` | nullable |
| `activo` | `Boolean` | `activo` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones JPA: no hay relaciones `@ManyToOne`; `profesorId/apoderadoId` son campos planos.

Nota de integridad:

- En migraciones históricas (`V1`/`V2`) `usuario.profesor_id` y `usuario.alumno_id` se usaron como referencias lógicas.
- Estado actual versionado: `usuario` referencia lógicamente `profesor_id` y `apoderado_id` (sin FK declarada en migraciones).

### `Apoderado` (`apoderado`)

| Campo Java | Tipo Java | Columna BD | Tipo BD esperado | Constraints |
|---|---|---|---|---|
| `id` | `String` | `id` | `VARCHAR(36)` | PK |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` | NOT NULL |
| `apellido` | `String` | `apellido` | `VARCHAR(100)` | NOT NULL |
| `rut` | `String` | `rut` | `VARCHAR(20)` | UNIQUE, nullable |
| `email` | `String` | `email` | `VARCHAR(255)` | UNIQUE, nullable |
| `telefono` | `String` | `telefono` | `VARCHAR(30)` | nullable |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

### `ApoderadoAlumno` (`apoderado_alumno`)

| Campo Java | Tipo Java | Columna BD | Tipo BD esperado | Constraints |
|---|---|---|---|---|
| `id.apoderadoId` | `String` | `apoderado_id` | `VARCHAR(36)` | PK compuesta, FK lógica |
| `id.alumnoId` | `String` | `alumno_id` | `VARCHAR(36)` | PK compuesta, FK lógica |
| `esPrincipal` | `Boolean` | `es_principal` | `BOOLEAN` | NOT NULL |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |

Relaciones JPA:
- `ManyToOne` hacia `Apoderado` con `@MapsId("apoderadoId")`.
- `ManyToOne` hacia `Alumno` con `@MapsId("alumnoId")`.

Índices: `idx_usuario_email`, `idx_usuario_rol`, `ux_usuario_rut_not_null`.

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

Regla de estado:

- `AnoEscolar` calcula su estado con `calcularEstado(LocalDate fechaReferencia)` (no depende de `LocalDate.now()` dentro de la entidad).

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
| `horasPedagogicasContrato` | `Integer` | `horas_pedagogicas_contrato` | `INTEGER` | nullable |
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
  (profesor_id, apoderado_id como string sin FK JPA)

apoderado 1---* apoderado_alumno *---1 alumno

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
14. `V14__profesor_horas_pedagogicas_contrato.sql`
15. `V15__usuario_rut_y_backfill_profesores.sql`
16. `V16__create_asistencia.sql`
17. `V17__apoderado_entity.sql`

### Qué hace cada migración

| Migración | Cambios |
|---|---|
| `V1` | Crea `usuario`, índices por email y rol |
| `V2` | Seed usuarios (`ADMIN`, `PROFESOR`, `APODERADO`) con credenciales dev (`admin123`,`prof123`,`apod123`) y vínculo lógico `profesor_id='p2'` para usuario profesor |
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
| `V14` | Agrega columna nullable `profesor.horas_pedagogicas_contrato` |
| `V15` | Agrega `usuario.rut` (nullable + índice único parcial), completa RUT en usuarios existentes vinculados a profesor y crea usuarios faltantes para profesores activos (password inicial: RUT normalizado en BCrypt) |
| `V16` | Crea `asistencia_clase` y `registro_asistencia` con FKs, índices y unicidad por bloque/fecha y por alumno en una clase |
| `V17` | Migración marcador: documenta refactor de apoderado ejecutado directamente en Supabase (`apoderado`, `apoderado_alumno`, `usuario.apoderado_id`) sin DDL en Flyway |

### Estado resultante del esquema (según migraciones + código)

- Modelo oficial en código y queries usa: `usuario`, `apoderado`, `apoderado_alumno`, `ano_escolar`, `grado`, `materia`, `profesor`, `profesor_materia`, `curso`, `seccion_catalogo`, `malla_curricular`, `alumno`, `matricula`, `bloque_horario`, `asistencia_clase`, `registro_asistencia`.
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
2. Crear nueva `V17+` para cualquier ajuste de esquema.
3. Incluir `ALTER` explícito para cerrar drift (`ano_escolar`, `alumno`) si existe.

---

## SECCIÓN 6: SEGURIDAD Y AUTENTICACIÓN

### Flujo JWT completo

1. `POST /api/auth/login` recibe `LoginRequest(identificador,password)`.
2. `LoginUsuario`:
   - si `identificador` contiene `@`, busca por email (`UsuarioRepository.findByEmail`).
   - si no contiene `@`, normaliza RUT y busca por `UsuarioRepository.findByRut`.
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

- `id`, `email`, `password`, `rol`, `profesorId`, `apoderadoId`, `nombre`, `apellido`.

Authorities:

- una autoridad: `ROLE_<ROL>`.

Datos en token (`JwtTokenProvider`):

- `sub=email`, claims: `id`, `rol`, `profesorId`, `apoderadoId`, `nombre`, `apellido`, `iat`, `exp`.

### Manejo de roles (`ADMIN`, `PROFESOR`, `APODERADO`)

Estado actual de autorización por código:

- Casi todos los controllers de dominio: `@PreAuthorize("hasRole('ADMIN')")` a nivel clase o método.
- `AnoEscolarController.obtenerActivo`: `isAuthenticated()`.
- Endpoint específico `PROFESOR`: `GET /api/profesores/{profesorId}/horario` (también accesible para `ADMIN`).
- Endpoint específico `PROFESOR`: `GET /api/profesor/mis-clases-hoy` (solo `PROFESOR`).
- `GET /api/matriculas/curso/{cursoId}` está disponible para `ADMIN` y `PROFESOR` con ownership por bloques activos del curso en el año escolar activo.
- Endpoints admin de apoderado: `POST/GET /api/apoderados...` (solo `ADMIN`).
- Endpoints portal apoderado: `GET /api/apoderado/...` (solo `APODERADO` con ownership por vínculo `apoderado_alumno`).
- `GET /api/cursos/{cursoId}/jornada` está disponible para `ADMIN` y `APODERADO`, con ownership para apoderado.

### Uso de `@PreAuthorize`

Sí existe en controllers de:

- Alumnos, Años, Apoderados, Cursos, Grados, Jornada, Malla, Materias, Matrículas, Profesores.
- `DevToolsController` no usa `@PreAuthorize`; su exposición depende estrictamente de `@Profile("dev")`.

### Reglas de ownership (profesor y apoderado)

Implementación actual:

- En `ProfesorHorarioController`, si el rol autenticado es `PROFESOR`, solo puede consultar su propio `profesorId`.
- Si intenta consultar otro `profesorId`, responde `403` (`AccessDeniedException` -> `ACCESS_DENIED`).
- En `AsistenciaController`, el profesor autenticado solo puede registrar/consultar asistencia de bloques donde está asignado como docente.
- En `ApoderadoPortalController`, cada consulta valida vínculo `apoderadoId`->`alumnoId` antes de exponer asistencia o resumen.
- En `JornadaController` (`GET /api/cursos/{cursoId}/jornada`), si el rol es `APODERADO` valida que tenga al menos un alumno con matrícula `ACTIVA` en ese curso.

---

## SECCIÓN 7: ENDPOINTS API (CATÁLOGO COMPLETO)

> Todos los endpoints listados provienen de controladores actuales.

### Dominio: Auth

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/auth/login` | Login y emisión JWT (email o RUT) | Público | Body JSON | `LoginRequest {identificador,password}` | `AuthResponse {token,tipo,id,email,nombre,apellido,rol,profesorId,apoderadoId}` | `LoginUsuario` | `AUTH_BAD_CREDENTIALS`, `VALIDATION_FAILED` |
| `GET /api/auth/me` | Datos del usuario actual | Público por `permitAll` (intención: autenticado) | Header `Authorization` esperado | - | `Map{id,email,nombre,apellido,rol,profesorId,apoderadoId}` | directo | sin token puede terminar en `INTERNAL_SERVER_ERROR` (NPE) |

### Dominio: Apoderados (Admin)

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/apoderados` | Crea apoderado + usuario `ROL=APODERADO`, o vincula un apoderado existente por RUT | `ADMIN` | Body JSON | `ApoderadoRequest` | `ApoderadoResponse` (`201`) | `CrearApoderadoConUsuario` | `RESOURCE_NOT_FOUND`, `CONFLICT`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/apoderados/buscar-por-rut?rut=...` | Busca apoderado por RUT normalizado | `ADMIN` | Query `rut` | - | `ApoderadoBuscarResponse` | directo repository | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE` |
| `GET /api/apoderados/por-alumno/{alumnoId}` | Retorna apoderado vinculado al alumno (MVP: primer vínculo) | `ADMIN` | Path `alumnoId` | - | `ApoderadoResponse` o `204` | directo repository | `RESOURCE_NOT_FOUND` |

### Dominio: Portal Apoderado

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/apoderado/mis-alumnos` | Lista hijos vinculados al apoderado autenticado, con matrícula activa del año activo si existe | `APODERADO` | `Authorization` | - | `List<AlumnoApoderadoResponse>` | `ObtenerAlumnosApoderado` | `ACCESS_DENIED` (si principal inválido) |
| `GET /api/apoderado/alumnos/{alumnoId}/asistencia/mensual?mes=...&anio=...` | Asistencia diaria agregada por bloques para un mes (`PRESENTE/AUSENTE/PARCIAL`) | `APODERADO` (ownership) | Path `alumnoId`, Query `mes,anio` | - | `AsistenciaMensualResponse` | `ObtenerAsistenciaMensualAlumno` | `ACCESS_DENIED`, `RESOURCE_NOT_FOUND`, `BUSINESS_RULE` |
| `GET /api/apoderado/alumnos/{alumnoId}/asistencia/resumen?anoEscolarId=...` | Resumen anual de asistencia por bloques con porcentaje | `APODERADO` (ownership) | Path `alumnoId`, Query `anoEscolarId` | - | `ResumenAsistenciaResponse` | `ObtenerResumenAsistenciaAlumno` | `ACCESS_DENIED`, `RESOURCE_NOT_FOUND` |

### Dominio: DevTools (solo perfil `dev`)

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/dev/clock` | Retorna fecha/hora actual del clock centralizado y si está overrideado | según config de seguridad vigente | - | - | `{currentDateTime,isOverridden}` | directo (`ClockProvider`) | - |
| `POST /api/dev/clock` | Fija el clock a una fecha/hora ISO-8601 | según config de seguridad vigente | Body JSON | `{dateTime}` | `{currentDateTime,isOverridden}` | directo (`ClockProvider`) | `BUSINESS_RULE` por formato inválido, `INTERNAL_SERVER_ERROR`/`UnsupportedOperationException` fuera de `dev` |
| `DELETE /api/dev/clock` | Resetea clock a tiempo del sistema | según config de seguridad vigente | - | - | `{currentDateTime,isOverridden}` | directo (`ClockProvider`) | `INTERNAL_SERVER_ERROR`/`UnsupportedOperationException` fuera de `dev` |

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
| `GET /api/profesores/{id}` | Obtiene profesor con contrato y horas asignadas del año ACTIVO (si existe) | `ADMIN` | Path | - | `ProfesorResponse` | directo | `RESOURCE_NOT_FOUND` |
| `POST /api/profesores` | Crea profesor con materias, horas de contrato y usuario asociado (`ROL=PROFESOR`) | `ADMIN` | Body | `ProfesorRequest` | `ProfesorResponse` | `CrearProfesorConUsuario` | `PROFESOR_RUT_DUPLICADO`, `PROFESOR_EMAIL_DUPLICADO`, `PROFESOR_TELEFONO_DUPLICADO`, `MATERIAS_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `PUT /api/profesores/{id}` | Actualiza profesor; RUT inmutable; permite setear/limpiar horas de contrato | `ADMIN` | Path + Body | `ProfesorRequest` | `ProfesorResponse` | directo | `RESOURCE_NOT_FOUND`, `PROFESOR_RUT_INMUTABLE`, duplicados, `MATERIAS_NOT_FOUND`, `VALIDATION_FAILED` |
| `GET /api/profesores/{profesorId}/horario?anoEscolarId=...` | Horario semanal consolidado por profesor y año escolar (solo bloques CLASE con materia) | `ADMIN`,`PROFESOR` (ownership) | Path `profesorId` + Query `anoEscolarId` | - | `ProfesorHorarioResponse` | directo | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED` |
| `GET /api/profesor/mis-clases-hoy` | Clases del día para el profesor autenticado (`estado` por ventana de 15 min, `asistenciaTomada` real por bloque+fecha) | `PROFESOR` | `Authorization` (usa `profesorId` del JWT) | - | `ClasesHoyResponse` | `ObtenerClasesHoyProfesor` | `ACCESS_DENIED` |

### Dominio: Alumnos

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/alumnos` | Lista paginada con búsqueda y enriquecimiento opcional por matrícula | `ADMIN` | Query: `page,size,sortBy,sortDir,anoEscolarId,cursoId,gradoId,q` | - | `AlumnoPageResponse` | directo + `AlumnoSpecifications` | - |
| `GET /api/alumnos/{id}` | Obtiene alumno; opcional matrícula activa por año | `ADMIN` | Path + Query opcional `anoEscolarId` | - | `AlumnoResponse` | directo | `RESOURCE_NOT_FOUND` |
| `GET /api/alumnos/buscar-por-rut` | Búsqueda exacta por RUT (normaliza con/sin puntos y guion) + matrícula opcional por año | `ADMIN` | Query: `rut` (obligatorio), `anoEscolarId` (opcional) | - | `AlumnoResponse` | directo | `RESOURCE_NOT_FOUND` |
| `POST /api/alumnos` | Crea alumno | `ADMIN` | Body | `AlumnoRequest` | `AlumnoResponse` | directo | `409` (RUT duplicado vía `ResponseStatusException`), `VALIDATION_FAILED` |
| `PUT /api/alumnos/{id}` | Actualiza alumno | `ADMIN` | Path + Body | `AlumnoRequest` | `AlumnoResponse` | directo | `RESOURCE_NOT_FOUND`, `409` (RUT duplicado), `VALIDATION_FAILED` |

### Dominio: Matrículas

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/matriculas` | Matricula alumno en curso/año | `ADMIN` | Body | `MatriculaRequest {alumnoId,cursoId,anoEscolarId,fechaMatricula?}` | `MatriculaResponse` | `MatricularAlumno` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/matriculas/curso/{cursoId}` | Matrículas activas por curso | `ADMIN`,`PROFESOR` (ownership por curso en año activo) | Path `cursoId` | - | `List<MatriculaResponse>` | directo + `ValidarAccesoMatriculasCursoProfesor` | `ACCESS_DENIED` |
| `GET /api/matriculas/alumno/{alumnoId}` | Historial de matrículas por alumno | `ADMIN` | Path `alumnoId` | - | `List<MatriculaResponse>` | directo | - |
| `PATCH /api/matriculas/{id}/estado` | Cambia estado (`ACTIVA/RETIRADO/TRASLADADO`) | `ADMIN` | Path + body map `{estado}` | `Map<String,String>` | `MatriculaResponse` | `CambiarEstadoMatricula` | `400` por body inválido, `RESOURCE_NOT_FOUND`, `BUSINESS_RULE` |

### Dominio: Asistencia

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/asistencia/clase` | Registra o edita (reemplaza registros) la asistencia de HOY para un bloque CLASE dentro de ventana ±15 min | `PROFESOR` (ownership por bloque) | Body | `GuardarAsistenciaRequest {bloqueHorarioId,fecha,registros[]}` | `AsistenciaClaseResponse` (`201 Created`) | `GuardarAsistenciaClase` | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/asistencia/clase?bloqueHorarioId=...&fecha=...` | Obtiene asistencia registrada para un bloque y fecha | `PROFESOR` (ownership por bloque) | Query `bloqueHorarioId`,`fecha` | - | `AsistenciaClaseResponse` | `ObtenerAsistenciaClase` | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED` |

### Dominio: Jornada

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `PUT /api/cursos/{cursoId}/jornada/{diaSemana}` | Guarda/reemplaza la jornada de un día | `ADMIN` | Path `cursoId`,`diaSemana`; Body | `JornadaDiaRequest` | `JornadaDiaResponse` | `GuardarJornadaDia` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/cursos/{cursoId}/jornada` | Obtiene jornada completa; opcionalmente filtra por día | `ADMIN`,`APODERADO` (ownership por matrícula activa en el curso) | Path `cursoId`; Query opcional `diaSemana` | - | `JornadaCursoResponse` | `ObtenerJornadaCurso` + validación en controller | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED` |
| `GET /api/cursos/{cursoId}/jornada/resumen` | Obtiene solo resumen semanal | `ADMIN` | Path `cursoId` | - | `JornadaResumenResponse` | `ObtenerJornadaCurso` (wrapper) | `RESOURCE_NOT_FOUND` |
| `POST /api/cursos/{cursoId}/jornada/{diaSemanaOrigen}/copiar` | Copia estructura de un día a días destino | `ADMIN` | Path `cursoId`,`diaSemanaOrigen`; Body | `CopiarJornadaRequest` | `JornadaCursoResponse` | `CopiarJornadaDia` | `BUSINESS_RULE`, `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |
| `DELETE /api/cursos/{cursoId}/jornada/{diaSemana}` | Elimina lógicamente jornada de un día | `ADMIN` | Path `cursoId`,`diaSemana` | - | `204` | `EliminarJornadaDia` | `BUSINESS_RULE`, `RESOURCE_NOT_FOUND` |
| `GET /api/cursos/{cursoId}/jornada/materias-disponibles?bloqueId=...` | Lista materias asignables a un bloque CLASE con cálculo de minutos | `ADMIN` | Path `cursoId`; Query `bloqueId` | - | `MateriasDisponiblesResponse` | `ObtenerMateriasDisponibles` | `RESOURCE_NOT_FOUND`, `BLOQUE_NO_ES_CLASE` |
| `PATCH /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/materia` | Asigna/reemplaza materia en bloque CLASE | `ADMIN` | Path `cursoId`,`bloqueId`; Body | `AsignarMateriaRequest` | `BloqueHorarioResponse` | `AsignarMateriaBloque` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `MATERIA_NO_EN_MALLA`, `MATERIA_EXCEDE_MINUTOS_MALLA` |
| `DELETE /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/materia` | Quita materia (y profesor) de bloque CLASE | `ADMIN` | Path `cursoId`,`bloqueId` | - | `BloqueHorarioResponse` | `QuitarMateriaBloque` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `BLOQUE_NO_ES_CLASE`, `BLOQUE_SIN_MATERIA` |
| `GET /api/cursos/{cursoId}/jornada/asignacion-materias` | Resumen de cobertura de malla por minutos y bloques | `ADMIN` | Path `cursoId` | - | `AsignacionMateriaResumenResponse` | `ObtenerResumenAsignacionMaterias` | `RESOURCE_NOT_FOUND` |
| `GET /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesores-disponibles` | Lista profesores disponibles para el bloque según materia y colisiones | `ADMIN` | Path `cursoId`,`bloqueId` | - | `ProfesoresDisponiblesResponse` | `ObtenerProfesoresDisponibles` | `RESOURCE_NOT_FOUND`, `BLOQUE_NO_ES_CLASE`, `BLOQUE_SIN_MATERIA_PARA_PROFESOR` |
| `PATCH /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesor` | Asigna/reemplaza profesor en bloque CLASE | `ADMIN` | Path `cursoId`,`bloqueId`; Body | `AsignarProfesorRequest` | `BloqueHorarioResponse` | `AsignarProfesorBloque` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `PROFESOR_NO_ENSENA_MATERIA`, `PROFESOR_COLISION_HORARIO` |
| `DELETE /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesor` | Quita profesor manteniendo materia del bloque | `ADMIN` | Path `cursoId`,`bloqueId` | - | `BloqueHorarioResponse` | `QuitarProfesorBloque` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `BLOQUE_NO_ES_CLASE` |
| `GET /api/cursos/{cursoId}/jornada/asignacion-profesores` | Resumen de asignación de docentes y bloques pendientes | `ADMIN` | Path `cursoId` | - | `AsignacionProfesoresResumenResponse` | `ObtenerResumenAsignacionProfesores` | `RESOURCE_NOT_FOUND` |

---

## SECCIÓN 8: USE CASES

### `com.schoolmate.api.usecase.asistencia.GuardarAsistenciaClase`

- Función: registrar o editar asistencia de una clase para la fecha actual.
- Repositorios/dependencias:
  - `BloqueHorarioRepository`, `MatriculaRepository`, `AsistenciaClaseRepository`, `RegistroAsistenciaRepository`, `ClockProvider`
- Validaciones:
  - bloque existe y es tipo `CLASE`
  - ownership: bloque debe pertenecer al profesor autenticado
  - fecha del request debe ser `clockProvider.today()`
  - fecha debe corresponder al `diaSemana` del bloque
  - ventana horaria permitida: `horaInicio-15min` a `horaFin+15min`
  - todos los alumnos del request deben estar con matrícula `ACTIVA` en el curso
  - no se aceptan alumnos duplicados en el request
- Comportamiento de persistencia:
  - si no existe asistencia para `(bloque,fecha)`, crea `asistencia_clase` + registros
  - si ya existe, actualiza `updatedAt`, borra registros previos y vuelve a insertar los registros enviados
- Manejo de concurrencia:
  - ante carrera al crear cabecera (`asistencia_clase`), recupera la existente y continúa como edición
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.asistencia.ObtenerAsistenciaClase`

- Función: recuperar asistencia registrada por bloque y fecha.
- Repositorios/dependencias:
  - `BloqueHorarioRepository`, `AsistenciaClaseRepository`, `RegistroAsistenciaRepository`
- Reglas:
  - bloque debe existir
  - ownership por profesor del bloque
  - debe existir asistencia para `(bloque,fecha)`
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.auth.LoginUsuario`

- Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/usecase/auth/LoginUsuario.java`
- Función: autenticar usuario y emitir JWT.
- Repositorios/dependencias:
  - `UsuarioRepository`
  - `PasswordEncoder`
  - `JwtTokenProvider`
- Validaciones:
  - usuario existe por `identificador` (email o RUT normalizado)
  - usuario activo
  - password BCrypt coincide
- Flujo `execute()`:
  1. resuelve usuario por `identificador`:
     - email (`findByEmail`) o
     - RUT (`RutNormalizer.normalize` + `findByRut`)
  2. valida `activo`
  3. valida password
  4. construye `UserPrincipal`
  5. genera token
  6. retorna `AuthResponse`
- Errores:
  - `BadCredentialsException` (credenciales inválidas / usuario desactivado)
- `@Transactional`: no.

### `com.schoolmate.api.usecase.apoderado.CrearApoderadoConUsuario`

- Función: crear apoderado + usuario `ROL=APODERADO` o vincular apoderado existente por RUT a un alumno.
- Repositorios/dependencias:
  - `ApoderadoRepository`, `ApoderadoAlumnoRepository`, `UsuarioRepository`, `AlumnoRepository`, `PasswordEncoder`
- Reglas:
  - alumno debe existir
  - MVP: un alumno no puede tener más de un apoderado vinculado
  - RUT se normaliza (`RutNormalizer`)
  - si el apoderado no existe por RUT, valida unicidad de email en `usuario` y `apoderado`
  - al crear usuario, password inicial = RUT normalizado con BCrypt
  - crea vínculo `apoderado_alumno`
- Errores:
  - `ResourceNotFoundException`
  - `ConflictException`
  - `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.apoderado.ObtenerAlumnosApoderado`

- Función: listar alumnos vinculados al apoderado autenticado.
- Repositorios/dependencias:
  - `ApoderadoAlumnoRepository`, `AlumnoRepository`, `MatriculaRepository`, `AnoEscolarRepository`, `ClockProvider`
- Reglas:
  - retorna lista vacía si no hay vínculos
  - solo incluye alumnos activos
  - si existe año escolar activo, enriquece con matrícula `ACTIVA` (`cursoId`, `cursoNombre`, `anoEscolarId`)
  - ordena por apellido/nombre
- `@Transactional`: no.

### `com.schoolmate.api.usecase.apoderado.ObtenerAsistenciaMensualAlumno`

- Función: obtener asistencia mensual agregada por día para un alumno del apoderado.
- Repositorios/dependencias:
  - `ApoderadoAlumnoRepository`, `RegistroAsistenciaRepository`, `AlumnoRepository`
- Reglas:
  - ownership obligatorio (`apoderadoId` -> `alumnoId`)
  - valida mes/año para construir rango de fechas
  - agrupa registros por fecha y calcula `PRESENTE`, `AUSENTE` o `PARCIAL`
  - mes sin registros retorna `dias=[]`
- Errores:
  - `AccessDeniedException`
  - `ResourceNotFoundException`
  - `BusinessException`
- `@Transactional`: no.

### `com.schoolmate.api.usecase.apoderado.ObtenerResumenAsistenciaAlumno`

- Función: calcular resumen de asistencia anual por bloques (`presentes`, `ausentes`, `% asistencia`).
- Repositorios/dependencias:
  - `ApoderadoAlumnoRepository`, `RegistroAsistenciaRepository`, `AlumnoRepository`
- Reglas:
  - ownership obligatorio (`apoderadoId` -> `alumnoId`)
  - conteo por estado (`PRESENTE`, `AUSENTE`) filtrado por `anoEscolarId`
  - porcentaje con 1 decimal sobre el total de bloques registrados
- Errores:
  - `AccessDeniedException`
  - `ResourceNotFoundException`
- `@Transactional`: no.

### `com.schoolmate.api.usecase.profesor.CrearProfesorConUsuario`

- Función: creación transaccional de profesor + usuario (`ROL=PROFESOR`) con password inicial igual al RUT normalizado.
- Repositorios/dependencias:
  - `ProfesorRepository`, `MateriaRepository`, `UsuarioRepository`, `PasswordEncoder`
- Reglas:
  - mantiene validaciones de duplicados de `Profesor` (rut/email/teléfono)
  - valida duplicados en `Usuario` por email y RUT normalizado
  - si falla creación de usuario, rollback completo de profesor
- `@Transactional`: sí.

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
  4. define fecha (`request.fechaMatricula` o `clockProvider.today()`)
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

### `com.schoolmate.api.usecase.matricula.ValidarAccesoMatriculasCursoProfesor`

- Función: validar ownership del endpoint `GET /api/matriculas/curso/{cursoId}` para rol `PROFESOR`.
- Repositorios/dependencias:
  - `ClockProvider`, `AnoEscolarRepository`, `BloqueHorarioRepository`
- Reglas:
  - `ADMIN` pasa directo
  - `PROFESOR` requiere `profesorId` en JWT
  - debe existir año escolar activo en `clockProvider.today()`
  - debe existir al menos un bloque activo del profesor en el curso y año activo
- Error:
  - `AccessDeniedException` (mapea a `ACCESS_DENIED`).

### `com.schoolmate.api.usecase.profesor.ObtenerClasesHoyProfesor`

- Función: obtener clases del día del profesor autenticado.
- Repositorios/dependencias:
  - `ClockProvider`, `AnoEscolarRepository`, `BloqueHorarioRepository`, `MatriculaRepository`, `AsistenciaClaseRepository`
- Reglas:
  - usa `profesorId` del JWT (`UserPrincipal`)
  - si hoy es sábado/domingo o no hay año activo: retorna lista vacía
  - consulta bloques `CLASE` activos del día y año activo
  - calcula estado temporal `PENDIENTE|DISPONIBLE|EXPIRADA` con ventana ±15 minutos
  - calcula `cantidadAlumnos` con matrículas `ACTIVA`
  - `asistenciaTomada` se calcula por existencia de `asistencia_clase` para `(bloqueId, fechaHoy)`
- `@Transactional(readOnly = true)`: sí.

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

### `com.schoolmate.api.usecase.jornada.ObtenerMateriasDisponibles`

- Función: calcular materias disponibles para un bloque CLASE según malla y minutos ya asignados.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`, `MallaCurricularRepository`
- Reglas:
  - bloque debe pertenecer al curso y estar activo
  - bloque debe ser de tipo `CLASE`
  - minutos permitidos por materia: `horasPedagogicas * 45`
  - minutos asignados excluyen el bloque actual
- `@Transactional`: no.

### `com.schoolmate.api.usecase.jornada.AsignarMateriaBloque`

- Función: asignar/reemplazar materia en bloque CLASE validando malla y tope de minutos.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`, `MateriaRepository`, `MallaCurricularRepository`
- Reglas:
  - curso debe existir y no estar `CERRADO`
  - bloque activo y perteneciente al curso
  - bloque tipo `CLASE`
  - materia debe existir y pertenecer a malla activa del grado/año del curso
  - `minutosAsignados + duracionBloque <= minutosPermitidos`
  - no-op si bloque ya tiene la misma materia
  - al reemplazar materia, si el profesor asignado no enseña la nueva materia, se limpia `profesor`
- Errores relevantes:
  - `MATERIA_NO_EN_MALLA`
  - `MATERIA_EXCEDE_MINUTOS_MALLA` (409 con `details`)
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.jornada.QuitarMateriaBloque`

- Función: quitar materia de un bloque CLASE y limpiar profesor.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`
- Reglas:
  - curso no `CERRADO`
  - bloque activo y del curso
  - bloque tipo `CLASE`
  - bloque debe tener materia asignada
- Errores relevantes:
  - `BLOQUE_NO_ES_CLASE`
  - `BLOQUE_SIN_MATERIA`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.jornada.ObtenerResumenAsignacionMaterias`

- Función: resumir avance de asignación por materia de malla (`COMPLETA`, `PARCIAL`, `SIN_ASIGNAR`).
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`, `MallaCurricularRepository`
- Cálculo:
  - agrega minutos y bloques de tipo `CLASE` por materia
  - compara contra `horasPedagogicas * 45`
- `@Transactional`: no.

### `com.schoolmate.api.usecase.jornada.ObtenerProfesoresDisponibles`

- Función: listar profesores habilitados para la materia del bloque y marcar disponibilidad por colisión horaria.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`, `ProfesorRepository`
- Reglas:
  - bloque activo, del curso y de tipo `CLASE`
  - el bloque debe tener `materia` asignada
  - profesores filtrados por materia (`profesor_materia`) y `activo=true`
  - colisión evaluada cross-curso dentro del mismo año escolar
- `@Transactional`: no.

### `com.schoolmate.api.usecase.jornada.AsignarProfesorBloque`

- Función: asignar/reemplazar profesor en bloque `CLASE`.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`, `ProfesorRepository`
- Reglas:
  - curso no `CERRADO`
  - bloque activo y del curso
  - bloque tipo `CLASE`
  - bloque con materia previa
  - profesor activo y habilitado para la materia del bloque
  - sin colisión de horario en el mismo `anoEscolar`
  - no-op si ya tiene el mismo profesor
- Errores relevantes:
  - `BLOQUE_SIN_MATERIA_PARA_PROFESOR`
  - `PROFESOR_NO_ENSENA_MATERIA` (409 con `details`)
  - `PROFESOR_COLISION_HORARIO` (409 con `details`)
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.jornada.QuitarProfesorBloque`

- Función: quitar profesor del bloque manteniendo la materia.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`
- Reglas:
  - curso no `CERRADO`
  - bloque activo y del curso
  - bloque tipo `CLASE`
  - bloque debe tener profesor asignado
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.jornada.ObtenerResumenAsignacionProfesores`

- Función: resumir asignación de docentes en bloques `CLASE` del curso.
- Repositorios:
  - `BloqueHorarioRepository`, `CursoRepository`
- Cálculo:
  - total de bloques con/sin profesor
  - bloques con materia sin profesor
  - agrupación por profesor con minutos y bloques asignados
- `@Transactional`: no.

---

## SECCIÓN 9: REPOSITORIOS

| Repositorio | Entidad | Métodos derivados destacados | `@Query` custom | Specifications |
|---|---|---|---|---|
| `AlumnoRepository` | `Alumno` | `existsByRut`, `existsByRutAndIdNot` | `findActivoByRutNormalizado` (native SQL con `regexp_replace`) | sí, vía `JpaSpecificationExecutor` |
| `AnoEscolarRepository` | `AnoEscolar` | `findAllByOrderByAnoDesc`, `findByAno`, `existsByAno`, `findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual`, `findActivoByFecha` (default) | no | no |
| `AsistenciaClaseRepository` | `AsistenciaClase` | `findByBloqueHorarioIdAndFecha`, `existsByBloqueHorarioIdAndFecha` | no | no |
| `BloqueHorarioRepository` | `BloqueHorario` | `findByCursoIdAndActivoTrueOrderByDiaSemanaAscNumeroBloqueAsc`, `findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc`, `findByCursoIdAndActivoTrueAndTipo`, `findByCursoIdAndActivoTrueAndTipoAndMateriaId` | `desactivarBloquesDia`, `findDiasConfigurados`, `findColisionesProfesor`, `findHorarioProfesorEnAnoEscolar`, `findBloquesClaseProfesoresEnAnoEscolar`, `findClasesProfesorEnDia`, `existsBloqueActivoProfesorEnCurso` | no |
| `CursoRepository` | `Curso` | `findByAnoEscolarIdOrderByNombreAsc`, `findByAnoEscolarIdAndGradoIdOrderByLetraAsc`, `findByActivoTrueAndAnoEscolarIdOrderByNombreAsc` | `findLetrasUsadasByGradoIdAndAnoEscolarId` | no |
| `GradoRepository` | `Grado` | `findAllByOrderByNivelAsc` | no | no |
| `MallaCurricularRepository` | `MallaCurricular` | múltiples `findBy...` y `existsBy...` combinando materia/grado/año/activo | no | no |
| `MateriaRepository` | `Materia` | `findAllByOrderByNombreAsc`, `existsByNombre` | no | no |
| `MatriculaRepository` | `Matricula` | `findByAlumnoId`, `findByCursoIdAndEstado`, `findByAlumnoIdAndAnoEscolarIdAndEstado`, `existsByAlumnoIdAndAnoEscolarIdAndEstado`, `countByCursoIdAndEstado`, etc. | `countActivasByCursoIds` | no |
| `ProfesorRepository` | `Profesor` | unicidad por rut/email/teléfono + listas ordenadas, `findByActivoTrueAndMaterias_Id` | no | no |
| `ApoderadoRepository` | `Apoderado` | `findByEmail`, `findByRut`, `existsByEmail`, `existsByRut` | no | no |
| `ApoderadoAlumnoRepository` | `ApoderadoAlumno` | `existsByIdApoderadoIdAndIdAlumnoId`, `findByIdApoderadoId`, `findByIdAlumnoId`, wrappers `findByApoderadoId/findByAlumnoId`, `existsByAlumnoId`, `existsByApoderadoIdAndAlumnoId` | no | no |
| `RegistroAsistenciaRepository` | `RegistroAsistencia` | `findByAsistenciaClaseId` (con `@EntityGraph alumno`), `deleteByAsistenciaClaseId`, `findByAlumnoIdAndFechaEntre`, `countByAlumnoIdAndEstadoAndAnoEscolarId` | `DELETE` por `asistenciaClase.id`; JPQL para mensual por fecha y resumen por año escolar vía joins `asistencia_clase -> bloque_horario -> curso` | no |
| `SeccionCatalogoRepository` | `SeccionCatalogo` | `findByActivoTrueOrderByOrdenAsc` | no | no |
| `UsuarioRepository` | `Usuario` | `findByEmail`, `findByRut`, `findByApoderadoId`, `existsByEmail`, `existsByRut`, `existsByProfesorId` | no | no |

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
| `LoginRequest` | `identificador`, `password` | `@NotBlank` en ambos (sin `@Email` para soportar RUT) | `@Data` |
| `AnoEscolarRequest` | `ano`, `fechaInicioPlanificacion`, `fechaInicio`, `fechaFin` | `@NotNull` | `@Data` |
| `MateriaRequest` | `nombre`, `icono` | `@NotBlank(nombre)` | `@Data` |
| `ProfesorRequest` | `rut,nombre,apellido,email,telefono,fechaContratacion,horasPedagogicasContrato?,materiaIds` | `@NotBlank`, `@Size`, `@Email`, `@NotNull`, `@NotEmpty`, `@Min(1)`, `@Max(50)` (opcional) | `@Data` |
| `CursoRequest` | `gradoId`, `anoEscolarId` | `@NotBlank` | `@Data` |
| `AlumnoRequest` | `rut,nombre,apellido,fechaNacimiento,apoderadoNombre,apoderadoApellido,apoderadoEmail,apoderadoTelefono,apoderadoVinculo` | `@NotBlank`, `@Size`, `@Email` | `@Data @Builder` |
| `MatriculaRequest` | `alumnoId,cursoId,anoEscolarId,fechaMatricula?` | `@NotBlank` (except fecha) | `@Data @Builder` |
| `MallaCurricularRequest` | `materiaId,gradoId,anoEscolarId,horasPedagogicas` | `@NotBlank`, `@NotNull`, `@Min(1)`, `@Max(15)` | `@Data` |
| `MallaCurricularBulkRequest` | `materiaId,anoEscolarId,grados[]` | `@NotBlank`, `@NotEmpty`, `@Valid` | `@Data` |
| `MallaCurricularBulkRequest.GradoHoras` | `gradoId,horasPedagogicas` | `@NotBlank`, `@NotNull`, `@Min(1)`, `@Max(15)` | `@Data` |
| `BloqueRequest` | `numeroBloque,horaInicio,horaFin,tipo` | `@NotNull`, `@Min(1)`, `@Pattern` para hora `HH:mm` y tipo | `@Getter/@Setter` |
| `JornadaDiaRequest` | `bloques[]` | `@NotNull`, `@Size(min=1)`, `@Valid` | `@Getter/@Setter` |
| `CopiarJornadaRequest` | `diasDestino[]` | `@NotNull`, `@Size(min=1)`, elementos `@Min(1) @Max(5)` | `@Getter/@Setter` |
| `AsignarMateriaRequest` | `materiaId` | `@NotBlank` | `@Data` |
| `AsignarProfesorRequest` | `profesorId` | `@NotBlank` | `@Data` |
| `GuardarAsistenciaRequest` | `bloqueHorarioId,fecha,registros[]` | `@NotBlank`, `@NotNull`, `@Size(min=1)`, `@Valid` | `@Data` |
| `RegistroAlumnoRequest` | `alumnoId,estado` | `@NotBlank`, `@NotNull` | `@Data` |

### DTOs de Portal/Apoderado (`com.schoolmate.api.dto`)

| DTO | Campos principales | Builder/Lombok |
|---|---|---|
| `ApoderadoRequest` | `nombre,apellido,rut,email,telefono,alumnoId` | `@Getter/@Setter` + validaciones `@NotBlank/@Email` |
| `ApoderadoBuscarResponse` | `id,nombre,apellido,rut,email,telefono,existe` | `@Getter/@Setter @Builder` |
| `ApoderadoResponse` | `id,nombre,apellido,rut,email,telefono,usuarioId,cuentaActiva,alumnos[]` | `@Getter/@Setter @Builder` |
| `ApoderadoResponse.AlumnoResumen` | `id,nombre,apellido` | `@Getter/@Setter @Builder` |
| `AlumnoApoderadoResponse` | `id,nombre,apellido,cursoId,cursoNombre,anoEscolarId` | `@Getter/@Setter @Builder` |
| `AsistenciaMensualResponse` | `alumnoId,alumnoNombre,mes,anio,dias[]` | `@Getter/@Setter @Builder` |
| `AsistenciaDiaResponse` | `fecha,totalBloques,bloquesPresente,bloquesAusente,estado` | `@Getter/@Setter @Builder` |
| `ResumenAsistenciaResponse` | `alumnoId,alumnoNombre,totalClases,totalPresente,totalAusente,porcentajeAsistencia` | `@Getter/@Setter @Builder` |
| `RegistroConFecha` | `registroId,alumnoId,estado,fecha` (proyección interna para query) | `@Getter/@Setter` |

### Response DTOs

| DTO | Campos principales | Builder/Lombok |
|---|---|---|
| `AuthResponse` | `token,tipo,id,email,nombre,apellido,rol,profesorId,apoderadoId` | `@Data @Builder` |
| `ApiErrorResponse` | `code,message,status,field,path,timestamp,details` | `@Data @Builder` |
| `AnoEscolarResponse` | `id,ano,fechaInicioPlanificacion,fechaInicio,fechaFin,estado,createdAt,updatedAt` | `@Data @Builder` |
| `MateriaResponse` | `id,nombre,icono,createdAt,updatedAt` | `@Data @Builder` |
| `MateriaPageResponse` | `content,page,size,totalElements,totalPages,sortBy,sortDir,hasNext,hasPrevious` | `@Data @Builder` |
| `ProfesorResponse` | `id,rut,nombre,apellido,email,telefono,fechaContratacion,horasPedagogicasContrato,horasAsignadas,activo,materias,createdAt,updatedAt` | `@Data @Builder` |
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
| `MateriaDisponibleResponse` | `materiaId,materiaNombre,materiaIcono,horasPedagogicas,minutosSemanalesPermitidos,minutosAsignados,minutosDisponibles,asignable,asignadaEnEsteBloque` | `@Data @Builder` |
| `MateriasDisponiblesResponse` | `bloqueId,bloqueDuracionMinutos,materias[]` | `@Data @Builder` |
| `AsignacionMateriaResumenResponse` | resumen curso + `materias[]` + bloques asignados por materia | `@Data @Builder` |
| `ConflictoHorarioResponse` | `cursoNombre,materiaNombre,horaInicio,horaFin,bloqueId` | `@Data @Builder` |
| `EstadoClaseHoy` | `PENDIENTE,DISPONIBLE,EXPIRADA` | `enum` |
| `ClaseHoyResponse` | `bloqueId,numeroBloque,horaInicio,horaFin,cursoId,cursoNombre,materiaId,materiaNombre,materiaIcono,cantidadAlumnos,estado,asistenciaTomada` | `@Data @Builder` |
| `ClasesHoyResponse` | `fecha,diaSemana,nombreDia,clases[]` | `@Data @Builder` |
| `AsistenciaClaseResponse` | `asistenciaClaseId,bloqueHorarioId,fecha,tomadaEn,registros[]` | `@Data @Builder` |
| `RegistroAsistenciaResponse` | `alumnoId,alumnoNombre,alumnoApellido,estado` | `@Data @Builder` |
| `ProfesorDisponibleResponse` | `profesorId,profesorNombre,profesorApellido,horasPedagogicasContrato,horasAsignadas,excedido,disponible,asignadoEnEsteBloque,conflicto` | `@Data @Builder` |
| `ProfesoresDisponiblesResponse` | bloque + materia + `profesores[]` | `@Data @Builder` |
| `ProfesorHorarioResponse` | horario consolidado por año (`resumenSemanal`, `dias[]`, bloques con curso/materia) | `@Data @Builder` |
| `BloqueProfesorResumenResponse` | `bloqueId,diaSemana,numeroBloque,horaInicio,horaFin,materiaNombre` | `@Data @Builder` |
| `ProfesorResumenAsignacionResponse` | profesor + materias + minutos + bloques | `@Data @Builder` |
| `BloquePendienteProfesorResponse` | bloque con materia pendiente de profesor | `@Data @Builder` |
| `AsignacionProfesoresResumenResponse` | resumen curso + profesores + bloques pendientes | `@Data @Builder` |

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
- `horasPedagogicasContrato` opcional en profesor:
  - si viene valor, rango válido `1..50`
  - en update puede enviarse `null` para limpiar
- `GET /api/profesores/{id}` calcula `horasAsignadas` en año escolar `ACTIVO`:
  - suma minutos de bloques `CLASE` asignados al profesor
  - convierte a horas pedagógicas con `ceil(minutos / 45.0)`
  - si no existe año `ACTIVO`, retorna `horasAsignadas=null`

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
  - la asignación de materia solo aplica a bloques `CLASE`
  - materia debe pertenecer a malla activa del grado/año
  - tope por materia: `horasPedagogicas * 45` minutos semanales
  - quitar materia limpia también `profesor_id`
  - asignación de profesor solo en bloques `CLASE` con materia
  - profesor debe enseñar la materia del bloque (`profesor_materia`)
  - profesor no puede tener colisión de horario (cross-curso, mismo año escolar)
- Transiciones de matrícula restringidas (`ACTIVA<->RETIRADO/TRASLADADO`).
- Gestión de apoderados (MVP):
  - RUT de apoderado normalizado antes de crear/vincular.
  - creación de usuario `ROL=APODERADO` con password inicial = RUT normalizado (BCrypt).
  - máximo un apoderado por alumno en flujo admin actual (`existsByAlumnoId`).
  - si el alumno ya tiene vínculo, responde conflicto (`409`).
- Portal apoderado:
  - ownership estricto por vínculo `apoderado_alumno`.
  - asistencia mensual marca día `PARCIAL` cuando hay bloques presentes y ausentes en la misma fecha.
  - porcentaje de asistencia se calcula sobre bloques con registro, no sobre bloques teóricos.
- Jornada para apoderado:
  - `GET /api/cursos/{cursoId}/jornada` solo permitido si existe matrícula `ACTIVA` de algún alumno del apoderado en ese curso.

---

## SECCIÓN 12: CONTRATOS CLAVE PARA FRONTEND

### Respuestas enriquecidas

- `GET /api/cursos/{id}` retorna:
  - datos del curso
  - `alumnosMatriculados`
  - `cantidadMaterias`
  - `totalHorasPedagogicas`
  - `materias[]` con carga horaria por materia
- `GET /api/cursos/{cursoId}/jornada/materias-disponibles` retorna por materia:
  - minutos permitidos/asignados/disponibles
  - flag `asignable` para el bloque consultado
- `GET /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesores-disponibles` retorna:
  - disponibilidad por profesor
  - detalle de conflicto horario cuando existe
  - enriquecimiento de carga docente: `horasPedagogicasContrato`, `horasAsignadas`, `excedido` (informativo, no bloqueante)
- `GET /api/profesores/{profesorId}/horario?anoEscolarId=...` retorna:
  - horario consolidado agrupado por día y ordenado por hora
  - resumen semanal (`totalBloques`, `diasConClase`)
  - `horasAsignadas` calculadas sobre el año consultado
- `GET /api/apoderado/mis-alumnos` retorna:
  - hijos vinculados al apoderado autenticado
  - `cursoId/cursoNombre/anoEscolarId` cuando existe matrícula `ACTIVA` en año activo
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/mensual` retorna:
  - lista `dias[]` solo con fechas que tienen registros
  - por día: `totalBloques`, `bloquesPresente`, `bloquesAusente`, `estado`
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/resumen` retorna:
  - `totalClases`, `totalPresente`, `totalAusente`, `porcentajeAsistencia`

- `GET /api/alumnos` y `GET /api/alumnos/{id}`:
  - si se envía `anoEscolarId`, agrega datos de matrícula activa (`curso`, `grado`, `estado`, `fechaMatricula`).
- `GET /api/alumnos/buscar-por-rut?rut=...`:
  - endpoint dedicado para búsqueda exacta por RUT en frontend
  - acepta RUT con o sin formato (`9.057.419-9`, `9057419-9`, `90574199`)
  - puede enriquecer con matrícula si se envía `anoEscolarId`.

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
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/mensual`:
  - `mes`, `anio`
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/resumen`:
  - `anoEscolarId`

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
| Apoderados (admin) | ✅ | create/vincular, buscar por RUT, obtener por alumno |
| Portal Apoderado | ✅ parcial | mis-alumnos, asistencia mensual, resumen asistencia |
| Alumnos | ✅ | list/get/create/update |
| Matrículas | ✅ | create, list por curso, historial por alumno, cambio estado |
| Jornada escolar por curso | ✅ | guardar día, obtener jornada/resumen (admin + apoderado con ownership en GET), copiar día, eliminar día, asignar/quitar materia y profesor por bloque, resúmenes de asignación |
| Ownership por rol no-admin | ⚠️ parcial | profesor (horario, asistencia, matrículas por curso) + apoderado (portal y lectura de jornada por curso) |
| Asistencia | ✅ parcial | registro/consulta por profesor + consulta mensual/resumen por apoderado |
| Reportes | ❌ | no existe |
| Dashboards | ❌ | no existe |

### Qué falta para que frontend deje DataContext completamente

- Completar ownership en backend para más dominios (`principal.profesorId/apoderadoId`) fuera de los ya cubiertos.
- Extender capacidades de `APODERADO` a más vistas académicas (más allá de asistencia y jornada).
- Endpoints de agregación operacional (dashboard, indicadores) si frontend hoy los calcula localmente.
- Completar módulos pendientes (reportes/dashboards) que normalmente DataContext simula.

### Próximos módulos lógicos

1. Reportes (académico, matrícula, carga docente).
2. Dashboards (KPIs por año/curso/docente).
3. Asistencia avanzada (justificaciones, alertas, historiales comparativos).

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

6. Reglas de ownership implementadas de forma parcial:
- ya existe ownership para `PROFESOR` (horario/asistencia/matrículas por curso) y `APODERADO` (portal + lectura de jornada por curso), pero faltan más dominios con permisos finos por rol.

7. Secretos hardcodeados:
- datasource password y `jwt.secret` están en YAML versionado.

### Impacto operativo

- Riesgo de errores 500 evitables en `/api/auth/me`.
- Riesgo de fallas de arranque en entornos nuevos por desalineación de esquema.
- Riesgo de seguridad por exposición de secretos en repositorio.
