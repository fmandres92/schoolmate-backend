# SchoolMate Hub API - Backend Documentation (Rebuilt from Source)

Last update: 2026-02-25  
Source of truth: Java code in `src/main/java`, Spring config in `src/main/resources`, and Flyway migrations in `src/main/resources/db/migration`.

---

## 1) Purpose of this document

This document is a full technical map of the current backend implementation.
It is designed to be used by:
- engineers onboarding to the project,
- reviewers doing architecture audits,
- AI agents that need exact operational context.

Goal: describe what the system **really does today**, not what was planned.

---

## 2) Project snapshot (current state)

- Application: `schoolmate-hub-api`
- Main class: `com.schoolmate.api.SchoolmateApiApplication`
- Base package: `com.schoolmate.api`
- Style: Spring MVC + Use Cases + JPA repositories
- Profiles: `dev` (default), `prod`

Current inventory:
- Controllers: 19
- Use cases: 73
- Entities: 19
- Repositories: 18
- Request DTOs (`dto/request`): 22
- Response DTOs (`dto/response`): 53
- Projection DTOs (`dto/projection`): 1
- Additional DTOs in `dto/`: 0
- Flyway migrations in repo: 22 (`V1` to `V23`, with gaps intentionally not present)

---

## 3) Tech stack and dependencies

## Runtime and framework
- Java 21
- Spring Boot 4.0.2
- Spring Framework 7.0.x (via Boot)
- Spring Security (method security enabled)
- Spring Data JPA
- Hibernate ORM (via Boot)
- PostgreSQL driver
- Flyway + Flyway PostgreSQL extension
- JJWT 0.12.6
- Lombok 1.18.36

## Build
- Maven
- `maven-compiler-plugin` with `source/target=21`

## Test stack
- `spring-boot-starter-test`
- `spring-security-test`
- H2 runtime for tests
- JUnit 5 + Mockito in unit tests

---

## 4) Runtime configuration

## 4.1 Base config (`application.yml`)
- `spring.application.name=schoolmate-hub-api`
- `spring.profiles.active=dev`
- `spring.jpa.open-in-view=false` (OSIV disabled)
- `server.port=8080`

## 4.2 Dev config (`application-dev.yml`)
- DataSource points to Supabase PostgreSQL
- JPA: `ddl-auto=validate`, `show-sql=true`, `format_sql=true`
- Flyway enabled + validate-on-migrate + baseline-on-migrate
- Security logging enabled (`org.springframework.security=DEBUG`)
- JWT secret/expiration configured

## 4.3 Prod config (`application-prod.yml`)
- DataSource also points to Supabase PostgreSQL
- JPA: `ddl-auto=validate`, `show-sql=false`
- Flyway enabled
- JWT secret/expiration configured

## 4.4 Important operational note
`application-dev.yml` and `application-prod.yml` currently contain explicit credentials/secrets in plain text. This is an active risk and should be moved to environment variables or secret manager.

---

## 5) Architecture actually implemented

The backend follows a pragmatic layered approach:
- Controller layer: HTTP contract, auth annotations, parameter binding.
- Use case layer: business orchestration and transactional boundaries.
- Repository layer: DB access and custom queries.
- Entity layer: persistence model + selected domain behavior methods.

Pattern currently used:
- Most mutations (`POST/PUT/PATCH/DELETE`) route through use cases.
- Most reads also route through use cases (including paginated reads).
- Some domain logic has been moved from use cases to entities (anti-pattern #11 cleanup).

No generic service layer (`*Service`) is used for business orchestration.

---

## 6) Package map

- `common/rut`: RUT normalization and cross-person validation
- `common/time`: central time provider + overridable time context (dev tooling)
- `config`: security/web/cache/interceptors/aspect
- `controller`: all REST API endpoints
- `dto/request`, `dto/response`: API contracts
- `dto/projection`: DTOs internos para proyecciones de repositorio (no contratos HTTP)
- `entity`: JPA entities
- `enums`: domain and security enums
- `exception`: error model and global exception handling
- `repository`: JPA repositories and custom JPQL
- `security`: JWT, principals, handlers, auth filter
- `specification`: `AlumnoSpecifications`
- `usecase/*`: business orchestration by domain

---

## 7) Cross-cutting behavior

## 7.1 Time model
- `ClockProvider.now()` and `ClockProvider.today()` are the official time source.
- Backed by `TimeContext` static clock.
- Dev profile supports override/reset via `DevToolsController`.

Implication:
- Temporal rules (attendance windows, school-year state, etc.) are testable and controllable in dev.

## 7.2 Security model
- Stateless JWT authentication.
- Filter chain:
  - CSRF disabled
  - CORS enabled
  - Session policy stateless
  - JWT filter before `UsernamePasswordAuthenticationFilter`
- PermitAll routes:
  - `/api/auth/login`
  - `/api/auth/refresh`
  - `/h2-console/**`
  - `/error`
- All other routes require authentication.
- Role gates via `@PreAuthorize` at class and/or method level.

## 7.3 JWT claims model
`JwtTokenProvider` stores and restores:
- `id` (user UUID)
- `rol`
- `profesorId`
- `apoderadoId`
- `nombre`
- `apellido`
- subject = email

## 7.4 Error handling
- `GlobalExceptionHandler` standardizes API errors as `ApiErrorResponse`.
- Uses `ErrorCode` enum + `messages_es.properties` localization keys.
- Dedicated handlers for:
  - `BadCredentialsException`
  - `ResourceNotFoundException`
  - `BusinessException`
  - `ConflictException`
  - `ApiException`
  - `MethodArgumentNotValidException`
  - `AccessDeniedException`
  - `DataIntegrityViolationException`
  - fallback `Exception`

## 7.5 Security-specific error writers
- `ApiAuthenticationEntryPoint` -> `UNAUTHORIZED`
- `ApiAccessDeniedHandler` -> `ACCESS_DENIED`
- `SecurityErrorResponseWriter` writes JSON body for security errors.

## 7.6 Header `X-Ano-Escolar-Id`
- Interceptor: `AnoEscolarHeaderInterceptor`
- Resolver: `AnoEscolarArgumentResolver` for `@AnoEscolarActivo`
- Behavior:
  - The header is resolved in controllers through `@AnoEscolarActivo`; use cases receive `UUID anoEscolarId` already resolved.
  - If header missing: request continues only for endpoints marked with `@AnoEscolarActivo(required = false)`.
  - If header invalid UUID: `VALIDATION_FAILED`.
  - If year not found: `ResourceNotFoundException`.
  - If principal is PROFESOR/APODERADO and selected year is not ACTIVO: `ACCESS_DENIED`.
  - For write/list flows scoped by school year (`cursos`, `alumnos`, `malla`, `matriculas`, `profesor/apoderado portal`, `dashboard`, `dias-no-lectivos`), header is required.

## 7.7 HTTP cache behavior
By `CacheControlInterceptor` (GET + HTTP 200 only):

- `no-store, private`:
  - `/api/apoderado/**`
  - `/api/profesor/**`
  - `/api/auth/**`
  - `/api/auditoria/**`
  - `/api/profesores/{id}/sesiones`
  - `/api/profesores/{id}/cumplimiento-asistencia`

- `no-store`:
  - `/api/matriculas/**`
  - `/api/asistencia/**`
  - `/api/alumnos/**`

- `max-age=2min`:
  - `/api/dias-no-lectivos/**`

- `no-cache` (default for most catalog/config paths):
  - `/api/grados`
  - `/api/materias`
  - `/api/anos-escolares/**`
  - `/api/cursos/**`
  - `/api/malla-curricular`
  - `/api/profesores`

Also present:
- `ShallowEtagHeaderFilter` on `/api/*` for successful GET responses.

## 7.8 Automatic audit trail (AOP)
`AuditoriaAspect` captures successful mutations (`POST/PUT/PATCH/DELETE`) in `@RestController` classes:
- Excludes URIs starting with `/api/auth/` and `/api/dev/`.
- Persists `EventoAuditoria` in a separate transaction (`REQUIRES_NEW`).
- Captures:
  - authenticated user id/email/role snapshot
  - HTTP method, endpoint
  - request body (`@RequestBody` serialized when possible)
  - response status
  - IP (`X-Forwarded-For` -> `X-Real-IP` -> `remoteAddr`)
  - optional `X-Ano-Escolar-Id`
  - timestamp (`ClockProvider.now()`)

Note: aspect catches specific runtime persistence/serialization/context errors and does not block business response.

---

## 8) Domain model (entities)

## 8.1 Entity inventory
- `Usuario`
- `Apoderado`
- `ApoderadoAlumno` (+ `ApoderadoAlumnoId`)
- `AnoEscolar`
- `DiaNoLectivo`
- `Grado`
- `Materia`
- `Profesor`
- `Curso`
- `SeccionCatalogo`
- `Alumno`
- `Matricula`
- `MallaCurricular`
- `BloqueHorario`
- `AsistenciaClase`
- `RegistroAsistencia`
- `SesionUsuario`
- `EventoAuditoria`

## 8.2 Entities with explicit domain behavior (important)

- `Alumno`
  - `actualizarDatosPersonales(...)`

- `AnoEscolar`
  - `calcularEstado(LocalDate fechaReferencia)`
  - `actualizarConfiguracion(...)`

- `Curso`
  - `actualizarIdentidadAcademica(Grado, AnoEscolar, letra)`
  - internal name formatting

- `Matricula`
  - `cambiarEstado(EstadoMatricula nuevoEstado)`

- `Profesor`
  - `getMaterias()` returns unmodifiable list
  - `actualizarPerfil(...)`

- `Materia`
  - `actualizarDatos(nombre, icono)`

- `MallaCurricular`
  - `actualizarConfiguracion(...)`
  - `activarConHoras(...)`
  - `desactivar()`

- `BloqueHorario`
  - `asignarMateria(...)`
  - `limpiarProfesorSiNoEnsenaMateria()`
  - `quitarMateriaYProfesor()`
  - `asignarProfesor(...)`
  - `quitarProfesor()`

- `AsistenciaClase`
  - `getRegistros()` returns unmodifiable list
  - `removeRegistrosIf(...)`
  - `addRegistro(...)`
  - `marcarRegistradaPor(...)`

- `RegistroAsistencia`
  - `actualizarRegistro(...)`

- `Usuario`
  - `actualizarRefreshToken(...)`

## 8.3 Core relationship highlights

- `Curso` -> `Grado` (ManyToOne LAZY)
- `Curso` -> `AnoEscolar` (ManyToOne LAZY)
- `Matricula` -> `Alumno`, `Curso`, `AnoEscolar`
- `MallaCurricular` -> `Materia`, `Grado`, `AnoEscolar`
- `BloqueHorario` -> `Curso`, optional `Materia`, optional `Profesor`
- `AsistenciaClase` -> `BloqueHorario`, optional `Usuario registradoPor`
- `AsistenciaClase` -> `RegistroAsistencia` (OneToMany cascade all + orphanRemoval)
- `RegistroAsistencia` -> `Alumno`
- `Profesor` <-> `Materia` (ManyToMany via `profesor_materia`)
- `SesionUsuario` -> `Usuario`
- `EventoAuditoria` -> `Usuario`
- `ApoderadoAlumno` is link entity between `Apoderado` and `Alumno`.

---

## 9) Database and migrations

## 9.1 Migration files in repository

Ordered by version:
- V1 create usuario
- V2 seed usuarios
- V3 create catalogo base
- V4 seed catalogo base
- V5 create profesores/cursos
- V6 seed profesores/cursos
- V7 marker (alumno created directly in Supabase)
- V8 marker (alumno seed directly in Supabase)
- V9 create malla curricular
- V10 seccion catalogo + uniqueness for curso letra
- V11 matricula refactor
- V12 asignacion (legacy table)
- V13 rename horas_semanales -> horas_pedagogicas
- V14 add `profesor.horas_pedagogicas_contrato`
- V15 add `usuario.rut` + backfill script
- V16 asistencia tables
- V17 marker (apoderado/apoderado_alumno done directly in Supabase)
- V18 marker (REQ-04 apoderado-alumno updates)
- V19 add `usuario.refresh_token`
- V21 marker (sesion_usuario + trazabilidad asistencia)
- V22 marker (evento_auditoria)
- V23 consolidation/baseline idempotent script for schema drift

## 9.2 Important migration reality
- Some migrations are marker-only by design (executed directly in Supabase).
- V23 exists to close drift and make local empty DB bootstrap viable with idempotent DDL.

## 9.3 Live schema snapshot artifacts
The repo includes:
- `db/schema.sql`
- `db/schema_inventory.md`

These are generated from live catalog and include Supabase-managed schemas (`auth`, `storage`, etc.) in addition to the app's `public` schema.

---

## 10) API endpoints (exact inventory)

## 10.1 Auth

- `POST /api/auth/login`  
  - Public  
  - Body: `LoginRequest`  
  - Returns: `AuthResponse` (200)

- `POST /api/auth/refresh`  
  - Public  
  - Body: `RefreshTokenRequest`  
  - Returns: `AuthResponse` (200)

- `GET /api/auth/me`  
  - Authenticated  
  - Returns: profile map (200)

## 10.2 Auditoria

- `GET /api/auditoria`  
  - Role: ADMIN  
  - Query: `usuarioId, metodoHttp, endpoint, desde, hasta, page, size`  
  - Returns: `EventoAuditoriaPageResponse` (200)

## 10.3 Dashboard admin

- `GET /api/dashboard/admin/resumen`  
  - Role: ADMIN  
  - Header required: `X-Ano-Escolar-Id`  
  - Returns: `DashboardAdminResponse` (200)

## 10.4 Anos escolares

- `GET /api/anos-escolares` (ADMIN) -> `AnoEscolarPageResponse`
- `GET /api/anos-escolares/{id}` (ADMIN) -> `AnoEscolarResponse`
- `GET /api/anos-escolares/activo` (authenticated) -> `AnoEscolarResponse`
- `POST /api/anos-escolares` (ADMIN) -> `AnoEscolarResponse` (201)
- `PUT /api/anos-escolares/{id}` (ADMIN) -> `AnoEscolarResponse` (200)

## 10.5 Dias no lectivos

- `GET /api/dias-no-lectivos`  
  - Authenticated  
  - Header required: `X-Ano-Escolar-Id`  
  - Query optional: `mes, anio, page, size`  
  - Returns: `DiaNoLectivoPageResponse` (200)

- `POST /api/dias-no-lectivos`  
  - ADMIN  
  - Header required: `X-Ano-Escolar-Id`  
  - Body: `CrearDiaNoLectivoRequest`  
  - Returns: `List<DiaNoLectivoResponse>` (201)

- `DELETE /api/dias-no-lectivos/{id}`  
  - ADMIN  
  - Returns: 204

## 10.6 Grados

- `GET /api/grados` (ADMIN) -> `GradoPageResponse`
- `GET /api/grados/{id}` (ADMIN) -> `GradoResponse`

## 10.7 Materias

- `GET /api/materias` (ADMIN) -> `MateriaPageResponse`
- `GET /api/materias/{id}` (ADMIN) -> `MateriaResponse`
- `POST /api/materias` (ADMIN) -> `MateriaResponse` (201)
- `PUT /api/materias/{id}` (ADMIN) -> `MateriaResponse`
- `DELETE /api/materias/{id}` (ADMIN) -> 204

## 10.8 Malla curricular

All ADMIN (class-level)

- `GET /api/malla-curricular` (header required)
- `GET /api/malla-curricular/materia/{materiaId}` (header required)
- `GET /api/malla-curricular/grado/{gradoId}` (header required)
- `POST /api/malla-curricular` (201, header required)
- `PUT /api/malla-curricular/{id}`
- `POST /api/malla-curricular/bulk` (header required)
- `DELETE /api/malla-curricular/{id}` (204)

## 10.9 Cursos

All ADMIN (class-level)

- `GET /api/cursos` (header required)
- `GET /api/cursos/{id}`
- `POST /api/cursos` (201, header required)
- `PUT /api/cursos/{id}` (header required)

## 10.10 Profesores

`ProfesorController` (ADMIN class-level):
- `GET /api/profesores`
- `GET /api/profesores/{id}` (header optional; enriches `horasAsignadas`)
- `POST /api/profesores` (201)
- `PUT /api/profesores/{id}`
- `GET /api/profesores/{profesorId}/sesiones`
- `GET /api/profesores/{profesorId}/cumplimiento-asistencia` (header required; `fecha` opcional, default=today)

`ProfesorHorarioController`:
- `GET /api/profesores/{profesorId}/horario` (ADMIN or PROFESOR, header required)

`ProfesorMeController`:
- `GET /api/profesor/mis-clases-hoy` (PROFESOR, header required)

## 10.11 Alumnos

All ADMIN (class-level)

- `GET /api/alumnos` (header required)
- `GET /api/alumnos/{id}` (header optional)
- `GET /api/alumnos/buscar-por-rut` (header optional)
- `POST /api/alumnos` (201)
- `PUT /api/alumnos/{id}`
- `POST /api/alumnos/con-apoderado` (201)

## 10.12 Apoderados admin

All ADMIN (class-level)

- `POST /api/apoderados` (201)
- `GET /api/apoderados/buscar-por-rut`
- `GET /api/apoderados/por-alumno/{alumnoId}` (200 or 204)

## 10.13 Portal apoderado

- `GET /api/apoderado/mis-alumnos` (APODERADO, header required)
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/mensual` (APODERADO, header required)
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/resumen` (APODERADO, header required)

## 10.14 Matriculas

- `POST /api/matriculas` (ADMIN, header required) -> 201
- `GET /api/matriculas/curso/{cursoId}` (ADMIN, PROFESOR, header required)
- `GET /api/matriculas/alumno/{alumnoId}` (ADMIN)
- `PATCH /api/matriculas/{id}/estado` (ADMIN)

## 10.15 Asistencia

- `POST /api/asistencia/clase` (PROFESOR, ADMIN) -> 201
- `GET /api/asistencia/clase` (PROFESOR, ADMIN)
  - PROFESOR: valida ownership del bloque por `profesorId`.
  - ADMIN: bypass de ownership (`profesorId = null` en el use case).

## 10.16 Jornada (`/api/cursos/{cursoId}/jornada`)

- `PUT /{diaSemana}` (ADMIN)
- `GET /` (ADMIN, APODERADO)
- `GET /resumen` (ADMIN)
- `POST /{diaSemanaOrigen}/copiar` (ADMIN)
- `DELETE /{diaSemana}` (ADMIN)
- `GET /materias-disponibles` (ADMIN)
- `PATCH /bloques/{bloqueId}/materia` (ADMIN)
- `DELETE /bloques/{bloqueId}/materia` (ADMIN)
- `GET /asignacion-materias` (ADMIN)
- `GET /bloques/{bloqueId}/profesores-disponibles` (ADMIN)
- `PATCH /bloques/{bloqueId}/profesor` (ADMIN)
- `DELETE /bloques/{bloqueId}/profesor` (ADMIN)
- `GET /asignacion-profesores` (ADMIN)

## 10.17 Dev tools (`@Profile("dev")`)

- `GET /api/dev/clock`
- `POST /api/dev/clock`
- `DELETE /api/dev/clock`

---

## 11) Use case catalog (real signatures)

All use cases currently expose `execute(...)` as the entry method convention.

## 11.1 Alumno
- `ActualizarAlumno.execute(UUID alumnoId, AlumnoRequest request)`
- `BuscarAlumnoPorRut.execute(String rut, UUID anoEscolarId)`
- `CrearAlumno.execute(AlumnoRequest request)`
- `CrearAlumnoConApoderado.execute(CrearAlumnoConApoderadoRequest request)`
- `ObtenerAlumnos.execute(UUID anoEscolarId, Integer page, Integer size, String sortBy, String sortDir, UUID cursoId, UUID gradoId, String q)`
- `ObtenerDetalleAlumno.execute(UUID alumnoId, UUID anoEscolarId)`

## 11.2 Ano escolar
- `ActualizarAnoEscolar.execute(UUID id, AnoEscolarRequest request)`
- `CrearAnoEscolar.execute(AnoEscolarRequest request)`
- `ListarAnosEscolares.execute(Integer page, Integer size)`
- `ObtenerAnoEscolar.execute(UUID id)`
- `ObtenerAnoEscolarActivo.execute()`

## 11.3 Apoderado
- `BuscarApoderadoPorRut.execute(String rut)`
- `CrearApoderadoConUsuario.execute(ApoderadoRequest request)`
- `ObtenerAlumnosApoderado.execute(UUID apoderadoId, UUID anoEscolarId, Integer page, Integer size)`
- `ObtenerApoderadoPorAlumno.execute(UUID alumnoId)`
- `ObtenerAsistenciaMensualAlumno.execute(UUID alumnoId, int mes, int anio, UUID apoderadoId, UUID anoEscolarId)`
- `ObtenerResumenAsistenciaAlumno.execute(UUID alumnoId, UUID anoEscolarId, UUID apoderadoId)`

## 11.4 Asistencia
- `GuardarAsistenciaClase.execute(GuardarAsistenciaRequest request, UUID profesorId, UUID usuarioId, Rol rolUsuario)`
- `ObtenerAsistenciaClase.execute(UUID bloqueHorarioId, LocalDate fecha, UUID profesorId)`

## 11.5 Auditoria
- `ConsultarEventosAuditoria.execute(UUID usuarioId, String metodoHttp, String endpoint, LocalDate desde, LocalDate hasta, int page, int size)`

## 11.6 Auth
- `LoginUsuario.execute(LoginRequest request, HttpServletRequest httpRequest)`
- `ObtenerPerfilAutenticado.execute(UserPrincipal user)`
- `RefrescarToken.execute(RefreshTokenRequest request)`

## 11.7 Calendario
- `CrearDiasNoLectivos.execute(CrearDiaNoLectivoRequest request, UUID anoEscolarId)`
- `EliminarDiaNoLectivo.execute(UUID id)`
- `ListarDiasNoLectivos.execute(UUID anoEscolarId, Integer mes, Integer anio, Integer page, Integer size)`

## 11.8 Curso
- `ActualizarCurso.execute(UUID cursoId, UUID anoEscolarId, CursoRequest request)`
- `CrearCurso.execute(UUID anoEscolarId, CursoRequest request)`
- `ObtenerCursos.execute(UUID anoEscolarId, UUID gradoId, int page, int size, String sortBy, String sortDir)`
- `ObtenerDetalleCurso.execute(UUID cursoId)`

## 11.9 Dashboard
- `ObtenerDashboardAdmin.execute(UUID anoEscolarId)`

## 11.10 Grado
- `ListarGrados.execute(Integer page, Integer size, String sortDir)`
- `ObtenerGrado.execute(UUID id)`

## 11.11 Jornada
- `AsignarMateriaBloque.execute(UUID cursoId, UUID bloqueId, UUID materiaId)`
- `AsignarProfesorBloque.execute(UUID cursoId, UUID bloqueId, UUID profesorId)`
- `CopiarJornadaDia.execute(UUID cursoId, Integer diaSemanaOrigen, List<Integer> diasDestino)`
- `EliminarJornadaDia.execute(UUID cursoId, Integer diaSemana)`
- `GuardarJornadaDia.execute(UUID cursoId, Integer diaSemana, JornadaDiaRequest request)`
- `ObtenerJornadaCurso.execute(UUID cursoId, Integer diaSemana)`
- `ObtenerJornadaCurso.execute(UUID cursoId, Integer diaSemana, UserPrincipal user)`
- `ObtenerMateriasDisponibles.execute(UUID cursoId, UUID bloqueId)`
- `ObtenerProfesoresDisponibles.execute(UUID cursoId, UUID bloqueId)`
- `ObtenerResumenAsignacionMaterias.execute(UUID cursoId)`
- `ObtenerResumenAsignacionProfesores.execute(UUID cursoId)`
- `QuitarMateriaBloque.execute(UUID cursoId, UUID bloqueId)`
- `QuitarProfesorBloque.execute(UUID cursoId, UUID bloqueId)`
- `ValidarAccesoJornadaCurso.execute(UserPrincipal user, UUID cursoId)`

## 11.12 Malla
- `ActualizarMallaCurricular.execute(UUID id, Integer horasPedagogicas, Boolean activo)`
- `CrearMallaCurricular.execute(UUID anoEscolarId, MallaCurricularRequest request)`
- `EliminarMallaCurricular.execute(UUID id)`
- `GuardarMallaCurricularBulk.execute(UUID anoEscolarId, MallaCurricularBulkRequest request)`
- `ListarMallaCurricularPorAnoEscolar.execute(UUID anoEscolarId, int page, int size)`
- `ListarMallaCurricularPorGrado.execute(UUID anoEscolarId, UUID gradoId, int page, int size)`
- `ListarMallaCurricularPorMateria.execute(UUID anoEscolarId, UUID materiaId, int page, int size)`

## 11.13 Materia
- `ActualizarMateria.execute(UUID id, MateriaRequest request)`
- `CrearMateria.execute(MateriaRequest request)`
- `EliminarMateria.execute(UUID id)`
- `ListarMaterias.execute(Integer page, Integer size, String sortBy, String sortDir)`
- `ObtenerMateria.execute(UUID id)`

## 11.14 Matricula
- `CambiarEstadoMatricula.execute(UUID matriculaId, String nuevoEstadoRaw)`
- `MatricularAlumno.execute(MatriculaRequest request, UUID anoEscolarId)`
- `ObtenerMatriculasPorAlumno.execute(UUID alumnoId, int page, int size, String sortBy, String sortDir)`
- `ObtenerMatriculasPorCurso.execute(UUID cursoId, UserPrincipal principal, UUID anoEscolarId, int page, int size, String sortBy, String sortDir)`
- `ValidarAccesoMatriculasCursoProfesor.execute(UserPrincipal principal, UUID cursoId, UUID anoEscolarId)`

## 11.15 Profesor
- `ActualizarProfesor.execute(UUID id, ProfesorRequest request)`
- `CrearProfesorConUsuario.execute(ProfesorRequest request)`
- `ObtenerClasesHoyProfesor.execute(UserPrincipal principal, UUID anoEscolarId)`
- `ObtenerCumplimientoAsistenciaProfesor.execute(UUID profesorId, LocalDate fecha, UUID anoEscolarId)`
- `ObtenerDetalleProfesor.execute(UUID id, UUID anoEscolarId)`
- `ObtenerHorarioProfesor.execute(UUID profesorId, UUID anoEscolarId, UserPrincipal principal)`
- `ObtenerProfesores.execute(int page, int size, String sortBy, String sortDir)`
- `ObtenerSesionesProfesor.execute(UUID profesorId, LocalDate desde, LocalDate hasta, int page, int size)`

---

## 12) Repositories and key query strategy

## 12.1 Repository inventory
- `AlumnoRepository`
- `AnoEscolarRepository`
- `ApoderadoAlumnoRepository`
- `ApoderadoRepository`
- `AsistenciaClaseRepository`
- `BloqueHorarioRepository`
- `CursoRepository`
- `DiaNoLectivoRepository`
- `EventoAuditoriaRepository`
- `GradoRepository`
- `MallaCurricularRepository`
- `MateriaRepository`
- `MatriculaRepository`
- `ProfesorRepository`
- `RegistroAsistenciaRepository`
- `SeccionCatalogoRepository`
- `SesionUsuarioRepository`
- `UsuarioRepository`

## 12.2 Notable patterns in queries

- Extensive use of `@EntityGraph` and dedicated fetch queries to avoid lazy failures/N+1.
- Batch aggregate query for matricula counts by course:
  - `MatriculaRepository.countActivasByCursoIds(...)`
- Batch retrieval for attendance-taken flags:
  - `AsistenciaClaseRepository.findBloqueIdsConAsistenciaTomada(...)`
- Batch retrieval of asistencia records by block/date:
  - `AsistenciaClaseRepository.findByBloqueIdsAndFecha(...)`
- Batch aggregation of attendance counts by asistencia/estado:
  - `RegistroAsistenciaRepository.countByEstadoGroupedByAsistenciaClaseId(...)`
- Dedicated fetch for daily teacher class blocks in school year:
  - `BloqueHorarioRepository.findBloquesClaseByProfesorAndDia(...)`
- Batch collision queries for schedule assignment:
  - `BloqueHorarioRepository.findColisionesProfesoresConCursoYMateria(...)`
- Filtering with boolean apply flags in audit/session repositories to avoid PostgreSQL type ambiguity:
  - `EventoAuditoriaRepository.findByFiltros(...)`
  - `SesionUsuarioRepository.findByUsuarioIdAndFechas(...)`

## 12.3 Legacy residue to know
- `RegistroAsistenciaRepository.deleteByAsistenciaClaseId(...)` still exists but main attendance flow uses in-place merge and not delete+insert.

---

## 13) DTO catalog

## 13.1 Request DTOs (`dto/request`)
- `ApoderadoRequest`
- `AlumnoRequest`
- `AnoEscolarRequest`
- `AsignarMateriaRequest`
- `AsignarProfesorRequest`
- `BloqueRequest`
- `CambiarEstadoMatriculaRequest`
- `CopiarJornadaRequest`
- `CrearAlumnoConApoderadoRequest`
- `CrearDiaNoLectivoRequest`
- `CursoRequest`
- `GuardarAsistenciaRequest`
- `JornadaDiaRequest`
- `LoginRequest`
- `MallaCurricularBulkRequest`
- `MallaCurricularRequest`
- `MallaCurricularUpdateRequest`
- `MateriaRequest`
- `MatriculaRequest`
- `ProfesorRequest`
- `RefreshTokenRequest`
- `RegistroAlumnoRequest`

## 13.2 Response DTOs (`dto/response`)
Includes paginated wrappers and domain responses, among others:
- alumno: `AlumnoResponse`, `AlumnoPageResponse`, `AlumnoApoderadoPageResponse`
- ano escolar: `AnoEscolarResponse`, `AnoEscolarPageResponse`
- curso: `CursoResponse`, `CursoPageResponse`
- grado: `GradoResponse`, `GradoPageResponse`
- materia: `MateriaResponse`, `MateriaPageResponse`, `MateriaDisponibleResponse`, `MateriasDisponiblesResponse`
- malla: `MallaCurricularResponse`, `MallaCurricularPageResponse`
- profesor: `ProfesorResponse`, `ProfesorPageResponse`, `ProfesorHorarioResponse`, `ProfesorDisponibleResponse`, `ProfesorResumenAsignacionResponse`, `ProfesoresDisponiblesResponse`
- profesor sesiones/auditoria: `SesionProfesorResponse`, `SesionProfesorPageResponse`, `EventoAuditoriaResponse`, `EventoAuditoriaPageResponse`
- matricula: `MatriculaResponse`, `MatriculaPageResponse`
- asistencia/jornada: `AsistenciaClaseResponse`, `RegistroAsistenciaResponse`, `JornadaDiaResponse`, `JornadaCursoResponse`, `JornadaResumenResponse`, `BloqueHorarioResponse`, `AsignacionMateriaResumenResponse`, `AsignacionProfesoresResumenResponse`, `BloquePendienteProfesorResponse`, `BloqueProfesorResumenResponse`
- apoderado/portal: `ApoderadoResponse`, `ApoderadoBuscarResponse`, `AlumnoApoderadoResponse`, `AlumnoApoderadoPageResponse`, `AsistenciaDiaResponse`, `AsistenciaMensualResponse`, `ResumenAsistenciaResponse`
- auth/error: `AuthResponse`, `ApiErrorResponse`
- calendario/dashboard: `DiaNoLectivoResponse`, `DiaNoLectivoPageResponse`, `DashboardAdminResponse`
- profesor me: `ClaseHoyResponse`, `ClasesHoyResponse`, `EstadoClaseHoy`
- cumplimiento asistencia admin: `CumplimientoAsistenciaResponse`, `EstadoCumplimiento`

## 13.3 Projection DTOs (`dto/projection`)
- `RegistroConFecha` (proyección interna usada por repositorio de asistencia; no expuesto como contrato REST)

---

## 14) Business rule highlights currently enforced

- School year state is calculated by date (`FUTURO`, `PLANIFICACION`, `ACTIVO`, `CERRADO`).
- Mutations for many domains are blocked when year is `CERRADO`.
- Attendance write path:
  - enforces school day alignment and no weekend,
  - rejects non-teaching days (`dia_no_lectivo`),
  - enforces strict window for PROFESOR (same day + +/-15 min),
  - allows ADMIN bypass for exceptional corrections.
- Attendance read path (`GET /api/asistencia/clase`):
  - PROFESOR: must own the block.
  - ADMIN: can read without ownership check.
- Admin compliance view (`GET /api/profesores/{id}/cumplimiento-asistencia`):
  - computes block state (`TOMADA`, `NO_TOMADA`, `EN_CURSO`, `PROGRAMADA`) using date/time,
  - returns per-block attendance summary (`presentes`, `ausentes`, `total`) when attendance exists.
- Attendance child records are merged in place (UUIDs preserved).
- RUT validation includes format + check digit + cross-person uniqueness checks.
- Schedule assignment validates:
  - block type,
  - malla membership,
  - weekly minutes budget,
  - teacher qualification for subject,
  - collision detection.
- Matricula state transition logic is centralized in `Matricula.cambiarEstado(...)`.

---

## 15) Transactions and consistency profile

Observed transactional strategy:
- Use cases that mutate state use `@Transactional`.
- Read use cases generally use `@Transactional(readOnly = true)`.
- Audit AOP writes in independent transaction (`REQUIRES_NEW`).
- OSIV is disabled; read flows rely on explicit fetching within transactional boundaries.

---

## 16) Tests currently in repository

Current test files:
- `SchoolmateApiApplicationTests` (context load)
- `ApiSecurityHandlersTest`
- `JwtAuthenticationFilterTest`
- `LoginUsuarioTest`
- `RefrescarTokenTest`

Coverage concentration today:
- security handlers and JWT filter behavior
- auth login/refresh token lifecycle

Gaps still visible:
- no broad unit coverage yet for most use cases (curso, malla, jornada, matricula, asistencia, etc.).

---

## 17) Known technical observations (current reality)

1. Config files currently expose DB/JWT secrets in plain text.
2. Migration history intentionally contains marker scripts because some DDL ran directly in Supabase.
3. Jornada ownership validation is now enforced inside `ObtenerJornadaCurso` use case (not in controller), matching the same layering pattern used in `ObtenerMatriculasPorCurso`.

These are not hypothetical; they are visible in current code/config.

---

## 18) How to run locally

Typical flow:
1. Ensure Java 21 + Maven available.
2. Configure DB and JWT secrets (recommended via env vars, although current YAML has explicit values).
3. Run:

```bash
mvn spring-boot:run
```

Useful checks:

```bash
mvn -q -DskipTests compile
mvn test
```

---

## 19) Guidance for future contributors and agents

When changing backend behavior:
- update code first,
- then update this document in the same PR/commit,
- keep endpoint contracts explicit (status, auth, request/response DTOs),
- reflect any migration and caching/security change explicitly.

If a section here conflicts with Java code, Java code is authoritative and this file must be corrected immediately.
