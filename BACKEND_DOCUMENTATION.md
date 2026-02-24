# BACKEND_DOCUMENTATION.md

## SECCIÓN 1: RESUMEN EJECUTIVO

`schoolmate-hub-api` es un backend REST para gestión escolar (catálogos académicos, cursos, profesores, alumnos, matrícula, jornada escolar por curso y portal de apoderado). La API está construida en Spring Boot con seguridad JWT stateless y acceso a PostgreSQL en Supabase usando JPA + Flyway. La implementación actual mantiene foco administrativo (`ADMIN`), pero ya incorpora flujos de `APODERADO` para consultar hijos, asistencia y jornada con ownership, además de creación transaccional `alumno + apoderado` desde administración. El diseño separa catálogos estables (`materia`, `grado`, `año`) de vínculos temporales (`malla_curricular`, `matricula`) y bloques de jornada (`bloque_horario`).

### Stack tecnológico (versiones exactas verificadas)

Fuente: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/pom.xml` + `mvn dependency:tree`.

| Componente | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.2 |
| Spring Framework | 7.0.3 (transitivo) |
| Spring Security | 7.0.2 (transitivo) |
| Spring AOP | 7.0.3 |
| AspectJ Weaver | 1.9.25.1 |
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
| Alumnos | ✅ operativo (incluye alta con apoderado) |
| Matrículas | ✅ operativo |
| Jornada escolar por curso | ✅ operativo |
| Ownership por profesor/apoderado | ⚠️ parcial (profesor + apoderado en dominios acotados) |
| Asistencia, reportes, dashboards | ⚠️ parcial (asistencia operativa + dashboard admin base; reportes y dashboards avanzados pendientes) |

### Actualización técnica reciente (V19 + V20)

Aplicado en Java sobre migraciones ya ejecutadas en BD:

- V19:
  - `Materia` incluye `activo` (`Boolean`, default `true`, `nullable = false`).
  - `RegistroAsistencia` incluye `observacion` (`String`, largo máximo 500).
  - `EstadoAsistencia` ahora soporta: `PRESENTE`, `AUSENTE`, `TARDANZA`, `JUSTIFICADO`.
  - `RegistroAlumnoRequest` ahora recibe `observacion` opcional (`@Size(max = 500)`).
  - Flujo de asistencia (`GuardarAsistenciaClase` y `ObtenerAsistenciaClase`) persiste y retorna `observacion` en `RegistroAsistenciaResponse`.

- V20:
  - Migración global de IDs a `UUID` en entidades, DTOs, repositories, controllers, use cases y `UserPrincipal`.
  - PKs JPA usan `@GeneratedValue(strategy = GenerationType.UUID)` (ya no se usa generación manual en `@PrePersist` para IDs).
  - FKs planas (`Usuario.profesorId`, `Usuario.apoderadoId`) migradas a `UUID`.
  - Repositorios JPA migrados a `JpaRepository<Entidad, UUID>` (excepto claves naturales como `SeccionCatalogo.letra`).
  - Endpoints y filtros que reciben IDs ahora usan tipo `UUID` (path/query/body).

### Actualización técnica reciente (optimización de lectura y seguridad)

- `spring.jpa.open-in-view=false` en `application.yml` para evitar lazy loading implícito durante serialización de respuestas.
- Endpoints de lectura desacoplados a use cases con `@Transactional(readOnly = true)`:
  - `GradoController` (`listar`,`obtener`) via `ListarGrados` y `ObtenerGrado`.
  - `MateriaController` (`listar`,`obtener`) via `ListarMaterias` y `ObtenerMateria`.
  - Nota: `CursoController` (`listar`,`obtener`), `ProfesorController` (`listar`,`obtener`,`obtenerSesiones`), `AnoEscolarController` (`listar`,`obtener`,`obtenerActivo`), `MatriculaController` (`porCurso`,`porAlumno`) y `DiaNoLectivoController` (`listar`) ya estaban desacoplados a use cases de lectura.
- Fix de autenticación en `/api/auth/me`:
  - `SecurityConfig` limita públicos a `/api/auth/login` y `/api/auth/registro`.
  - `/api/auth/me` queda autenticado.
  - Guard defensivo centralizado en `ObtenerPerfilAutenticado` para responder `401` si principal nulo.
- Correcciones N+1 en curso/jornada mediante `@EntityGraph` y queries con `JOIN FETCH` en `CursoRepository`, `MallaCurricularRepository`, `BloqueHorarioRepository`, y ajuste de consumo en use cases/controladores de jornada.
- `AlumnoController.getMatriculaMap` ahora consulta solo matrículas de alumnos de la página (`alumno_id IN (...)`) vía `MatriculaRepository.findByAlumnoIdInAndAnoEscolarIdAndEstado(...)`.
- Hardening adicional contra `LazyInitializationException` en mutaciones:
  - `CrearCurso` y `ActualizarCurso` recargan curso con `grado` y `anoEscolar` (`findByIdWithGradoAndAnoEscolar`) antes del mapeo a `CursoResponse`.
  - `CrearProfesorConUsuario` y `PUT /api/profesores/{id}` recargan profesor con `materias` (`findByIdWithMaterias`) antes del mapeo a `ProfesorResponse`.
  - `CambiarEstadoMatricula` usa `findByIdWithRelaciones` para devolver `MatriculaResponse` sin depender de OSIV.
  - `ProfesorRepository.findByIdWithMaterias` usa `@Query` + `@EntityGraph` (evita parsing derivado inválido de Spring Data para ese nombre de método).
- Refactor Clean Architecture:
  - `MallaCurricularController` quedó delgado y delega en use cases por endpoint (`listar/crear/actualizar/bulk/eliminar`), manteniendo contratos HTTP sin cambios.
  - `AnoEscolarController` quedó delgado y delega todos sus endpoints (`listar/obtener/activo/crear/actualizar`) en use cases dedicados.
  - `ApoderadoController`, `ProfesorHorarioController`, `JornadaController`, `MatriculaController` y `DiaNoLectivoController` quedaron delgados; su orquestación/validación principal fue movida a use cases dedicados.
- Caching HTTP:
  - `CacheConfig` registra `ShallowEtagHeaderFilter` para `/api/*` (ETag automático por body en GET).
  - `CacheControlInterceptor` aplica `Cache-Control` por grupo de rutas (catálogos/configuración vs transaccional/sensible).
  - `SecurityConfig` deshabilita headers cache por defecto de Spring Security (`headers.cacheControl(...disable())`) para no sobrescribir la política del interceptor.

### Actualización técnica reciente (auth con refresh token persistido)

- Migración Flyway `V19__add_refresh_token_to_usuario.sql`:
  - agrega `usuario.refresh_token` (`varchar(255)`, nullable).
  - agrega índice único parcial `ux_usuario_refresh_token_not_null`.
- Login ahora emite `accessToken` + `refreshToken` y persiste el refresh token en `usuario` (un solo refresh token activo por usuario/dispositivo).
- Nuevo endpoint público `POST /api/auth/refresh` para renovar sesión:
  - busca usuario por `refreshToken`.
  - rota refresh token en cada refresh.
  - retorna nuevo `accessToken` y nuevo `refreshToken`.
- `JwtAuthenticationFilter` quedó stateless:
  - valida firma/claims del JWT y construye `UserPrincipal` directo desde claims.
  - ya no consulta BD por request autenticada.
- Manejo de errores auth/seguridad estandarizado en formato `ApiErrorResponse`:
  - `TOKEN_EXPIRED` (401), `UNAUTHORIZED` (401), `SESSION_REVOKED` (401), `ACCESS_DENIED` (403).
  - `SecurityConfig` usa `ApiAuthenticationEntryPoint` y `ApiAccessDeniedHandler`.

### Actualización técnica reciente (REQ-A sesiones y trazabilidad)

- Registro de sesiones de login en nueva entidad `SesionUsuario`:
  - guarda `usuario`, `ipAddress`, `latitud`, `longitud`, `precisionMetros`, `userAgent`, `createdAt`.
  - se registra solo en login exitoso (`POST /api/auth/login`), no en refresh ni en login fallido.
- `LoginRequest` ahora acepta geolocalización opcional (`latitud`, `longitud`, `precisionMetros`) sin romper clientes legacy.
- Trazabilidad de asistencia:
  - `AsistenciaClase` incorpora `registradoPor` (`usuario_id` nullable para históricos).
  - `GuardarAsistenciaClase` setea siempre el usuario autenticado que registra/edita.
  - `AsistenciaClaseResponse` expone `registradoPorNombre`.
- Nuevo endpoint admin: `GET /api/profesores/{profesorId}/sesiones` con filtros `desde/hasta` y paginación.
- Cache-control ajustado: `/api/profesores/{profesorId}/sesiones` usa `Cache-Control: no-store, private`.

### Actualización técnica reciente (REQ-B auditoría automática)

- Se incorporó auditoría automática de mutaciones HTTP con AOP:
  - `AuditoriaAspect` intercepta `@PostMapping/@PutMapping/@PatchMapping/@DeleteMapping` en `@RestController`.
  - excluye `/api/auth/**` y `/api/dev/**`.
  - persiste evento en `evento_auditoria` con transacción independiente (`REQUIRES_NEW`) y sin bloquear la operación principal ante fallas de auditoría.
- Nueva entidad `EventoAuditoria` + `EventoAuditoriaRepository`:
  - captura snapshot de usuario (`usuarioEmail`, `usuarioRol`), método, endpoint, `requestBody` (JSONB), status, IP, `anoEscolarId`, fecha/hora.
- Nuevo endpoint admin de consulta:
  - `GET /api/auditoria` con filtros por `usuarioId`, `metodoHttp`, `endpoint` parcial y rango `desde/hasta`.
  - response paginado `EventoAuditoriaPageResponse` con `requestBody` deserializado como JSON estructurado.
- `CacheControlInterceptor` clasifica `/api/auditoria/**` como sensible: `Cache-Control: no-store, private`.
- Migración marcador agregada: `V22__audit_trail_operaciones.sql` (DDL ejecutado en Supabase).

### Actualización técnica reciente (REQ-05 baseline Flyway)

- Se agregó `V23__consolidacion_esquema_faltante.sql` como migración idempotente de sinceramiento de drift.
- `V23` consolida DDL histórico faltante para ambientes limpios:
  - tablas: `alumno`, `bloque_horario`, `apoderado`, `apoderado_alumno`, `sesion_usuario`, `evento_auditoria`.
  - columnas/constraints de drift: `ano_escolar.fecha_inicio_planificacion`, `asistencia_clase.registrado_por_usuario_id`.
- Objetivo operativo: bootstrap reproducible de una BD nueva sin depender de ejecuciones manuales en Supabase.

### Actualización técnica reciente (REQ-06 + REQ-08 asistencia)

- `GuardarAsistenciaClase` dejó el anti-patrón `DELETE + INSERT` de `registro_asistencia` y ahora usa conciliación in-place por `alumnoId`:
  - actualiza existentes, elimina huérfanos (`orphanRemoval`) y agrega solo nuevos.
- `AsistenciaClase` expone colección administrada de `registros` con:
  - `@OneToMany(mappedBy = "asistenciaClase", cascade = CascadeType.ALL, orphanRemoval = true)`.
- Cierre temporal estricto por rol:
  - `PROFESOR`: solo puede guardar/editar cuando `fecha == hoy` y dentro de ventana `[horaInicio-15m, horaFin+15m]`.
  - `ADMIN`: bypass de cierre temporal y ownership de bloque para gestión excepcional.
- Nuevo código de error de negocio: `ASISTENCIA_CERRADA` (`400`) para violaciones de ventana temporal en profesor.

### Actualización técnica reciente (REQ-05 días no lectivos)

- Se incorporó calendario de excepciones académicas (`dia_no_lectivo`) por año escolar:
  - tipos soportados: `FERIADO_LEGAL`, `VACACIONES`, `SUSPENSION`, `INTERFERIADO`, `ADMINISTRATIVO`.
  - create/delete bloqueados cuando el año escolar está `CERRADO`.
  - creación por rango con reglas: máximo 60 días, exclusión de sábados/domingo, y validación de rango del año escolar.
- Nuevo dominio API:
  - `GET /api/dias-no-lectivos` (autenticado) con filtro opcional `mes` + `anio`.
  - `POST /api/dias-no-lectivos` (ADMIN) para alta masiva por rango.
  - `DELETE /api/dias-no-lectivos/{id}` (ADMIN).
- Integración en flujos existentes:
  - `GuardarAsistenciaClase` bloquea registro/edición si la fecha está marcada como no lectiva.
  - `ObtenerClasesHoyProfesor` enriquece la respuesta con `diaNoLectivo` para feedback de UI.
  - `ObtenerAsistenciaMensualAlumno` incorpora `diasNoLectivos` en el response mensual.
- Caching:
  - `CacheControlInterceptor` define `Cache-Control: max-age=120` para `/api/dias-no-lectivos` (recurso de configuración de baja frecuencia de cambio).

### Actualización técnica reciente (REQ-09 dashboard admin base + fix auditoría)

- Se incorporó endpoint de KPIs base para administración:
  - `GET /api/dashboard/admin/resumen` (`ADMIN`).
  - response `DashboardAdminResponse` con `totalAlumnos`, `totalCursos`, `totalProfesores`.
  - lectura por año escolar resuelto vía `@AnoEscolarActivo`.
- Nuevo caso de uso `ObtenerDashboardAdmin`:
  - conteo en BD (sin cargar entidades en memoria) usando:
    - `MatriculaRepository.countByAnoEscolarIdAndEstado(..., ACTIVA)`
    - `CursoRepository.countByAnoEscolarIdAndActivoTrue(...)`
    - `ProfesorRepository.countByActivoTrue()`
- Ajuste en consulta de auditoría (`GET /api/auditoria`) para PostgreSQL:
  - se reemplazó estrategia `:param IS NULL` por flags booleanos `aplicarFiltro`.
  - evita errores de tipado SQL (`42P18`, `varchar ~~ bytea`) con filtros opcionales.
- Ajuste equivalente en sesiones de profesor (`GET /api/profesores/{profesorId}/sesiones`):
  - `SesionUsuarioRepository.findByUsuarioIdAndFechas` usa flags `aplicarDesde/aplicarHasta`.
  - elimina errores SQL de tipado con filtros opcionales de fecha (`42P18`).

### Actualización técnica reciente (refactor incremental de controllers)

- `MateriaController` quedó delgado y delega completamente en use cases:
  - `ListarMaterias`, `ObtenerMateria`, `CrearMateria`, `ActualizarMateria`, `EliminarMateria`.
- `GradoController` quedó delgado y delega lecturas en:
  - `ListarGrados`, `ObtenerGrado`.
- `AuditoriaController` quedó delgado y delega filtros/paginación/mapeo JSON en:
  - `ConsultarEventosAuditoria`.
- `AuthController` delega `GET /api/auth/me` en:
  - `ObtenerPerfilAutenticado`.
- `MatriculaController` usa request tipado para cambio de estado:
  - nuevo DTO `CambiarEstadoMatriculaRequest {estado}`.
  - `CambiarEstadoMatricula` incorpora validación de estado raw y responde `ApiException(VALIDATION_FAILED)` para entradas inválidas.
- `ObtenerResumenAsistenciaAlumno` centraliza resolución de año escolar (`header > query`) dentro del use case y emite `VALIDATION_FAILED` cuando falta ambos.

### Actualización técnica reciente (frontera limpia en cursos/malla/matrículas/calendario)

- Controllers ajustados para no recibir entidades JPA en la capa HTTP:
  - `CursoController`, `MallaCurricularController`, `MatriculaController`, `DiaNoLectivoController`.
- Resolución de año escolar en estos controllers vía header UUID:
  - `@RequestHeader("X-Ano-Escolar-Id") UUID` (opcional/requerido según endpoint).
- Mapeo entidad -> response movido a use cases en mutaciones:
  - `CrearCurso`, `ActualizarCurso`, `MatricularAlumno`, `CambiarEstadoMatricula`, `CrearDiasNoLectivos` retornan DTO de salida.
- DTO extraído desde controller:
  - `MallaCurricularUpdateRequest` ahora vive en `dto/request` (ya no inner class en `MallaCurricularController`).

### Actualización técnica reciente (hardening JPA: Lombok en entidades)

- Se eliminó `@Data` de entidades JPA para evitar generación automática de `equals/hashCode/toString` sobre relaciones LAZY.
- Entidades migradas a combinación segura:
  - `@Getter` + `@Setter` + constructores (`@NoArgsConstructor`, `@AllArgsConstructor`) + `@Builder` donde aplica.
- Alcance aplicado:
  - `Alumno`, `AnoEscolar`, `Apoderado`, `ApoderadoAlumno`, `Curso`, `EventoAuditoria`, `Grado`, `MallaCurricular`, `Materia`, `Matricula`, `Profesor`, `SeccionCatalogo`, `SesionUsuario`, `Usuario`.
- Excepción explícita:
  - `ApoderadoAlumnoId` mantiene `@Data` por ser clave embebida (`@Embeddable`) y requerir `equals/hashCode` de valor para identidad compuesta.

### Actualización técnica reciente (anti-pattern fixes: fuga de entidad + N+1 apoderado)

- Antipatrón #5 (fuga de entidad REST) corregido en grados:
  - `GET /api/grados` y `GET /api/grados/{id}` ahora responden `GradoResponse` (ya no entidad `Grado` directa).
  - nuevo DTO: `dto/response/GradoResponse`.
- Antipatrón #1 (N+1 en stream/map) corregido en flujos de apoderado:
  - `ObtenerAlumnosApoderado`: reemplaza `findById` por iteración y lookup de matrícula por alumno por consultas batch:
    - vínculos con alumno via `findByApoderadoIdWithAlumno(...)`
    - matrículas activas por lote via `findByAlumnoIdInAndAnoEscolarIdAndEstado(...)`
  - `ObtenerApoderadoPorAlumno` y `CrearApoderadoConUsuario`:
    - dejan de resolver alumnos con `findById(...)` por cada vínculo y reutilizan `findByApoderadoIdWithAlumno(...)`.
  - resultado: menor cantidad de queries por request en portal/gestión de apoderados.

### Actualización técnica reciente (anti-pattern batch: paginación + N+1 + validaciones DB)

- Endpoints de lectura de colecciones ahora paginados para evitar crecimiento no acotado de payload:
  - `GET /api/cursos` -> `CursoPageResponse`
  - `GET /api/profesores` -> `ProfesorPageResponse`
  - `GET /api/malla-curricular` y derivados por `materia/grado` -> `MallaCurricularPageResponse`
  - `GET /api/matriculas/curso/{cursoId}` y `GET /api/matriculas/alumno/{alumnoId}` -> `MatriculaPageResponse`
- Nuevos DTOs de paginación agregados en `dto/response`:
  - `CursoPageResponse`, `ProfesorPageResponse`, `MallaCurricularPageResponse`, `MatriculaPageResponse`.
- Antipatrón #1 (N+1) adicional corregido:
  - `BuscarApoderadoPorRut` dejó de consultar matrículas por alumno dentro de `map` y ahora resuelve curso activo por lote con `findByAlumnoIdInAndEstadoOrderByFechaMatriculaDescCreatedAtDesc(...)`.
  - `ObtenerClasesHoyProfesor` dejó de consultar por bloque dentro de `map`:
    - `cantidadAlumnos` ahora se resuelve por lote con `MatriculaRepository.countActivasByCursoIds(...)`.
    - `asistenciaTomada` ahora se resuelve por lote con `AsistenciaClaseRepository.findBloqueIdsConAsistenciaTomada(...)`.
  - resultado: `GET /api/profesor/mis-clases-hoy` reduce queries de `1 + 2N` a consultas batch fijas.
- Antipatrón #2 (filtrado en memoria) corregido en años escolares:
  - `CrearAnoEscolar` y `ActualizarAnoEscolar` validan solapamientos con queries de existencia en BD (`existsSolapamiento*`) en vez de `findAll()` + loop.
- Antipatrón #6 (acoplamiento temporal) corregido en fallback de entidades insert-only:
  - `SesionUsuario` y `EventoAuditoria` usan `TimeContext.now()` en `@PrePersist`.

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
  - `/api/alumnos` en `AlumnoController` (listado, detalle, búsqueda por RUT y mutaciones simples).
- Use case explícito:
  - `/api/materias` en `MateriaController` (`ListarMaterias`, `ObtenerMateria`, `CrearMateria`, `ActualizarMateria`, `EliminarMateria`).
  - `/api/grados` en `GradoController` (`ListarGrados`, `ObtenerGrado`).
  - `/api/auditoria` en `AuditoriaController` (`ConsultarEventosAuditoria`).
  - `/api/auth/me` en `AuthController` (`ObtenerPerfilAutenticado`).
  - `/api/cursos` en `CursoController` (`ObtenerCursos`, `ObtenerDetalleCurso`, `CrearCurso`, `ActualizarCurso`).
  - `/api/profesores` en `ProfesorController` (`ObtenerProfesores`, `ObtenerDetalleProfesor`, `ActualizarProfesor`, `ObtenerSesionesProfesor`, `CrearProfesorConUsuario`).
  - `/api/malla-curricular` en `MallaCurricularController` (`ListarMallaCurricularPorAnoEscolar`, `ListarMallaCurricularPorMateria`, `ListarMallaCurricularPorGrado`, `CrearMallaCurricular`, `ActualizarMallaCurricular`, `GuardarMallaCurricularBulk`, `EliminarMallaCurricular`).
  - `LoginUsuario` para autenticación JWT.
  - `MatricularAlumno` y `CambiarEstadoMatricula` para reglas de matrícula.
  - `GuardarJornadaDia`, `CopiarJornadaDia`, `EliminarJornadaDia` y `ObtenerJornadaCurso` para reglas de jornada.

### Principios de diseño observados

- No existe capa `Service` genérica transversal.
- No hay interfaces de caso de uso innecesarias (use cases concretos como clases).
- IDs en entidades: `UUID` nativo (Hibernate/JPA con `GenerationType.UUID`).
- Borrado lógico en algunos dominios (`activo=false`): malla curricular y bloques de jornada.
- Tiempo centralizado: `ClockProvider` + `TimeContext` permiten controlar `today/now` en entorno `dev` sin afectar `prod`.
- Auditoría de mutaciones desacoplada del flujo funcional vía AOP (`@AfterReturning`) y transacción separada (`REQUIRES_NEW`).

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
│   ├── common/rut            # normalización y validación de RUT
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
│   ├── usecase/alumno        # casos de uso de creación/enlace alumno-apoderado
│   ├── usecase/auth          # caso de uso login
│   ├── usecase/calendario    # casos de uso de días no lectivos
│   ├── usecase/dashboard     # casos de uso de KPIs dashboard admin
│   ├── usecase/jornada       # casos de uso jornada escolar
│   ├── usecase/malla         # casos de uso de malla curricular
│   ├── usecase/matricula     # casos de uso matrícula
│   └── usecase/profesor      # casos de uso de profesor
└── src/main/resources
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    ├── messages_es.properties
    └── db/migration          # migraciones Flyway versionadas en repo: V1..V23 (V24 aplicada manual en Supabase)
```

### TODAS las clases por paquete

- `com.schoolmate.api`
  - `SchoolmateApiApplication`
- `com.schoolmate.api.config`
  - `AnoEscolarArgumentResolver`
  - `AnoEscolarHeaderInterceptor`
  - `AuditoriaAspect`
  - `CacheConfig`
  - `CacheControlInterceptor`
  - `CorsConfig`
  - `JacksonConfig`
  - `SecurityConfig`
  - `WebMvcConfig`
- `com.schoolmate.api.common.time`
  - `ClockProvider`
  - `TimeContext`
- `com.schoolmate.api.common.rut`
  - `RutNormalizer`
  - `RutValidationService`
- `com.schoolmate.api.controller`
  - `AlumnoController`
  - `AnoEscolarController`
  - `ApoderadoController`
  - `ApoderadoPortalController`
  - `AuditoriaController`
  - `AuthController`
  - `CursoController`
  - `DevToolsController` (solo perfil `dev`)
  - `DashboardController`
  - `DiaNoLectivoController`
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
  - `EventoAuditoriaPageResponse`
  - `EventoAuditoriaResponse`
  - `RegistroConFecha`
  - `ResumenAsistenciaResponse`
  - `SesionProfesorPageResponse`
  - `SesionProfesorResponse`
- `com.schoolmate.api.dto.request`
  - `AlumnoRequest`
  - `AsignarMateriaRequest`
  - `AsignarProfesorRequest`
  - `AnoEscolarRequest`
  - `BloqueRequest`
  - `GuardarAsistenciaRequest`
  - `CopiarJornadaRequest`
  - `CambiarEstadoMatriculaRequest`
  - `CrearAlumnoConApoderadoRequest`
  - `CursoRequest`
  - `CrearDiaNoLectivoRequest`
  - `JornadaDiaRequest`
  - `LoginRequest`
  - `MallaCurricularBulkRequest`
  - `MallaCurricularUpdateRequest`
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
  - `DashboardAdminResponse`
  - `DiaNoLectivoResponse`
  - `EstadoClaseHoy`
  - `AuthResponse`
  - `CursoPageResponse`
  - `GradoResponse`
  - `CursoResponse`
  - `JornadaCursoResponse`
  - `JornadaDiaResponse`
  - `JornadaResumenResponse`
  - `MateriaDisponibleResponse`
  - `MallaCurricularResponse`
  - `MallaCurricularPageResponse`
  - `MateriaPageResponse`
  - `MateriaResponse`
  - `MatriculaPageResponse`
  - `MatriculaResponse`
  - `ProfesorDisponibleResponse`
  - `ProfesorHorarioResponse`
  - `ProfesorResumenAsignacionResponse`
  - `ProfesoresDisponiblesResponse`
  - `ProfesorPageResponse`
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
  - `DiaNoLectivo`
  - `EventoAuditoria`
  - `Grado`
  - `MallaCurricular`
  - `Materia`
  - `Matricula`
  - `Profesor`
  - `RegistroAsistencia`
  - `SeccionCatalogo`
  - `SesionUsuario`
  - `Usuario`
- `com.schoolmate.api.enums`
  - `EstadoAnoEscolar`
  - `EstadoAsistencia`
  - `EstadoMatricula`
  - `Rol`
  - `TipoDiaNoLectivo`
  - `TipoBloque`
  - `TipoPersona`
  - `VinculoApoderado`
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
  - `DiaNoLectivoRepository`
  - `EventoAuditoriaRepository`
  - `GradoRepository`
  - `MallaCurricularRepository`
  - `MateriaRepository`
  - `MatriculaRepository`
  - `ProfesorRepository`
  - `RegistroAsistenciaRepository`
  - `SesionUsuarioRepository`
  - `SeccionCatalogoRepository`
  - `UsuarioRepository`
- `com.schoolmate.api.security`
  - `AnoEscolarActivo`
  - `CustomUserDetailsService`
  - `JwtAuthenticationFilter`
  - `JwtConfig`
  - `JwtTokenProvider`
  - `UserPrincipal`
- `com.schoolmate.api.specification`
  - `AlumnoSpecifications`
- `com.schoolmate.api.usecase.apoderado`
  - `BuscarApoderadoPorRut`
  - `CrearApoderadoConUsuario`
  - `ObtenerAlumnosApoderado`
  - `ObtenerApoderadoPorAlumno`
  - `ObtenerAsistenciaMensualAlumno`
  - `ObtenerResumenAsistenciaAlumno`
- `com.schoolmate.api.usecase.auditoria`
  - `ConsultarEventosAuditoria`
- `com.schoolmate.api.usecase.auth`
  - `LoginUsuario`
  - `ObtenerPerfilAutenticado`
  - `RefrescarToken`
- `com.schoolmate.api.usecase.calendario`
  - `CrearDiasNoLectivos`
  - `EliminarDiaNoLectivo`
  - `ListarDiasNoLectivos`
- `com.schoolmate.api.usecase.curso`
  - `ActualizarCurso`
  - `CrearCurso`
  - `ObtenerCursos`
  - `ObtenerDetalleCurso`
- `com.schoolmate.api.usecase.dashboard`
  - `ObtenerDashboardAdmin`
- `com.schoolmate.api.usecase.grado`
  - `ListarGrados`
  - `ObtenerGrado`
- `com.schoolmate.api.usecase.alumno`
  - `ActualizarAlumno`
  - `BuscarAlumnoPorRut`
  - `CrearAlumno`
  - `CrearAlumnoConApoderado`
  - `ObtenerAlumnos`
  - `ObtenerDetalleAlumno`
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
  - `ValidarAccesoJornadaCurso`
- `com.schoolmate.api.usecase.malla`
  - `ActualizarMallaCurricular`
  - `CrearMallaCurricular`
  - `EliminarMallaCurricular`
  - `GuardarMallaCurricularBulk`
  - `ListarMallaCurricularPorAnoEscolar`
  - `ListarMallaCurricularPorGrado`
  - `ListarMallaCurricularPorMateria`
- `com.schoolmate.api.usecase.materia`
  - `ActualizarMateria`
  - `CrearMateria`
  - `EliminarMateria`
  - `ListarMaterias`
  - `ObtenerMateria`
- `com.schoolmate.api.usecase.matricula`
  - `CambiarEstadoMatricula`
  - `MatricularAlumno`
  - `ObtenerMatriculasPorAlumno`
  - `ObtenerMatriculasPorCurso`
  - `ValidarAccesoMatriculasCursoProfesor`
- `com.schoolmate.api.usecase.profesor`
  - `ActualizarProfesor`
  - `CrearProfesorConUsuario`
  - `ObtenerClasesHoyProfesor`
  - `ObtenerDetalleProfesor`
  - `ObtenerHorarioProfesor`
  - `ObtenerProfesores`
  - `ObtenerSesionesProfesor`

---

## SECCIÓN 4: MODELO DE DATOS (ENTIDADES JPA)

> Nota: donde hay divergencia entre entidad y migraciones SQL, se marca explícitamente.

### `Usuario` (`usuario`)

| Campo Java | Tipo Java | Columna BD | Tipo BD (migración) | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
| `email` | `String` | `email` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `rut` | `String` | `rut` | `VARCHAR(20)` | nullable, UNIQUE parcial (`rut IS NOT NULL`) |
| `passwordHash` | `String` | `password_hash` | `VARCHAR(255)` | NOT NULL |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` | NOT NULL |
| `apellido` | `String` | `apellido` | `VARCHAR(100)` | NOT NULL |
| `rol` | `Rol` | `rol` | `VARCHAR(20)` | NOT NULL |
| `profesorId` | `UUID` | `profesor_id` | `UUID` | nullable |
| `apoderadoId` | `UUID` | `apoderado_id` | `UUID` | nullable |
| `refreshToken` | `String` | `refresh_token` | `VARCHAR(255)` | nullable, UNIQUE parcial (`refresh_token IS NOT NULL`) |
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
| `id` | `UUID` | `id` | `UUID` | PK |
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
| `id.apoderadoId` | `UUID` | `apoderado_id` | `UUID` | PK compuesta, FK lógica |
| `id.alumnoId` | `UUID` | `alumno_id` | `UUID` | PK compuesta, FK lógica |
| `esPrincipal` | `Boolean` | `es_principal` | `BOOLEAN` | NOT NULL |
| `vinculo` | `VinculoApoderado` | `vinculo` | `VARCHAR(20)` | NOT NULL (`MADRE`,`PADRE`,`TUTOR_LEGAL`,`ABUELO`,`OTRO`) |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |

Relaciones JPA:
- `ManyToOne` hacia `Apoderado` con `@MapsId("apoderadoId")`.
- `ManyToOne` hacia `Alumno` con `@MapsId("alumnoId")`.

Índices: `idx_usuario_email`, `idx_usuario_rol`, `ux_usuario_rut_not_null`.

### `AnoEscolar` (`ano_escolar`)

| Campo Java | Tipo Java | Columna BD | Tipo BD esperado |
|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` |
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

### `DiaNoLectivo` (`dia_no_lectivo`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
| `anoEscolar` | `AnoEscolar` | `ano_escolar_id` | `UUID` | FK NOT NULL |
| `fecha` | `LocalDate` | `fecha` | `DATE` | NOT NULL |
| `tipo` | `TipoDiaNoLectivo` | `tipo` | `VARCHAR(30)` | NOT NULL |
| `descripcion` | `String` | `descripcion` | `VARCHAR(200)` | nullable |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToOne(fetch = LAZY)` -> `AnoEscolar`.

Restricciones esperadas de BD:

- unicidad por año/fecha (`UNIQUE (ano_escolar_id, fecha)`).
- check de `tipo` acotado a valores de `TipoDiaNoLectivo`.

### `Grado` (`grado`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
| `nombre` | `String` | `nombre` | `VARCHAR(50)` | NOT NULL |
| `nivel` | `Integer` | `nivel` | `INTEGER` | NOT NULL |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Índice: `idx_grado_nivel`.

### `Materia` (`materia`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` | NOT NULL |
| `icono` | `String` | `icono` | `VARCHAR(50)` | nullable |
| `activo` | `Boolean` | `activo` | `BOOLEAN` | NOT NULL DEFAULT TRUE |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

### `Profesor` (`profesor`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
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
| `id` | `UUID` | `id` | `UUID` | PK |
| `nombre` | `String` | `nombre` | `VARCHAR(50)` | NOT NULL |
| `letra` | `String` | `letra` | `VARCHAR(5)` (V5), validado a 1 char por V10 | NOT NULL |
| `grado` | `Grado` | `grado_id` | `UUID` | FK NOT NULL |
| `anoEscolar` | `AnoEscolar` | `ano_escolar_id` | `UUID` | FK NOT NULL |
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
| `id` | `UUID` | `id` | `UUID` |
| `rut` | `String` | `rut` | `VARCHAR(20)` UNIQUE |
| `nombre` | `String` | `nombre` | `VARCHAR(100)` |
| `apellido` | `String` | `apellido` | `VARCHAR(100)` |
| `fechaNacimiento` | `LocalDate` | `fecha_nacimiento` | `DATE` |
| `activo` | `Boolean` | `activo` | `BOOLEAN` |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` |

Divergencia/migraciones: `V7__create_alumnos.sql` y `V8__seed_alumnos.sql` están vacíos (tracking), por lo que el DDL exacto de creación inicial no está versionado en Flyway del repo.

### `Matricula` (`matricula`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
| `alumno` | `Alumno` | `alumno_id` | `UUID` | FK NOT NULL |
| `curso` | `Curso` | `curso_id` | `UUID` | FK NOT NULL |
| `anoEscolar` | `AnoEscolar` | `ano_escolar_id` | `UUID` | FK NOT NULL |
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
| `id` | `UUID` | `id` | `UUID` | PK |
| `materia` | `Materia` | `materia_id` | `UUID` | FK NOT NULL |
| `grado` | `Grado` | `grado_id` | `UUID` | FK NOT NULL |
| `anoEscolar` | `AnoEscolar` | `ano_escolar_id` | `UUID` | FK NOT NULL |
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
| `id` | `UUID` | `id` | `UUID` | PK |
| `curso` | `Curso` | `curso_id` | `UUID` | FK NOT NULL |
| `diaSemana` | `Integer` | `dia_semana` | `INTEGER` | CHECK 1..5 |
| `numeroBloque` | `Integer` | `numero_bloque` | `INTEGER` | NOT NULL |
| `horaInicio` | `LocalTime` | `hora_inicio` | `TIME` | NOT NULL |
| `horaFin` | `LocalTime` | `hora_fin` | `TIME` | NOT NULL, > inicio |
| `tipo` | `TipoBloque` | `tipo` | `VARCHAR(20)` | NOT NULL (`CLASE`,`RECREO`,`ALMUERZO`) |
| `profesor` | `Profesor` | `profesor_id` | `UUID` | FK nullable |
| `materia` | `Materia` | `materia_id` | `UUID` | FK nullable |
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

### `AsistenciaClase` (`asistencia_clase`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
| `bloqueHorario` | `BloqueHorario` | `bloque_horario_id` | `UUID` | FK NOT NULL |
| `fecha` | `LocalDate` | `fecha` | `DATE` | NOT NULL |
| `registradoPor` | `Usuario` | `registrado_por_usuario_id` | `UUID` | FK nullable (históricos pre-REQ-A) |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |
| `updatedAt` | `LocalDateTime` | `updated_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToOne` -> `BloqueHorario`
- `@ManyToOne` -> `Usuario` (`registradoPor`, lazy)
- `@OneToMany` -> `RegistroAsistencia` (`cascade = ALL`, `orphanRemoval = true`)

### `SesionUsuario` (`sesion_usuario`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
| `usuario` | `Usuario` | `usuario_id` | `UUID` | FK NOT NULL |
| `ipAddress` | `String` | `ip_address` | `VARCHAR(45)` | nullable |
| `latitud` | `BigDecimal` | `latitud` | `NUMERIC(10,7)` | nullable |
| `longitud` | `BigDecimal` | `longitud` | `NUMERIC(10,7)` | nullable |
| `precisionMetros` | `BigDecimal` | `precision_metros` | `NUMERIC(8,2)` | nullable |
| `userAgent` | `String` | `user_agent` | `VARCHAR(500)` | nullable |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToOne` -> `Usuario`

### `EventoAuditoria` (`evento_auditoria`)

| Campo Java | Tipo Java | Columna BD | Tipo BD | Constraints |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `UUID` | PK |
| `usuario` | `Usuario` | `usuario_id` | `UUID` | FK NOT NULL |
| `usuarioEmail` | `String` | `usuario_email` | `VARCHAR(255)` | NOT NULL (snapshot) |
| `usuarioRol` | `String` | `usuario_rol` | `VARCHAR(20)` | NOT NULL (snapshot) |
| `metodoHttp` | `String` | `metodo_http` | `VARCHAR(10)` | NOT NULL |
| `endpoint` | `String` | `endpoint` | `VARCHAR(500)` | NOT NULL |
| `requestBody` | `String` | `request_body` | `JSONB` | nullable |
| `responseStatus` | `Integer` | `response_status` | `INTEGER` | NOT NULL |
| `ipAddress` | `String` | `ip_address` | `VARCHAR(45)` | nullable |
| `anoEscolarId` | `UUID` | `ano_escolar_id` | `UUID` | nullable (sin FK) |
| `createdAt` | `LocalDateTime` | `created_at` | `TIMESTAMP` | NOT NULL |

Relaciones:

- `@ManyToOne` -> `Usuario` (`LAZY`)
- `requestBody` mapeado como JSONB con `@JdbcTypeCode(SqlTypes.JSON)`.

### Campos de auditoría

Presente en todas las entidades excepto sin `updatedAt` en `SeccionCatalogo`.

- `created_at` + `updated_at`: `Usuario`, `AnoEscolar`, `DiaNoLectivo`, `Grado`, `Materia`, `Profesor`, `Curso`, `Alumno`, `Matricula`, `MallaCurricular`, `BloqueHorario`, `AsistenciaClase`, `RegistroAsistencia`, `Apoderado`.
- Solo `created_at`: `SeccionCatalogo`, `SesionUsuario`, `EventoAuditoria`.

### Enums usados y valores

- `Rol`: `ADMIN`, `PROFESOR`, `APODERADO`
- `EstadoAnoEscolar`: `FUTURO`, `PLANIFICACION`, `ACTIVO`, `CERRADO`
- `EstadoMatricula`: `ACTIVA`, `RETIRADO`, `TRASLADADO`
- `TipoBloque`: `CLASE`, `RECREO`, `ALMUERZO`
- `TipoDiaNoLectivo`: `FERIADO_LEGAL`, `VACACIONES`, `SUSPENSION`, `INTERFERIADO`, `ADMINISTRATIVO`
- `VinculoApoderado`: `MADRE`, `PADRE`, `TUTOR_LEGAL`, `ABUELO`, `OTRO`

### Diagrama ER ASCII (modelo lógico actual)

```text
usuario
  (profesor_id, apoderado_id como UUID sin FK JPA)
   |
   *---1 sesion_usuario
   |
   *---1 evento_auditoria

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

ano_escolar 1---* dia_no_lectivo

curso 1---* bloque_horario *---0..1 profesor
                        |
                        0..1
                      materia

bloque_horario 1---* asistencia_clase *---0..1 usuario (registrado_por_usuario_id)

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
18. `V18__req04_alumno_con_apoderado.sql`
19. `V19__add_refresh_token_to_usuario.sql`
20. `V20__migracion_uuid_nativo.sql` *(aplicada en BD, no versionada en este repo)*
21. `V21__registro_sesiones_y_trazabilidad_asistencia.sql`
22. `V22__audit_trail_operaciones.sql`
23. `V23__consolidacion_esquema_faltante.sql`

Nota de estado:

- `V24` (creación de `dia_no_lectivo`) fue aplicada directamente en Supabase y actualmente no está versionada como archivo Flyway en este repositorio.

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
| `V18` | Migración marcador REQ-04: documenta `ALTER TABLE apoderado_alumno ADD COLUMN vinculo VARCHAR(20) NOT NULL DEFAULT 'OTRO'` ejecutado directamente en Supabase |
| `V19` | Agrega `usuario.refresh_token` (`varchar(255)`) e índice único parcial `ux_usuario_refresh_token_not_null` |
| `V20` | Migración global a UUID nativo en BD (`id`/FKs) para entidades de negocio |
| `V21` | Migración marcador (DDL ejecutado en Supabase): `sesion_usuario` + `asistencia_clase.registrado_por_usuario_id` + índices de trazabilidad |
| `V22` | Migración marcador (DDL ejecutado en Supabase): `evento_auditoria` + FK a `usuario` + índices BTREE/GIN |
| `V23` | Consolidación idempotente de drift: crea tablas faltantes históricas y agrega `ano_escolar.fecha_inicio_planificacion` + FK/índice de `asistencia_clase.registrado_por_usuario_id` |
| `V24` *(manual en Supabase)* | Crea `dia_no_lectivo` con FK a `ano_escolar`, `UNIQUE (ano_escolar_id, fecha)`, checks de tipo e índices por año/fecha |

### Estado resultante del esquema (según migraciones + código)

- Modelo oficial en código y queries usa: `usuario`, `sesion_usuario`, `evento_auditoria`, `apoderado`, `apoderado_alumno`, `ano_escolar`, `dia_no_lectivo`, `grado`, `materia`, `profesor`, `profesor_materia`, `curso`, `seccion_catalogo`, `malla_curricular`, `alumno`, `matricula`, `bloque_horario`, `asistencia_clase`, `registro_asistencia`.
- `apoderado_alumno` incorpora columna `vinculo` en el modelo actual (REQ-04) y es tratada como enum `VinculoApoderado` en JPA.
- IDs/FKs operativos en modelo actual: `UUID` (alineado con V20).
- Drift histórico mitigado por `V23` para bootstrap en ambientes limpios.

### Convenciones para nuevas migraciones

Convenciones observadas:

- Prefijo secuencial `V{n}__descripcion.sql`.
- Uso frecuente de `IF EXISTS / IF NOT EXISTS` para idempotencia parcial.
- Seeds en migración versionada cuando aplica.
- Restricciones en DB para reglas críticas (unicidad parcial/checks).

Recomendación alineada al proyecto:

1. No editar migraciones históricas.
2. Crear nueva `V{n}` disponible para cualquier ajuste de esquema y evitar DDL manual sin marcador Flyway.
3. Incluir `ALTER` explícito para cerrar drift (`ano_escolar`, `alumno`) si existe.

---

## SECCIÓN 6: SEGURIDAD Y AUTENTICACIÓN

### Flujo Access Token + Refresh Token

1. `POST /api/auth/login` recibe `LoginRequest(identificador,password,latitud?,longitud?,precisionMetros?)`.
2. `LoginUsuario`:
   - resuelve usuario por email o RUT normalizado.
   - valida `activo=true`.
   - valida password BCrypt.
   - genera `accessToken` (JWT corto, `jwt.expiration=900000`).
   - genera `refreshToken` aleatorio (`UUID`), lo persiste en `usuario.refresh_token` y sobrescribe el anterior.
3. Respuesta `AuthResponse` con `accessToken`, `refreshToken`, `token` (alias legacy) y metadata de usuario.
4. En login exitoso se registra `SesionUsuario` con:
   - `usuario_id`, `created_at`
   - `ipAddress` (prioridad `X-Forwarded-For` -> `X-Real-IP` -> `remoteAddr`)
   - `userAgent`
   - `latitud/longitud/precisionMetros` (opcionales del request)
5. `POST /api/auth/refresh`:
   - recibe `RefreshTokenRequest {refreshToken}`.
   - busca usuario por `refreshToken`.
   - si no existe: `SESSION_REVOKED` (401).
   - si existe: rota refresh token y retorna nuevo `accessToken` + nuevo `refreshToken`.
6. En cada request autenticada:
   - `JwtAuthenticationFilter` toma `Authorization: Bearer <accessToken>`.
   - valida firma/claims del JWT de forma stateless.
   - construye `UserPrincipal` desde claims del token (sin lookup a BD).
   - setea `SecurityContext` con authorities `ROLE_*`.

### `SecurityConfig`

Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/config/SecurityConfig.java`

- CSRF deshabilitado.
- CORS habilitado por bean.
- Session policy: `STATELESS`.
- `permitAll`:
  - `/api/auth/login`
  - `/api/auth/registro`
  - `/api/auth/refresh`
  - `/h2-console/**`
  - `/error`
- Resto: autenticado (incluye `/api/auth/me`).
- Headers de cache de Spring Security deshabilitados (`headers.cacheControl(cache -> cache.disable())`) para delegar `Cache-Control` al interceptor de aplicación.
- Manejo explícito de excepciones de seguridad:
  - `ApiAuthenticationEntryPoint` -> `UNAUTHORIZED` (401) en JSON.
  - `ApiAccessDeniedHandler` -> `ACCESS_DENIED` (403) en JSON.

### Caching HTTP (ETag + Cache-Control)

Configuración activa en código:

- `CacheConfig`:
  - registra `ShallowEtagHeaderFilter` en patrón `/api/*`.
  - comportamiento efectivo: en `GET` con status `2xx`, genera `ETag` sobre el body y soporta revalidación `If-None-Match` -> `304 Not Modified` cuando coincide.
- `CacheControlInterceptor` (registrado en `WebMvcConfig`):
  - aplica solo en `GET` con status `200`.
  - reglas:
    - `/api/dias-no-lectivos` -> `Cache-Control: max-age=120`.
    - Grupo A/B (`/api/grados`, `/api/materias`, `/api/anos-escolares/**`, `/api/cursos/**`, `/api/malla-curricular`, `/api/profesores`) -> `Cache-Control: no-cache`.
    - Grupo C transaccional (`/api/matriculas/**`, `/api/asistencia/**`, `/api/alumnos/**`) -> `Cache-Control: no-store`.
    - Grupo C sensible (`/api/apoderado/**`, `/api/profesor/**`, `/api/auth/**`, `/api/profesores/{id}/sesiones`, `/api/auditoria/**`) -> `Cache-Control: no-store, private`.
    - default `GET` restante -> `Cache-Control: no-cache`.

### Auditoría automática de operaciones (AOP)

- `AuditoriaAspect` registra eventos de mutación en `evento_auditoria` para respuestas exitosas:
  - pointcut: métodos en `@RestController` anotados con `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`.
  - exclusions por URI: `/api/auth/**`, `/api/dev/**`.
  - datos capturados: usuario autenticado (`UserPrincipal`), método HTTP, endpoint, status response, IP, header `X-Ano-Escolar-Id`, request body (`@RequestBody` serializado JSON).
- Persistencia en transacción separada (`@Transactional(REQUIRES_NEW)`) y manejo defensivo:
  - si falla serialización o guardado, se loguea warning y no se interrumpe la request original.

### Resolución de año escolar por header (`X-Ano-Escolar-Id`)

Infraestructura MVC implementada:

- `AnoEscolarHeaderInterceptor`:
  - corre para `/api/**` (excepto rutas exentas configuradas en `WebMvcConfig`)
  - lee `X-Ano-Escolar-Id` y resuelve `AnoEscolar` por `AnoEscolarRepository.findById(...)`
  - valida acceso por rol:
    - `ADMIN`: cualquier año
    - `PROFESOR` y `APODERADO`: solo año cuyo estado calculado sea `ACTIVO`
  - guarda el objeto resuelto en request attribute `anoEscolarResuelto`
- `AnoEscolarArgumentResolver`:
  - inyecta `AnoEscolar` en parámetros anotados con `@AnoEscolarActivo`
  - si `required=true` y no hay valor resuelto, lanza `VALIDATION_FAILED`
- `@AnoEscolarActivo`:
  - anotación de parámetro para controllers (`required=true` por defecto)
- `WebMvcConfig`:
  - registra interceptor + argument resolver

Importante:

- El header usa el tipo real de `ano_escolar.id` en el modelo actual (`UUID`), por lo que se valida formato UUID.
- Los endpoints migrados usan estrategia gradual: prioridad `header > query/body` para mantener compatibilidad temporal con frontend anterior.

### `JwtAuthenticationFilter`

- Extiende `OncePerRequestFilter`.
- Token válido: construye `UserPrincipal` desde claims JWT y autentica en contexto sin consultar repositorio.
- `ExpiredJwtException`: responde `401` con `ApiErrorResponse {code: TOKEN_EXPIRED}` y corta la cadena.
- `JwtException`/token mal formado o firma inválida: responde `401` con `ApiErrorResponse {code: UNAUTHORIZED}` y corta la cadena.

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

- En `ProfesorHorarioController` (vía `ObtenerHorarioProfesor`), si el rol autenticado es `PROFESOR`, solo puede consultar su propio `profesorId`.
- Si intenta consultar otro `profesorId`, responde `403` (`AccessDeniedException` -> `ACCESS_DENIED`).
- En `AsistenciaController`:
  - `PROFESOR` solo puede registrar/consultar asistencia de bloques donde está asignado y con cierre temporal estricto.
  - `ADMIN` puede registrar/editar asistencia sin restricción de ventana horaria para gestión excepcional.
- En `ApoderadoPortalController`, cada consulta valida vínculo `apoderadoId`->`alumnoId` antes de exponer asistencia o resumen.
- En `JornadaController` (`GET /api/cursos/{cursoId}/jornada`), la validación de ownership de `APODERADO` se ejecuta en `ValidarAccesoJornadaCurso` y exige al menos un alumno con matrícula `ACTIVA` en ese curso.

---

## SECCIÓN 7: ENDPOINTS API (CATÁLOGO COMPLETO)

> Todos los endpoints listados provienen de controladores actuales.

### Dominio: Auth

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/auth/login` | Login y emisión de sesión (`accessToken` + `refreshToken`) por email o RUT; registra sesión de acceso | Público | Body JSON | `LoginRequest {identificador,password,latitud?,longitud?,precisionMetros?}` | `AuthResponse {token,accessToken,refreshToken,tipo,id,email,nombre,apellido,rol,profesorId,apoderadoId}` | `LoginUsuario` | `AUTH_BAD_CREDENTIALS`, `VALIDATION_FAILED` |
| `POST /api/auth/refresh` | Refresca sesión usando refresh token persistido (con rotación) | Público | Body JSON | `RefreshTokenRequest {refreshToken}` | `AuthResponse {token,accessToken,refreshToken,tipo,id,email,nombre,apellido,rol,profesorId,apoderadoId}` | `RefrescarToken` | `SESSION_REVOKED`, `VALIDATION_FAILED` |
| `GET /api/auth/me` | Datos del usuario actual | Autenticado | Header `Authorization` requerido | - | `Map{id,email,nombre,apellido,rol,profesorId,apoderadoId}` | `ObtenerPerfilAutenticado` | `UNAUTHORIZED`, `ACCESS_DENIED` |

### Dominio: Auditoría

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/auditoria` | Consulta paginada de eventos auditados con filtros opcionales por usuario, método, endpoint y rango de fechas | `ADMIN` | Query opcional `usuarioId,metodoHttp,endpoint,desde,hasta,page,size` | - | `EventoAuditoriaPageResponse` | `ConsultarEventosAuditoria` | `ACCESS_DENIED` |

### Dominio: Dashboard

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/dashboard/admin/resumen` | KPIs administrativos base del año escolar activo/resuelto (`alumnos activos`, `cursos activos`, `profesores activos`) | `ADMIN` | Header `X-Ano-Escolar-Id` (resuelto por `@AnoEscolarActivo`) | - | `DashboardAdminResponse` | `ObtenerDashboardAdmin` | `ACCESS_DENIED`, `VALIDATION_FAILED` |

### Dominio: Apoderados (Admin)

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/apoderados` | Crea apoderado + usuario `ROL=APODERADO`, o vincula un apoderado existente por RUT; valida formato/DV de RUT y cross-tabla solo si es persona nueva | `ADMIN` | Body JSON | `ApoderadoRequest` | `ApoderadoResponse` (`201`) | `CrearApoderadoConUsuario` | `RESOURCE_NOT_FOUND`, `CONFLICT`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/apoderados/buscar-por-rut?rut=...` | Busca apoderado por RUT normalizado e incluye lista de alumnos vinculados (`cursoNombre` cuando existe matrícula activa) | `ADMIN` | Query `rut` | - | `ApoderadoBuscarResponse` | `BuscarApoderadoPorRut` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE` |
| `GET /api/apoderados/por-alumno/{alumnoId}` | Retorna apoderado vinculado al alumno (MVP: primer vínculo) | `ADMIN` | Path `alumnoId` | - | `ApoderadoResponse` o `204` | `ObtenerApoderadoPorAlumno` | `RESOURCE_NOT_FOUND` |

### Dominio: Portal Apoderado

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/apoderado/mis-alumnos` | Lista hijos vinculados al apoderado autenticado, con matrícula activa del año activo si existe | `APODERADO` | `Authorization` | - | `List<AlumnoApoderadoResponse>` | `ObtenerAlumnosApoderado` | `ACCESS_DENIED` (si principal inválido) |
| `GET /api/apoderado/alumnos/{alumnoId}/asistencia/mensual?mes=...&anio=...` | Asistencia diaria agregada por bloques para un mes (`PRESENTE/AUSENTE/PARCIAL`) + lista `diasNoLectivos` del período | `APODERADO` (ownership) | Path `alumnoId`, Query `mes,anio` | - | `AsistenciaMensualResponse` | `ObtenerAsistenciaMensualAlumno` | `ACCESS_DENIED`, `RESOURCE_NOT_FOUND`, `BUSINESS_RULE` |
| `GET /api/apoderado/alumnos/{alumnoId}/asistencia/resumen` | Resumen anual de asistencia por bloques con porcentaje (prioridad `header > query`) | `APODERADO` (ownership) | Path `alumnoId`, Header `X-Ano-Escolar-Id` o Query `anoEscolarId` (fallback) | - | `ResumenAsistenciaResponse` | `ObtenerResumenAsistenciaAlumno` | `ACCESS_DENIED`, `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |

### Dominio: DevTools (solo perfil `dev`)

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/dev/clock` | Retorna fecha/hora actual del clock centralizado y si está overrideado | según config de seguridad vigente | - | - | `{currentDateTime,isOverridden}` | directo (`ClockProvider`) | - |
| `POST /api/dev/clock` | Fija el clock a una fecha/hora ISO-8601 | según config de seguridad vigente | Body JSON | `{dateTime}` | `{currentDateTime,isOverridden}` | directo (`ClockProvider`) | `BUSINESS_RULE` por formato inválido, `INTERNAL_SERVER_ERROR`/`UnsupportedOperationException` fuera de `dev` |
| `DELETE /api/dev/clock` | Resetea clock a tiempo del sistema | según config de seguridad vigente | - | - | `{currentDateTime,isOverridden}` | directo (`ClockProvider`) | `INTERNAL_SERVER_ERROR`/`UnsupportedOperationException` fuera de `dev` |

### Dominio: Años Escolares

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/anos-escolares` | Lista años con estado calculado | `ADMIN` | - | - | `List<AnoEscolarResponse>` | `ListarAnosEscolares` | `ACCESS_DENIED` |
| `GET /api/anos-escolares/{id}` | Obtiene año por id | `ADMIN` | Path `id` | - | `AnoEscolarResponse` | `ObtenerAnoEscolar` | `RESOURCE_NOT_FOUND` |
| `GET /api/anos-escolares/activo` | Año activo para fecha actual | autenticado | - | - | `AnoEscolarResponse` | `ObtenerAnoEscolarActivo` | `RESOURCE_NOT_FOUND` |
| `POST /api/anos-escolares` | Crea año | `ADMIN` | Body | `AnoEscolarRequest {ano,fechaInicioPlanificacion,fechaInicio,fechaFin}` | `AnoEscolarResponse` | `CrearAnoEscolar` | `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `PUT /api/anos-escolares/{id}` | Actualiza año | `ADMIN` | Path `id` + Body | `AnoEscolarRequest` | `AnoEscolarResponse` | `ActualizarAnoEscolar` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |

### Dominio: Días No Lectivos

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/dias-no-lectivos` | Lista días no lectivos del año escolar resuelto por header; filtro opcional por `mes` y `anio` | autenticado | Header requerido `X-Ano-Escolar-Id`, Query opcional `mes,anio` | - | `List<DiaNoLectivoResponse>` | `ListarDiasNoLectivos` | `VALIDATION_FAILED`, `BUSINESS_RULE` |
| `POST /api/dias-no-lectivos` | Crea días no lectivos por rango (`fechaInicio`..`fechaFin`) | `ADMIN` | Header `X-Ano-Escolar-Id`, Body | `CrearDiaNoLectivoRequest` | `List<DiaNoLectivoResponse>` (`201`) | `CrearDiasNoLectivos` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `DELETE /api/dias-no-lectivos/{id}` | Elimina un día no lectivo | `ADMIN` | Path `id` | - | `204` | `EliminarDiaNoLectivo` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE` |

### Dominio: Grados

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/grados` | Lista grados por nivel | `ADMIN` | - | - | `List<GradoResponse>` | `ListarGrados` | `ACCESS_DENIED` |
| `GET /api/grados/{id}` | Obtiene grado por id | `ADMIN` | Path `id` | - | `GradoResponse` | `ObtenerGrado` | `RESOURCE_NOT_FOUND` |

### Dominio: Materias

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/materias` | Lista paginada y ordenable | `ADMIN` | Query: `page,size,sortBy,sortDir` | - | `MateriaPageResponse` | `ListarMaterias` | `ACCESS_DENIED` |
| `GET /api/materias/{id}` | Obtiene materia | `ADMIN` | Path `id` | - | `MateriaResponse` | `ObtenerMateria` | `RESOURCE_NOT_FOUND` |
| `POST /api/materias` | Crea materia | `ADMIN` | Body | `MateriaRequest {nombre,icono}` | `MateriaResponse` | `CrearMateria` | `VALIDATION_FAILED` |
| `PUT /api/materias/{id}` | Actualiza materia | `ADMIN` | Path + Body | `MateriaRequest` | `MateriaResponse` | `ActualizarMateria` | `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |
| `DELETE /api/materias/{id}` | Elimina materia (físico) | `ADMIN` | Path | - | `204` | `EliminarMateria` | `RESOURCE_NOT_FOUND`, `DATA_INTEGRITY` |

### Dominio: Malla Curricular

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/malla-curricular` | Lista malla activa por año (prioridad `header > query`), paginada | `ADMIN` | Header `X-Ano-Escolar-Id` o Query `anoEscolarId` (fallback, obligatorio al menos uno) + Query `page,size` | - | `MallaCurricularPageResponse` | `ListarMallaCurricularPorAnoEscolar` | `ACCESS_DENIED`, `VALIDATION_FAILED` |
| `GET /api/malla-curricular/materia/{materiaId}` | Lista malla por materia y año (prioridad `header > query`), paginada | `ADMIN` | Path + Header `X-Ano-Escolar-Id` o Query `anoEscolarId` (fallback) + Query `page,size` | - | `MallaCurricularPageResponse` | `ListarMallaCurricularPorMateria` | `VALIDATION_FAILED` |
| `GET /api/malla-curricular/grado/{gradoId}` | Lista malla por grado y año (prioridad `header > query`), paginada | `ADMIN` | Path + Header `X-Ano-Escolar-Id` o Query `anoEscolarId` (fallback) + Query `page,size` | - | `MallaCurricularPageResponse` | `ListarMallaCurricularPorGrado` | `VALIDATION_FAILED` |
| `POST /api/malla-curricular` | Crea registro malla (prioridad `header > body`) y bloquea escritura en año `CERRADO` | `ADMIN` | Header `X-Ano-Escolar-Id` o Body | `MallaCurricularRequest {materiaId,gradoId,anoEscolarId?,horasPedagogicas}` | `MallaCurricularResponse` | `CrearMallaCurricular` | `409` conflict (duplicate), `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`, `BUSINESS_RULE` |
| `PUT /api/malla-curricular/{id}` | Actualiza horas/activo | `ADMIN` | Path + Body | `MallaCurricularUpdateRequest {horasPedagogicas,activo}` | `MallaCurricularResponse` | `ActualizarMallaCurricular` | `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED` |
| `POST /api/malla-curricular/bulk` | Upsert masivo por materia-año (prioridad `header > body`) y bloquea escritura en año `CERRADO` | `ADMIN` | Header `X-Ano-Escolar-Id` o Body | `MallaCurricularBulkRequest` | `List<MallaCurricularResponse>` | `GuardarMallaCurricularBulk` | `BAD_REQUEST` grados duplicados, `RESOURCE_NOT_FOUND`, `VALIDATION_FAILED`, `BUSINESS_RULE` |
| `DELETE /api/malla-curricular/{id}` | Baja lógica (`activo=false`) | `ADMIN` | Path | - | `204` | `EliminarMallaCurricular` | `RESOURCE_NOT_FOUND` |

### Dominio: Cursos

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/cursos` | Lista cursos (filtro opcional por año/grado) + matriculados (prioridad `header > query`), paginada y ordenable | `ADMIN` | Header opcional `X-Ano-Escolar-Id`; Query opcional `anoEscolarId`,`gradoId`,`page`,`size`,`sortBy`,`sortDir` | - | `CursoPageResponse` | `ObtenerCursos` | - |
| `GET /api/cursos/{id}` | Detalle enriquecido (malla + conteos) | `ADMIN` | Path `id` | - | `CursoResponse` con `materias`, `cantidadMaterias`, `totalHorasPedagogicas`, `alumnosMatriculados` | `ObtenerDetalleCurso` | `RESOURCE_NOT_FOUND` |
| `POST /api/cursos` | Crea curso con letra automática (prioridad `header > body`) y bloquea escritura en año `CERRADO` | `ADMIN` | Header `X-Ano-Escolar-Id` o Body | `CursoRequest {gradoId,anoEscolarId?}` | `CursoResponse` | `CrearCurso` | `RESOURCE_NOT_FOUND`, `CURSO_SIN_SECCION_DISPONIBLE`, `VALIDATION_FAILED`, `BUSINESS_RULE` |
| `PUT /api/cursos/{id}` | Reasigna curso (recalcula letra si cambia grado/año, prioridad `header > body`) | `ADMIN` | Path + Header `X-Ano-Escolar-Id` o Body | `CursoRequest` | `CursoResponse` | `ActualizarCurso` | `RESOURCE_NOT_FOUND`, `CURSO_SIN_SECCION_DISPONIBLE`, `VALIDATION_FAILED` |

### Dominio: Profesores

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/profesores` | Lista profesores, paginada y ordenable | `ADMIN` | Query `page,size,sortBy,sortDir` | - | `ProfesorPageResponse` | `ObtenerProfesores` | - |
| `GET /api/profesores/{id}` | Obtiene profesor con contrato y horas asignadas del año ACTIVO (si existe) | `ADMIN` | Path | - | `ProfesorResponse` | `ObtenerDetalleProfesor` | `RESOURCE_NOT_FOUND` |
| `GET /api/profesores/{profesorId}/sesiones` | Lista sesiones de login del usuario asociado al profesor (paginado, filtro por fechas) | `ADMIN` | Path `profesorId`; Query opcional `desde`,`hasta`,`page`,`size` | - | `SesionProfesorPageResponse` | `ObtenerSesionesProfesor` | `RESOURCE_NOT_FOUND` |
| `POST /api/profesores` | Crea profesor con materias, horas de contrato y usuario asociado (`ROL=PROFESOR`); valida formato/DV de RUT y disponibilidad cross-tabla | `ADMIN` | Body | `ProfesorRequest` | `ProfesorResponse` | `CrearProfesorConUsuario` | `PROFESOR_RUT_DUPLICADO`, `PROFESOR_EMAIL_DUPLICADO`, `PROFESOR_TELEFONO_DUPLICADO`, `MATERIAS_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `PUT /api/profesores/{id}` | Actualiza profesor; valida formato de RUT, mantiene RUT inmutable y permite setear/limpiar horas de contrato | `ADMIN` | Path + Body | `ProfesorRequest` | `ProfesorResponse` | `ActualizarProfesor` | `RESOURCE_NOT_FOUND`, `PROFESOR_RUT_INMUTABLE`, duplicados, `MATERIAS_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/profesores/{profesorId}/horario` | Horario semanal consolidado por profesor y año escolar (solo bloques CLASE con materia, prioridad `header > query`) | `ADMIN`,`PROFESOR` (ownership) | Path `profesorId` + Header `X-Ano-Escolar-Id` o Query `anoEscolarId` (fallback) | - | `ProfesorHorarioResponse` | `ObtenerHorarioProfesor` | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED`, `VALIDATION_FAILED` |
| `GET /api/profesor/mis-clases-hoy` | Clases del día para el profesor autenticado (`estado` por ventana de 15 min, `asistenciaTomada` real por bloque+fecha) con `diaNoLectivo` cuando aplica | `PROFESOR` | `Authorization` (usa `profesorId` del JWT) | - | `ClasesHoyResponse` | `ObtenerClasesHoyProfesor` | `ACCESS_DENIED` |

### Dominio: Alumnos

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `GET /api/alumnos` | Lista paginada con búsqueda y enriquecimiento opcional por matrícula (prioridad `header > query`) | `ADMIN` | Query: `page,size,sortBy,sortDir,anoEscolarId,cursoId,gradoId,q` + Header opcional `X-Ano-Escolar-Id` | - | `AlumnoPageResponse` | directo + `AlumnoSpecifications` | - |
| `GET /api/alumnos/{id}` | Obtiene alumno; opcional matrícula activa por año y apoderado principal vinculado (si existe), prioridad `header > query` | `ADMIN` | Path + Header opcional `X-Ano-Escolar-Id` + Query opcional `anoEscolarId` | - | `AlumnoResponse` | directo | `RESOURCE_NOT_FOUND` |
| `GET /api/alumnos/buscar-por-rut` | Búsqueda exacta por RUT + matrícula opcional por año, prioridad `header > query` | `ADMIN` | Query: `rut` (obligatorio), `anoEscolarId` (opcional) + Header opcional `X-Ano-Escolar-Id` | - | `AlumnoResponse` | directo | `RESOURCE_NOT_FOUND` |
| `POST /api/alumnos` | Crea alumno; valida formato/DV de RUT y disponibilidad cross-tabla antes de la unicidad interna de alumno | `ADMIN` | Body | `AlumnoRequest` | `AlumnoResponse` | directo | `CONFLICT`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `POST /api/alumnos/con-apoderado` | Crea alumno y vincula/crea apoderado en una transacción; valida formato de ambos RUT y cross-tabla (apoderado solo si es nuevo) | `ADMIN` | Body | `CrearAlumnoConApoderadoRequest` | `AlumnoResponse` (`201`) | `CrearAlumnoConApoderado` | `CONFLICT`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `PUT /api/alumnos/{id}` | Actualiza alumno; valida formato/DV de RUT y disponibilidad cross-tabla antes de la unicidad interna | `ADMIN` | Path + Body | `AlumnoRequest` | `AlumnoResponse` | directo | `RESOURCE_NOT_FOUND`, `CONFLICT`, `BUSINESS_RULE`, `VALIDATION_FAILED` |

### Dominio: Matrículas

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/matriculas` | Matricula alumno en curso/año (prioridad `header > body`); bloquea creación si el año escolar está `CERRADO` | `ADMIN` | Header `X-Ano-Escolar-Id` o Body | `MatriculaRequest {alumnoId,cursoId,anoEscolarId?,fechaMatricula?}` | `MatriculaResponse` | `MatricularAlumno` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/matriculas/curso/{cursoId}` | Matrículas activas por curso, paginadas y ordenables | `ADMIN`,`PROFESOR` (ownership por curso en año activo) | Path `cursoId` + Query `page,size,sortBy,sortDir` | - | `MatriculaPageResponse` | `ObtenerMatriculasPorCurso` + `ValidarAccesoMatriculasCursoProfesor` | `ACCESS_DENIED` |
| `GET /api/matriculas/alumno/{alumnoId}` | Historial de matrículas por alumno, paginado y ordenable | `ADMIN` | Path `alumnoId` + Query `page,size,sortBy,sortDir` | - | `MatriculaPageResponse` | `ObtenerMatriculasPorAlumno` | - |
| `PATCH /api/matriculas/{id}/estado` | Cambia estado (`ACTIVA/RETIRADO/TRASLADADO`) | `ADMIN` | Path + Body | `CambiarEstadoMatriculaRequest {estado}` | `MatriculaResponse` | `CambiarEstadoMatricula` | `VALIDATION_FAILED`, `RESOURCE_NOT_FOUND`, `BUSINESS_RULE` |

### Dominio: Asistencia

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `POST /api/asistencia/clase` | Registra o edita asistencia de un bloque CLASE. `PROFESOR`: cierre estricto (`fecha==hoy` + ventana ±15 min). `ADMIN`: bypass temporal para gestión excepcional. Bloquea fechas no lectivas | `PROFESOR`,`ADMIN` | Body | `GuardarAsistenciaRequest {bloqueHorarioId,fecha,registros[]}` | `AsistenciaClaseResponse` (`201 Created`) | `GuardarAsistenciaClase` | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED`, `ASISTENCIA_CERRADA`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/asistencia/clase?bloqueHorarioId=...&fecha=...` | Obtiene asistencia registrada para un bloque y fecha | `PROFESOR` (ownership por bloque) | Query `bloqueHorarioId`,`fecha` | - | `AsistenciaClaseResponse` | `ObtenerAsistenciaClase` | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED` |

### Dominio: Jornada

| Método + URL | Descripción | Roles | Parámetros | Request DTO | Response DTO | UseCase/CRUD | Errores específicos |
|---|---|---|---|---|---|---|---|
| `PUT /api/cursos/{cursoId}/jornada/{diaSemana}` | Guarda/reemplaza la jornada de un día | `ADMIN` | Path `cursoId`,`diaSemana`; Body | `JornadaDiaRequest` | `JornadaDiaResponse` | `GuardarJornadaDia` | `RESOURCE_NOT_FOUND`, `BUSINESS_RULE`, `VALIDATION_FAILED` |
| `GET /api/cursos/{cursoId}/jornada` | Obtiene jornada completa; opcionalmente filtra por día | `ADMIN`,`APODERADO` (ownership por matrícula activa en el curso) | Path `cursoId`; Query opcional `diaSemana` | - | `JornadaCursoResponse` | `ObtenerJornadaCurso` + `ValidarAccesoJornadaCurso` | `RESOURCE_NOT_FOUND`, `ACCESS_DENIED` |
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

- Función: registrar o editar asistencia de una clase con reglas temporales por rol.
- Repositorios/dependencias:
  - `BloqueHorarioRepository`, `MatriculaRepository`, `AsistenciaClaseRepository`, `RegistroAsistenciaRepository`, `DiaNoLectivoRepository`, `UsuarioRepository`, `ClockProvider`
- Firma actual:
  - `execute(GuardarAsistenciaRequest request, UUID profesorId, UUID usuarioId, Rol rolUsuario)`
- Validaciones generales:
  - bloque existe y es tipo `CLASE`
  - no permite sábados ni domingos
  - fecha debe corresponder al `diaSemana` del bloque
  - no permite fechas marcadas en `dia_no_lectivo` para el año escolar del curso
  - el año escolar del curso no puede estar `CERRADO`
  - la fecha debe estar dentro de `[fechaInicio, fechaFin]` del año escolar
  - todos los alumnos del request deben estar con matrícula `ACTIVA` en el curso
  - no se aceptan alumnos duplicados en el request
- Reglas por rol:
  - `PROFESOR`:
    - `fechaRequest` debe ser exactamente `clockProvider.today()`
    - hora actual debe estar dentro de `[horaInicio-15min, horaFin+15min]`
    - si falla cualquiera de ambas, lanza `ApiException(ErrorCode.ASISTENCIA_CERRADA, ...)`
    - ownership: el bloque debe pertenecer al profesor autenticado
  - `ADMIN`:
    - bypass de cierre temporal (puede registrar/editar fuera de la ventana)
    - bypass de ownership de bloque para gestión excepcional
- Comportamiento de persistencia:
  - si no existe asistencia para `(bloque,fechaRequest)`, crea `asistencia_clase`
  - si ya existe, actualiza metadata (`updatedAt`, `registradoPor`)
  - conciliación in-place de `registro_asistencia` por `alumnoId`:
    - elimina huérfanos de la colección (`orphanRemoval`)
    - actualiza existentes (`estado`, `observacion`, `updatedAt`)
    - inserta solo nuevos alumnos
  - preserva UUID de registros existentes (sin `DELETE + INSERT` masivo)
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
  - response incluye `registradoPorNombre` (nullable para históricos)
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.auth.LoginUsuario`

- Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/usecase/auth/LoginUsuario.java`
- Función: autenticar usuario y emitir sesión (`accessToken` + `refreshToken` persistido).
- Repositorios/dependencias:
  - `UsuarioRepository`
  - `SesionUsuarioRepository`
  - `PasswordEncoder`
  - `JwtTokenProvider`
  - `ClockProvider`
- Validaciones:
  - usuario existe por `identificador` (email o RUT normalizado)
  - usuario activo
  - password BCrypt coincide
- Flujo `execute()`:
  - Firma actual: `execute(LoginRequest request, HttpServletRequest httpRequest)`
  1. resuelve usuario por `identificador`:
     - email (`findByEmail`) o
     - RUT (`RutNormalizer.normalize` + `findByRut`)
  2. valida `activo`
  3. valida password
  4. construye `UserPrincipal`
  5. genera `accessToken` (JWT)
  6. genera `refreshToken` (`UUID`) y persiste en `usuario.refreshToken`
  7. registra `SesionUsuario` con ip/user-agent/geolocalización opcional
  8. retorna `AuthResponse` con `token` (alias), `accessToken` y `refreshToken`
- Errores:
  - `BadCredentialsException` (credenciales inválidas / usuario desactivado)
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.auth.RefrescarToken`

- Archivo: `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/usecase/auth/RefrescarToken.java`
- Función: renovar sesión usando refresh token persistido con rotación.
- Repositorios/dependencias:
  - `UsuarioRepository`
  - `JwtTokenProvider`
- Flujo `execute()`:
  1. busca usuario por `refreshToken` (`findByRefreshToken`)
  2. si no existe o usuario inactivo, lanza `ApiException(SESSION_REVOKED)`
  3. rota refresh token (`UUID`) y persiste usuario
  4. genera nuevo `accessToken` JWT y retorna `AuthResponse`
- Errores:
  - `ApiException(SESSION_REVOKED)`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.auth.ObtenerPerfilAutenticado`

- Función: resolver el perfil autenticado para `GET /api/auth/me`.
- Repositorios/dependencias:
  - no usa repositorios (opera con `UserPrincipal` del contexto de seguridad).
- Reglas:
  - rechaza principal nulo con `ApiException(UNAUTHORIZED)`.
  - retorna mapa con `id,email,nombre,apellido,rol,profesorId,apoderadoId`.
- Errores:
  - `ApiException(UNAUTHORIZED)`.

### `com.schoolmate.api.usecase.auditoria.ConsultarEventosAuditoria`

- Función: consultar auditoría paginada con filtros opcionales y deserialización segura de `requestBody`.
- Repositorios/dependencias:
  - `EventoAuditoriaRepository`, `ObjectMapper`
- Reglas:
  - normaliza `metodoHttp` a mayúsculas.
  - aplica filtros opcionales con flags booleanos para evitar problemas de tipado SQL en PostgreSQL.
  - usa `desde` inclusivo y `hasta` exclusivo (`+1 día`).
  - deserializa `requestBody` JSON string a objeto estructurado; si falla, retorna string raw.
- Salida:
  - `EventoAuditoriaPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.anoescolar.ListarAnosEscolares`

- Función: listar años escolares en orden descendente con estado calculado.
- Repositorios/dependencias:
  - `AnoEscolarRepository`, `ClockProvider`
- Reglas:
  - usa `findAllByOrderByAnoDesc()`.
  - calcula estado con `AnoEscolar.calcularEstado(clockProvider.today())`.
- Salida:
  - `List<AnoEscolarResponse>`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.anoescolar.ObtenerAnoEscolar`

- Función: obtener un año escolar por id con estado calculado.
- Repositorios/dependencias:
  - `AnoEscolarRepository`, `ClockProvider`
- Reglas:
  - valida existencia por id.
  - calcula estado según fecha actual del `ClockProvider`.
- Salida:
  - `AnoEscolarResponse`.
- Errores:
  - `ResourceNotFoundException("Año escolar no encontrado")`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.anoescolar.ObtenerAnoEscolarActivo`

- Función: obtener año escolar activo para la fecha actual.
- Repositorios/dependencias:
  - `AnoEscolarRepository`, `ClockProvider`
- Reglas:
  - busca por rango inclusivo `fechaInicio <= hoy <= fechaFin`.
  - calcula estado sobre la misma fecha de referencia.
- Salida:
  - `AnoEscolarResponse`.
- Errores:
  - `ResourceNotFoundException("No hay año escolar activo para la fecha actual")`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.anoescolar.CrearAnoEscolar`

- Función: crear un año escolar validando consistencia temporal y solapamientos.
- Repositorios/dependencias:
  - `AnoEscolarRepository`, `ClockProvider`
- Reglas:
  - bloquea duplicados por `ano`.
  - valida orden `fechaInicioPlanificacion < fechaInicio < fechaFin`.
  - valida `request.ano == request.fechaInicio.year`.
  - valida que no se solape con otros años existentes mediante `existsSolapamiento(...)` en BD.
  - bloquea creación con `fechaFin` en el pasado.
- Salida:
  - `AnoEscolarResponse`.
- Errores:
  - `BusinessException`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.anoescolar.ActualizarAnoEscolar`

- Función: actualizar un año escolar existente aplicando reglas de consistencia.
- Repositorios/dependencias:
  - `AnoEscolarRepository`, `ClockProvider`
- Reglas:
  - valida existencia por id.
  - no permite modificar un año en estado `CERRADO`.
  - valida orden de fechas, coherencia de año y no solapamiento por query de existencia (`existsSolapamientoExcluyendoId(...)`).
- Salida:
  - `AnoEscolarResponse`.
- Errores:
  - `ResourceNotFoundException`, `BusinessException`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.grado.ListarGrados`

- Función: listar grados en orden ascendente por nivel.
- Repositorios/dependencias:
  - `GradoRepository`
- Salida:
  - `List<Grado>`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.grado.ObtenerGrado`

- Función: obtener un grado por `id`.
- Repositorios/dependencias:
  - `GradoRepository`
- Reglas:
  - valida existencia por id.
- Salida:
  - `Grado`.
- Errores:
  - `ResourceNotFoundException("Grado no encontrado")`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.curso.ObtenerCursos`

- Función: listar cursos con filtros opcionales por año/grado, paginación/orden y enriquecer con total de matrículas activas.
- Repositorios/dependencias:
  - `CursoRepository`, `MatriculaRepository`
- Reglas:
  - resuelve prioridad de año escolar `header > query`.
  - normaliza paginación (`page >= 0`, `size` en rango `1..100`).
  - normaliza orden:
    - `sortBy` permitido: `nombre`, `letra`, `createdAt`, `updatedAt` (fallback `nombre`).
    - `sortDir` permitido: `asc|desc` (fallback `asc`).
  - si hay año+grado: consulta por ambos.
  - si hay solo año: consulta por año.
  - si no hay año: lista global.
  - calcula `alumnosMatriculados` con query agregada `countActivasByCursoIds(...)`.
- Salida:
  - `CursoPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.curso.ObtenerDetalleCurso`

- Función: devolver detalle enriquecido de un curso.
- Repositorios/dependencias:
  - `CursoRepository`, `MatriculaRepository`, `MallaCurricularRepository`
- Reglas:
  - carga curso con `grado` y `anoEscolar`.
  - cuenta matrículas activas del curso.
  - obtiene malla activa de `grado + año`.
  - ordena materias por nombre (case-insensitive).
  - calcula `cantidadMaterias` y `totalHorasPedagogicas`.
- Errores:
  - `ResourceNotFoundException("Curso no encontrado")`
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.curso.CrearCurso`

- Función: crear curso con letra automática disponible.
- Repositorios/dependencias:
  - `CursoRepository`, `GradoRepository`, `AnoEscolarRepository`, `SeccionCatalogoRepository`, `ClockProvider`
- Reglas:
  - resuelve prioridad `header > body` para `anoEscolarId`.
  - valida que exista `grado` y `anoEscolar`.
  - bloquea escritura si `anoEscolar` está `CERRADO`.
  - selecciona primera letra libre desde `seccion_catalogo` (orden ascendente).
  - delega armado de identidad académica en `Curso.actualizarIdentidadAcademica(...)`.
  - tras guardar, recarga curso con `grado` y `anoEscolar` usando `findByIdWithGradoAndAnoEscolar`.
- Salida:
  - `CursoResponse`.
- Errores:
  - `ResourceNotFoundException`, `ApiException(CURSO_SIN_SECCION_DISPONIBLE)`, `ApiException(VALIDATION_FAILED)`, `ApiException(BUSINESS_RULE)`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.curso.ActualizarCurso`

- Función: reasignar curso (grado/año) recalculando letra cuando cambia la asignación.
- Repositorios/dependencias:
  - `CursoRepository`, `GradoRepository`, `AnoEscolarRepository`, `SeccionCatalogoRepository`
- Reglas:
  - resuelve prioridad `header > body` para `anoEscolarId`.
  - valida existencia de curso, grado y año.
  - si mantiene mismo `grado + año`, conserva la letra actual.
  - si cambia asignación, toma primera letra disponible del catálogo.
  - actualiza identidad del curso vía `Curso.actualizarIdentidadAcademica(...)`.
  - tras guardar, recarga curso con `grado` y `anoEscolar` usando `findByIdWithGradoAndAnoEscolar`.
- Salida:
  - `CursoResponse`.
- Errores:
  - `ResourceNotFoundException`, `ApiException(CURSO_SIN_SECCION_DISPONIBLE)`, `ApiException(VALIDATION_FAILED)`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.materia.ListarMaterias`

- Función: listar materias paginadas con ordenamiento controlado.
- Repositorios/dependencias:
  - `MateriaRepository`
- Reglas:
  - normaliza paginación (`page >= 0`, `size` en rango `1..100`).
  - whitelistea campos de orden (`nombre`, `createdAt`, `updatedAt`, `id`).
  - normaliza dirección (`asc|desc`, default `desc`).
- Salida:
  - `MateriaPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.materia.ObtenerMateria`

- Función: obtener una materia por `id`.
- Repositorios/dependencias:
  - `MateriaRepository`
- Reglas:
  - valida existencia por id.
- Salida:
  - `MateriaResponse`.
- Errores:
  - `ResourceNotFoundException("Materia no encontrada")`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.materia.CrearMateria`

- Función: crear una materia.
- Repositorios/dependencias:
  - `MateriaRepository`
- Salida:
  - `MateriaResponse`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.materia.ActualizarMateria`

- Función: actualizar nombre/icono de una materia existente.
- Repositorios/dependencias:
  - `MateriaRepository`
- Reglas:
  - valida existencia por id.
- Salida:
  - `MateriaResponse`.
- Errores:
  - `ResourceNotFoundException("Materia no encontrada")`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.materia.EliminarMateria`

- Función: eliminar físicamente una materia por id.
- Repositorios/dependencias:
  - `MateriaRepository`
- Reglas:
  - valida existencia por id antes de eliminar.
- Errores:
  - `ResourceNotFoundException("Materia no encontrada")`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.malla.ListarMallaCurricularPorAnoEscolar`

- Función: listar malla activa por año escolar (`header > query`), paginada y ordenada por grado y materia.
- Repositorios/dependencias:
  - `MallaCurricularRepository`
- Reglas:
  - requiere `anoEscolarId` resuelto desde header o query.
  - normaliza paginación (`page >= 0`, `size` en rango `1..100`).
  - orden fijo: `grado.nivel asc`, `materia.nombre asc`.
- Salida:
  - `MallaCurricularPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.malla.ListarMallaCurricularPorMateria`

- Función: listar malla por `materiaId` y año escolar (`header > query`), paginada.
- Repositorios/dependencias:
  - `MallaCurricularRepository`
- Reglas:
  - requiere `anoEscolarId` resuelto desde header o query.
  - normaliza paginación (`page >= 0`, `size` en rango `1..100`).
  - orden fijo: `grado.nivel asc`.
- Salida:
  - `MallaCurricularPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.malla.ListarMallaCurricularPorGrado`

- Función: listar malla por `gradoId` y año escolar (`header > query`), paginada.
- Repositorios/dependencias:
  - `MallaCurricularRepository`
- Reglas:
  - requiere `anoEscolarId` resuelto desde header o query.
  - normaliza paginación (`page >= 0`, `size` en rango `1..100`).
  - orden fijo: `materia.nombre asc`.
- Salida:
  - `MallaCurricularPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.malla.CrearMallaCurricular`

- Función: crear registro de malla curricular.
- Repositorios/dependencias:
  - `MallaCurricularRepository`, `MateriaRepository`, `GradoRepository`, `AnoEscolarRepository`, `ClockProvider`
- Reglas:
  - resuelve prioridad `header > body` para `anoEscolarId`.
  - valida duplicado por combinación `(materiaId, gradoId, anoEscolarId)` (409).
  - valida existencia de materia/grado/año.
  - bloquea escritura si el año escolar está `CERRADO`.
- Salida:
  - `MallaCurricularResponse`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.malla.ActualizarMallaCurricular`

- Función: actualizar `horasPedagogicas` y `activo` de un registro de malla.
- Repositorios/dependencias:
  - `MallaCurricularRepository`
- Reglas:
  - valida existencia por `id`.
- Salida:
  - `MallaCurricularResponse`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.malla.GuardarMallaCurricularBulk`

- Función: upsert masivo de malla por materia-año, con desactivación de grados no enviados.
- Repositorios/dependencias:
  - `MallaCurricularRepository`, `MateriaRepository`, `GradoRepository`, `AnoEscolarRepository`, `ClockProvider`
- Reglas:
  - resuelve prioridad `header > body` para `anoEscolarId`.
  - valida existencia de materia/año y bloqueo por año `CERRADO`.
  - valida que no haya `gradoId` duplicados en el request (400).
  - reactiva/actualiza existentes, crea nuevos y desactiva ausentes (`activo=false`).
- Salida:
  - `List<MallaCurricularResponse>`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.malla.EliminarMallaCurricular`

- Función: baja lógica de malla (`activo=false`).
- Repositorios/dependencias:
  - `MallaCurricularRepository`
- Reglas:
  - valida existencia por `id`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.dashboard.ObtenerDashboardAdmin`

- Función: consolidar KPIs administrativos base para el dashboard.
- Repositorios/dependencias:
  - `MatriculaRepository`, `CursoRepository`, `ProfesorRepository`
- Cálculo:
  - `totalAlumnos`: conteo de matrículas `ACTIVA` para el `anoEscolarId` consultado.
  - `totalCursos`: conteo de cursos `activo=true` para el `anoEscolarId`.
  - `totalProfesores`: conteo global de profesores `activo=true`.
- Salida:
  - `DashboardAdminResponse {totalAlumnos,totalCursos,totalProfesores}`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.calendario.CrearDiasNoLectivos`

- Función: registrar días no lectivos por rango para un año escolar.
- Repositorios/dependencias:
  - `AnoEscolarRepository`, `DiaNoLectivoRepository`, `ClockProvider`
- Reglas:
  - el año escolar debe existir y no estar `CERRADO`
  - `fechaFin >= fechaInicio`
  - rango máximo permitido: 60 días corridos
  - excluye sábados y domingos del alta
  - si el rango no deja días hábiles, retorna error
  - cada fecha debe estar dentro de `[anoEscolar.fechaInicio, anoEscolar.fechaFin]`
  - no permite duplicado por `(anoEscolarId, fecha)`
- Resultado:
  - guarda una entidad por cada fecha hábil válida y retorna `List<DiaNoLectivoResponse>`.
- Errores:
  - `ResourceNotFoundException`, `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.calendario.EliminarDiaNoLectivo`

- Función: eliminar un día no lectivo por id.
- Repositorios/dependencias:
  - `DiaNoLectivoRepository`, `ClockProvider`
- Reglas:
  - el día no lectivo debe existir
  - el año escolar asociado no puede estar `CERRADO`
- Errores:
  - `ResourceNotFoundException`, `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.calendario.ListarDiasNoLectivos`

- Función: listar días no lectivos por año escolar, con filtro opcional por mes/año.
- Repositorios/dependencias:
  - `DiaNoLectivoRepository`
- Reglas:
  - si no se envían filtros, retorna todo el año escolar ordenado por fecha.
  - si se filtra, exige `mes` y `anio` juntos.
  - valida mes en rango `1..12`.
  - valida que `mes/anio` formen una fecha válida.
- Salida:
  - `List<DiaNoLectivoResponse>`.
- Errores:
  - `BusinessException`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.apoderado.CrearApoderadoConUsuario`

- Función: crear apoderado + usuario `ROL=APODERADO` o vincular apoderado existente por RUT a un alumno.
- Repositorios/dependencias:
  - `ApoderadoRepository`, `ApoderadoAlumnoRepository`, `UsuarioRepository`, `AlumnoRepository`, `PasswordEncoder`, `RutValidationService`
- Reglas:
  - alumno debe existir
  - MVP: un alumno no puede tener más de un apoderado vinculado
  - RUT se normaliza (`RutNormalizer`) y siempre se valida formato + dígito verificador
  - si el apoderado no existe por RUT, valida disponibilidad cross-tabla (no debe existir como alumno o profesor)
  - si el apoderado no existe por RUT, valida unicidad de email en `usuario` y `apoderado`
  - al crear usuario, password inicial = RUT normalizado con BCrypt
  - crea vínculo `apoderado_alumno`
- Errores:
  - `ResourceNotFoundException`
  - `ConflictException`
  - `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.apoderado.BuscarApoderadoPorRut`

- Función: buscar apoderado por RUT normalizado y retornar alumnos vinculados con `cursoNombre` activo cuando exista.
- Repositorios/dependencias:
  - `ApoderadoRepository`, `ApoderadoAlumnoRepository`, `MatriculaRepository`
- Reglas:
  - normaliza y valida RUT de entrada.
  - requiere existencia de apoderado por RUT.
  - enriquece cada alumno con curso de matrícula `ACTIVA` cuando existe.
  - evita N+1: resuelve matrículas activas por lote (`alumnoId IN (...)`) y luego mapea en memoria.
- Salida:
  - `ApoderadoBuscarResponse`.
- Errores:
  - `BusinessException`, `ResourceNotFoundException`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.apoderado.ObtenerApoderadoPorAlumno`

- Función: obtener apoderado asociado a un alumno (MVP: primer vínculo), con estado de cuenta y resumen de alumnos del apoderado.
- Repositorios/dependencias:
  - `AlumnoRepository`, `ApoderadoAlumnoRepository`, `ApoderadoRepository`, `UsuarioRepository`
- Reglas:
  - valida existencia del alumno.
  - si no hay vínculo, retorna vacío (`Optional.empty()` -> `204` en controller).
  - si hay vínculo, arma `ApoderadoResponse` con `usuarioId/cuentaActiva`.
- Salida:
  - `Optional<ApoderadoResponse>`.
- Errores:
  - `ResourceNotFoundException`.
- `@Transactional(readOnly = true)`: sí.

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
  - `ApoderadoAlumnoRepository`, `RegistroAsistenciaRepository`, `AlumnoRepository`, `AnoEscolarRepository`, `DiaNoLectivoRepository`
- Reglas:
  - ownership obligatorio (`apoderadoId` -> `alumnoId`)
  - valida mes/año para construir rango de fechas
  - agrupa registros por fecha y calcula `PRESENTE`, `AUSENTE` o `PARCIAL`
  - resuelve año escolar activo en el mes consultado y agrega `diasNoLectivos` por rango mensual
  - mes sin registros retorna `dias=[]` y mantiene `diasNoLectivos` según calendario
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
  - resuelve `anoEscolarId` con prioridad `header > query`.
  - si faltan ambos (`header` y `query`), lanza `ApiException(VALIDATION_FAILED)`.
  - ownership obligatorio (`apoderadoId` -> `alumnoId`)
  - conteo por estado (`PRESENTE`, `AUSENTE`) filtrado por `anoEscolarId`
  - porcentaje con 1 decimal sobre el total de bloques registrados
- Errores:
  - `ApiException(VALIDATION_FAILED)`
  - `AccessDeniedException`
  - `ResourceNotFoundException`
- `@Transactional`: no.

### `com.schoolmate.api.usecase.alumno.CrearAlumnoConApoderado`

- Función: crear alumno y gestionar su apoderado principal en una sola transacción.
- Repositorios/dependencias:
  - `AlumnoRepository`, `ApoderadoRepository`, `ApoderadoAlumnoRepository`, `UsuarioRepository`, `PasswordEncoder`, `RutValidationService`
- Reglas:
  - normaliza RUT de alumno/apoderado con `RutNormalizer`
  - valida formato + dígito verificador en ambos RUT
  - valida disponibilidad cross-tabla del RUT alumno
  - valida disponibilidad cross-tabla del RUT apoderado solo cuando es apoderado nuevo
  - valida RUT de alumno único (`existsByRut`)
  - si apoderado no existe por RUT:
    - valida unicidad de email en `usuario` y `apoderado`
    - crea `Apoderado`
    - crea `Usuario` con `ROL=APODERADO`, `apoderadoId` y password inicial = RUT normalizado (BCrypt)
  - crea `Alumno` activo
  - crea vínculo `apoderado_alumno` con `esPrincipal=true` y `vinculo` (`VinculoApoderado`)
  - respuesta retorna `AlumnoResponse` con `apoderado` enriquecido
- Errores:
  - `ConflictException`
  - `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.profesor.CrearProfesorConUsuario`

- Función: creación transaccional de profesor + usuario (`ROL=PROFESOR`) con password inicial igual al RUT normalizado.
- Repositorios/dependencias:
  - `ProfesorRepository`, `MateriaRepository`, `UsuarioRepository`, `PasswordEncoder`, `RutValidationService`
- Reglas:
  - normaliza RUT y valida formato + dígito verificador
  - valida disponibilidad cross-tabla del RUT (no debe existir como alumno o apoderado)
  - mantiene validaciones de duplicados de `Profesor` (rut/email/teléfono)
  - valida duplicados en `Usuario` por email y RUT normalizado
  - si falla creación de usuario, rollback completo de profesor
  - tras persistir profesor/usuario, recarga profesor con `materias` usando `findByIdWithMaterias`.
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.profesor.ObtenerProfesores`

- Función: listar profesores paginados y ordenables.
- Repositorios/dependencias:
  - `ProfesorRepository`
- Reglas:
  - lectura pura para alimentar `GET /api/profesores`.
  - normaliza paginación (`page >= 0`, `size` en rango `1..100`).
  - normaliza orden:
    - `sortBy` permitido: `apellido`, `nombre`, `email`, `fechaContratacion`, `createdAt` (fallback `apellido`).
    - `sortDir` permitido: `asc|desc` (fallback `asc`).
  - usa `findPageWithMaterias(...)` con carga explícita de `materias`.
- Salida:
  - `ProfesorPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.profesor.ObtenerDetalleProfesor`

- Función: obtener detalle de profesor con horas asignadas en año escolar activo.
- Repositorios/dependencias:
  - `ProfesorRepository`, `AnoEscolarRepository`, `BloqueHorarioRepository`, `ClockProvider`
- Reglas:
  - carga profesor con materias (`findByIdWithMaterias`).
  - resuelve año activo por fecha con `findActivoByFecha(clockProvider.today())`.
  - calcula `horasAsignadas` desde bloques (`ceil(minutos/45)`), o `null` si no hay año activo.
- Salida:
  - `ProfesorResponse`.
- Errores:
  - `ResourceNotFoundException("Profesor no encontrado")`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.profesor.ActualizarProfesor`

- Función: actualizar datos de profesor manteniendo invariantes de RUT y unicidad.
- Repositorios/dependencias:
  - `ProfesorRepository`, `MateriaRepository`, `RutValidationService`
- Reglas:
  - valida formato de RUT normalizado.
  - bloquea cambio de RUT (`PROFESOR_RUT_INMUTABLE`).
  - valida duplicados en `rut`, `email`, `telefono`.
  - valida `materiaIds` existentes; si faltan -> `MATERIAS_NOT_FOUND`.
  - persiste y recarga con materias (`findByIdWithMaterias`) para mapeo seguro de respuesta.
- Salida:
  - `Profesor`.
- Errores:
  - `ResourceNotFoundException`, `ApiException` (`PROFESOR_RUT_INMUTABLE`, duplicados, `MATERIAS_NOT_FOUND`).
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.profesor.ObtenerSesionesProfesor`

- Función: listar sesiones de login del usuario asociado a un profesor (paginado, con filtros opcionales de fecha).
- Repositorios/dependencias:
  - `ProfesorRepository`, `UsuarioRepository`, `SesionUsuarioRepository`
- Reglas:
  - valida existencia de profesor.
  - valida que exista usuario vinculado al profesor.
  - convierte `desde` (inclusivo) y `hasta` (exclusivo +1 día) a `LocalDateTime`.
  - usa flags booleanos en repository (`aplicarDesde`, `aplicarHasta`) para tipado estable en PostgreSQL.
- Salida:
  - `SesionProfesorPageResponse`.
- Errores:
  - `ResourceNotFoundException` (profesor no encontrado / sin usuario asociado).
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.profesor.ObtenerHorarioProfesor`

- Función: construir horario semanal consolidado del profesor para un año escolar (`header > query`) con control de ownership.
- Repositorios/dependencias:
  - `ProfesorRepository`, `AnoEscolarRepository`, `BloqueHorarioRepository`
- Reglas:
  - requiere año escolar desde header o query.
  - si rol es `PROFESOR`, solo permite consultar su propio `profesorId`.
  - filtra bloques sin materia para el response.
  - agrupa por día, ordena por hora y calcula `horasAsignadas` (`ceil(minutos/45)`).
- Salida:
  - `ProfesorHorarioResponse`.
- Errores:
  - `ApiException(VALIDATION_FAILED)`, `AccessDeniedException`, `ResourceNotFoundException`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.matricula.MatricularAlumno`

- Función: crear matrícula activa de alumno resolviendo año escolar con prioridad `header > body`.
- Repositorios:
  - `AlumnoRepository`, `CursoRepository`, `AnoEscolarRepository`, `MatriculaRepository`
- Validaciones:
  - `anoEscolarId` requerido vía header `X-Ano-Escolar-Id` o body
  - alumno/curso/año existen
  - año escolar no puede estar `CERRADO` (`anoEscolar.calcularEstado(clockProvider.today())`)
  - curso pertenece al año indicado
  - alumno no tiene matrícula activa en ese año
- Flujo:
  1. resuelve `anoEscolarId` (`header > body`)
  2. carga entidades (curso con `findByIdWithGradoAndAnoEscolar`)
  3. valida estado del año escolar (`!= CERRADO`)
  4. valida pertenencia curso-año
  5. valida unicidad de matrícula activa
  6. define fecha (`request.fechaMatricula` o `clockProvider.today()`)
  7. guarda `Matricula(estado=ACTIVA)` y mapea a response.
- Salida:
  - `MatriculaResponse`.
- Errores:
  - `ApiException(VALIDATION_FAILED)`
  - `ResourceNotFoundException`
  - `BusinessException`
- `@Transactional`: sí.

### `com.schoolmate.api.usecase.matricula.CambiarEstadoMatricula`

- Función: transición de estado de matrícula.
- Repositorio: `MatriculaRepository`.
- Validaciones:
  - entrada `estado` no vacía cuando se usa firma raw (`String`)
  - parsing válido de enum (`ACTIVA|RETIRADO|TRASLADADO`) cuando se usa firma raw (`String`)
  - matrícula existe
  - transición válida:
    - `ACTIVA -> RETIRADO|TRASLADADO`
    - `RETIRADO|TRASLADADO -> ACTIVA`
  - no repetir mismo estado
- Flujo:
  1. (firma `String`) valida/reconvierte estado raw.
  2. carga matrícula con relaciones (`findByIdWithRelaciones`)
  3. valida transición
  4. setea nuevo estado
  5. guarda y mapea a response.
- Salida:
  - `MatriculaResponse`.
- Errores:
  - `ApiException(VALIDATION_FAILED)` (estado ausente/ inválido)
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

### `com.schoolmate.api.usecase.matricula.ObtenerMatriculasPorCurso`

- Función: listar matrículas activas por curso aplicando ownership para `PROFESOR`, paginado y ordenable.
- Repositorios/dependencias:
  - `MatriculaRepository`, `ValidarAccesoMatriculasCursoProfesor`
- Reglas:
  - valida acceso según rol/ownership antes de consultar.
  - retorna solo estado `ACTIVA`.
  - normaliza paginación (`page >= 0`, `size` en rango `1..100`).
  - normaliza orden:
    - `sortBy` permitido: `alumno.apellido`, `alumno.nombre`, `fechaMatricula`, `createdAt`, `updatedAt` (fallback `alumno.apellido`).
    - `sortDir` permitido: `asc|desc` (fallback `asc`).
- Salida:
  - `MatriculaPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.matricula.ObtenerMatriculasPorAlumno`

- Función: obtener historial de matrículas de un alumno, paginado y ordenable.
- Repositorios/dependencias:
  - `MatriculaRepository`
- Reglas:
  - lectura pura del historial por `alumnoId`.
  - normaliza paginación (`page >= 0`, `size` en rango `1..100`).
  - normaliza orden:
    - `sortBy` permitido: `fechaMatricula`, `createdAt`, `updatedAt`, `estado` (fallback `fechaMatricula`).
    - `sortDir` permitido: `asc|desc` (fallback `desc`).
- Salida:
  - `MatriculaPageResponse`.
- `@Transactional(readOnly = true)`: sí.

### `com.schoolmate.api.usecase.profesor.ObtenerClasesHoyProfesor`

- Función: obtener clases del día del profesor autenticado.
- Repositorios/dependencias:
  - `ClockProvider`, `AnoEscolarRepository`, `BloqueHorarioRepository`, `MatriculaRepository`, `AsistenciaClaseRepository`, `DiaNoLectivoRepository`
- Reglas:
  - usa `profesorId` del JWT (`UserPrincipal`)
  - si hoy es sábado/domingo o no hay año activo: retorna lista vacía
  - adjunta `diaNoLectivo` cuando existe registro para `hoy` en el año activo
  - consulta bloques `CLASE` activos del día y año activo
  - calcula estado temporal `PENDIENTE|DISPONIBLE|EXPIRADA` con ventana ±15 minutos
  - evita N+1 con resolución batch:
    - `cantidadAlumnos` se obtiene por curso con `countActivasByCursoIds(...)`.
    - `asistenciaTomada` se obtiene por conjunto de bloques con `findBloqueIdsConAsistenciaTomada(...)`.
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
  2. carga curso con `grado` y `anoEscolar` usando fetch explícito
  3. carga bloques activos con `materia` y `profesor` (todos o por día) usando queries fetch dedicadas
  4. agrupa por `diaSemana`
  5. construye `JornadaCursoResponse` + `JornadaResumenResponse`
- Errores:
  - `ResourceNotFoundException`
- `@Transactional`: no.

### `com.schoolmate.api.usecase.jornada.ValidarAccesoJornadaCurso`

- Función: validar acceso de lectura de jornada por curso.
- Repositorios/dependencias:
  - `ApoderadoAlumnoRepository`, `MatriculaRepository`
- Reglas:
  - `ADMIN` siempre permitido.
  - `APODERADO` requiere `apoderadoId` en JWT.
  - valida que tenga al menos un alumno vinculado con matrícula `ACTIVA` en el curso.
- Error:
  - `AccessDeniedException` (mapea a `ACCESS_DENIED`).

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
- Optimización aplicada:
  - curso cargado con `grado` y `anoEscolar` por método fetch (`findByIdWithGradoAndAnoEscolar`).
  - bloque cargado con detalle (`curso`,`materia`,`profesor`) por `findDetalleById`.
  - malla y bloques CLASE cargados con fetch explícito de relaciones para evitar lazy N+1.
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
- Optimización aplicada:
  - carga de curso con `grado` y `anoEscolar` vía fetch explícito.
  - carga de malla con `materia` y de bloques CLASE con `materia/profesor` para evitar N+1.
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
- Optimización aplicada:
  - bloque consultado por método con `JOIN FETCH` (`findDetalleById`).
  - carga de bloques de profesores con `profesor` fetch.
  - colisiones con `curso` y `materia` fetch para construir conflicto sin lazy adicional.
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
- Optimización aplicada:
  - curso y bloques `CLASE` cargados con fetch explícito de relaciones necesarias (`materia`, `profesor`).
- `@Transactional`: no.

---

## SECCIÓN 9: REPOSITORIOS

| Repositorio | Entidad | Métodos derivados destacados | `@Query` custom | Specifications |
|---|---|---|---|---|
| `AlumnoRepository` | `Alumno` | `existsByRut`, `existsByRutAndIdNot` | `findActivoByRutNormalizado` (native SQL con `regexp_replace`) | sí, vía `JpaSpecificationExecutor` |
| `AnoEscolarRepository` | `AnoEscolar` | `findAllByOrderByAnoDesc`, `findByAno`, `existsByAno`, `findByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual`, `findActivoByFecha` (default) | `existsSolapamiento`, `existsSolapamientoExcluyendoId` | no |
| `AsistenciaClaseRepository` | `AsistenciaClase` | `findByBloqueHorarioIdAndFecha`, `existsByBloqueHorarioIdAndFecha` | `findBloqueIdsConAsistenciaTomada` (batch por `bloqueIds + fecha`) | no |
| `BloqueHorarioRepository` | `BloqueHorario` | `findByCursoIdAndActivoTrueOrderByDiaSemanaAscNumeroBloqueAsc`, `findByCursoIdAndDiaSemanaAndActivoTrueOrderByNumeroBloqueAsc`, `findByCursoIdAndActivoTrueAndTipo`, `findByCursoIdAndActivoTrueAndTipoAndMateriaId` | `desactivarBloquesDia`, `findDiasConfigurados`, `findColisionesProfesor`, `findColisionesProfesorConCursoYMateria`, `findHorarioProfesorEnAnoEscolar`, `findBloquesClaseProfesoresEnAnoEscolarConProfesor`, `findClasesProfesorEnDia`, `existsBloqueActivoProfesorEnCurso`, `findDetalleById`, `findActivosByCursoIdWithMateriaAndProfesorOrderByDiaSemanaAscNumeroBloqueAsc`, `findActivosByCursoIdAndDiaSemanaWithMateriaAndProfesorOrderByNumeroBloqueAsc`, `findByCursoIdAndActivoTrueAndTipoWithMateriaAndProfesor` | no |
| `CursoRepository` | `Curso` | `findByAnoEscolarIdOrderByNombreAsc`, `findByAnoEscolarIdAndGradoIdOrderByLetraAsc`, `findByActivoTrueAndAnoEscolarIdOrderByNombreAsc`, `countByAnoEscolarIdAndActivoTrue`, `findPageByAnoEscolarIdAndGradoId`, `findPageByAnoEscolarId` | `findLetrasUsadasByGradoIdAndAnoEscolarId`, `findByIdWithGradoAndAnoEscolar`, `findByAnoEscolarIdOrderByNombreAscWithRelaciones`, `findByAnoEscolarIdAndGradoIdOrderByLetraAscWithRelaciones`, `findAllOrderByNombreAscWithRelaciones`, `findPageWithRelaciones` | no |
| `DiaNoLectivoRepository` | `DiaNoLectivo` | `existsByAnoEscolarIdAndFecha`, `findByAnoEscolarIdAndFecha`, `findByAnoEscolarIdOrderByFechaAsc`, `findByAnoEscolarIdAndFechaBetweenOrderByFechaAsc` | no | no |
| `GradoRepository` | `Grado` | `findAllByOrderByNivelAsc` | no | no |
| `MallaCurricularRepository` | `MallaCurricular` | múltiples `findBy...`/`findPageBy...` y `existsBy...` combinando materia/grado/año/activo | `findActivaByGradoIdAndAnoEscolarIdWithMateria` (`JOIN FETCH materia`) | no |
| `MateriaRepository` | `Materia` | `findAllByOrderByNombreAsc`, `existsByNombre` | no | no |
| `MatriculaRepository` | `Matricula` | `findByIdWithRelaciones`, `findByAlumnoId`, `findByCursoIdAndEstado`, `findByAlumnoIdAndAnoEscolarIdAndEstado`, `findByAlumnoIdInAndAnoEscolarIdAndEstado`, `findByAlumnoIdInAndEstadoOrderByFechaMatriculaDescCreatedAtDesc`, `findPageByCursoIdAndEstado`, `findPageByAlumnoId`, `existsByAlumnoIdAndAnoEscolarIdAndEstado`, `existsByCursoIdAndEstadoAndAlumnoIdIn`, `countByCursoIdAndEstado`, `countByAnoEscolarIdAndEstado` | `countActivasByCursoIds` | no |
| `ProfesorRepository` | `Profesor` | unicidad por rut/email/teléfono + listas ordenadas, `findByActivoTrueAndMaterias_Id`, `findByIdWithMaterias`, `findAllOrderByApellidoAscWithMaterias`, `findPageWithMaterias`, `countByActivoTrue` | `findByIdWithMaterias` (JPQL + `@EntityGraph(materias)`), `findAllOrderByApellidoAscWithMaterias`, `findPageWithMaterias` | no |
| `SesionUsuarioRepository` | `SesionUsuario` | `findByUsuarioIdAndFechas` (paginado, orden desc por `createdAt`) | JPQL con flags booleanos (`aplicarDesde/aplicarHasta`) para filtros opcionales por rango (`desde`/`hasta`) y tipado estable en PostgreSQL | no |
| `EventoAuditoriaRepository` | `EventoAuditoria` | `findByFiltros` (paginado por `createdAt DESC`) | JPQL con flags booleanos (`aplicarFiltro`) para filtros opcionales por `usuarioId`, `metodoHttp`, `endpoint LIKE`, `desde/hasta` (evita problemas de tipado con parámetros null en PostgreSQL) | no |
| `ApoderadoRepository` | `Apoderado` | `findByEmail`, `findByRut`, `existsByEmail`, `existsByRut` | no | no |
| `ApoderadoAlumnoRepository` | `ApoderadoAlumno` | `existsByIdApoderadoIdAndIdAlumnoId`, `findByIdApoderadoId`, `findByIdAlumnoId`, wrappers `findByApoderadoId/findByAlumnoId`, `existsByAlumnoId`, `existsByApoderadoIdAndAlumnoId` | `findByApoderadoIdWithAlumno` (JPQL con `JOIN FETCH`) | no |
| `RegistroAsistenciaRepository` | `RegistroAsistencia` | `findByAsistenciaClaseId` (con `@EntityGraph alumno`), `deleteByAsistenciaClaseId` (legacy, no usado por `GuardarAsistenciaClase` actual), `findByAlumnoIdAndFechaEntre`, `countByAlumnoIdAndEstadoAndAnoEscolarId` | `DELETE` por `asistenciaClase.id`; JPQL para mensual por fecha y resumen por año escolar vía joins `asistencia_clase -> bloque_horario -> curso` | no |
| `SeccionCatalogoRepository` | `SeccionCatalogo` | `findByActivoTrueOrderByOrdenAsc` | no | no |
| `UsuarioRepository` | `Usuario` | `findByEmail`, `findByRut`, `findByApoderadoId`, `findByProfesorId`, `findByRefreshToken`, `existsByEmail`, `existsByRut`, `existsByProfesorId` | no | no |

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
| `LoginRequest` | `identificador`, `password`, `latitud?`, `longitud?`, `precisionMetros?` | `@NotBlank` en `identificador/password` (geolocalización opcional) | `@Data` |
| `RefreshTokenRequest` | `refreshToken` | `@NotBlank` | `@Data` |
| `AnoEscolarRequest` | `ano`, `fechaInicioPlanificacion`, `fechaInicio`, `fechaFin` | `@NotNull` | `@Data` |
| `MateriaRequest` | `nombre`, `icono` | `@NotBlank(nombre)` | `@Data` |
| `ProfesorRequest` | `rut,nombre,apellido,email,telefono,fechaContratacion,horasPedagogicasContrato?,materiaIds` | `@NotBlank`, `@Size`, `@Email`, `@NotNull`, `@NotEmpty`, `@Min(1)`, `@Max(50)` (opcional) | `@Data` |
| `CursoRequest` | `gradoId`, `anoEscolarId` | `@NotBlank` solo en `gradoId` (`anoEscolarId` nullable para migración header) | `@Data` |
| `AlumnoRequest` | `rut,nombre,apellido,fechaNacimiento` | `@NotBlank`, `@Size`, `@NotNull` | `@Data @Builder` |
| `CrearAlumnoConApoderadoRequest` | `alumno{rut,nombre,apellido,fechaNacimiento}, apoderado{rut,nombre,apellido,email,telefono}, vinculo` | `@NotNull`, `@Valid`, `@NotBlank`, `@Email`, `@Size` | `@Data` |
| `MatriculaRequest` | `alumnoId,cursoId,anoEscolarId,fechaMatricula?` | `@NotBlank` en `alumnoId,cursoId` (`anoEscolarId` nullable para migración header) | `@Data @Builder` |
| `CambiarEstadoMatriculaRequest` | `estado` | `@NotBlank` en `estado` | `@Data` |
| `MallaCurricularRequest` | `materiaId,gradoId,anoEscolarId,horasPedagogicas` | `@NotBlank` en `materiaId,gradoId`; `@NotNull`, `@Min(1)`, `@Max(15)` en `horasPedagogicas` (`anoEscolarId` nullable) | `@Data` |
| `MallaCurricularBulkRequest` | `materiaId,anoEscolarId,grados[]` | `@NotBlank` en `materiaId`; `@NotEmpty`, `@Valid` en `grados` (`anoEscolarId` nullable) | `@Data` |
| `MallaCurricularBulkRequest.GradoHoras` | `gradoId,horasPedagogicas` | `@NotBlank`, `@NotNull`, `@Min(1)`, `@Max(15)` | `@Data` |
| `MallaCurricularUpdateRequest` | `horasPedagogicas,activo` | `@NotNull`, `@Min(1)`, `@Max(15)` en `horasPedagogicas`; `@NotNull` en `activo` | `@Data` |
| `BloqueRequest` | `numeroBloque,horaInicio,horaFin,tipo` | `@NotNull`, `@Min(1)`, `@Pattern` para hora `HH:mm` y tipo | `@Getter/@Setter` |
| `JornadaDiaRequest` | `bloques[]` | `@NotNull`, `@Size(min=1)`, `@Valid` | `@Getter/@Setter` |
| `CopiarJornadaRequest` | `diasDestino[]` | `@NotNull`, `@Size(min=1)`, elementos `@Min(1) @Max(5)` | `@Getter/@Setter` |
| `CrearDiaNoLectivoRequest` | `fechaInicio,fechaFin,tipo,descripcion?` | `@NotNull` en fechas/tipo, `@Size(max=200)` en descripción, validador `fechaFin >= fechaInicio` | `@Data` |
| `AsignarMateriaRequest` | `materiaId` | `@NotBlank` | `@Data` |
| `AsignarProfesorRequest` | `profesorId` | `@NotBlank` | `@Data` |
| `GuardarAsistenciaRequest` | `bloqueHorarioId,fecha,registros[]` | `@NotBlank`, `@NotNull`, `@Size(min=1)`, `@Valid` | `@Data` |
| `RegistroAlumnoRequest` | `alumnoId,estado` | `@NotBlank`, `@NotNull` | `@Data` |

### DTOs de Portal/Apoderado (`com.schoolmate.api.dto`)

| DTO | Campos principales | Builder/Lombok |
|---|---|---|
| `ApoderadoRequest` | `nombre,apellido,rut,email,telefono,alumnoId` | `@Getter/@Setter` + validaciones `@NotBlank/@Email` |
| `ApoderadoBuscarResponse` | `id,nombre,apellido,rut,email,telefono,existe,alumnos[]` | `@Getter/@Setter @Builder` |
| `ApoderadoBuscarResponse.AlumnoVinculado` | `id,nombre,apellido,cursoNombre` | `@Getter/@Setter @Builder` |
| `ApoderadoResponse` | `id,nombre,apellido,rut,email,telefono,usuarioId,cuentaActiva,alumnos[]` | `@Getter/@Setter @Builder` |
| `ApoderadoResponse.AlumnoResumen` | `id,nombre,apellido` | `@Getter/@Setter @Builder` |
| `AlumnoApoderadoResponse` | `id,nombre,apellido,cursoId,cursoNombre,anoEscolarId` | `@Getter/@Setter @Builder` |
| `AsistenciaMensualResponse` | `alumnoId,alumnoNombre,mes,anio,dias[],diasNoLectivos[]` | `@Getter/@Setter @Builder` |
| `AsistenciaDiaResponse` | `fecha,totalBloques,bloquesPresente,bloquesAusente,estado` | `@Getter/@Setter @Builder` |
| `ResumenAsistenciaResponse` | `alumnoId,alumnoNombre,totalClases,totalPresente,totalAusente,porcentajeAsistencia` | `@Getter/@Setter @Builder` |
| `RegistroConFecha` | `registroId,alumnoId,estado,fecha` (proyección interna para query) | `@Getter/@Setter` |
| `SesionProfesorResponse` | `id,fechaHora,ipAddress,latitud,longitud,precisionMetros,userAgent` | `@Data @Builder` |
| `SesionProfesorPageResponse` | `profesorId,profesorNombre,sesiones[],totalElements,totalPages,currentPage` | `@Data @Builder` |
| `DiaNoLectivoResponse` | `id,fecha,tipo,descripcion` | `@Data @Builder` |
| `EventoAuditoriaResponse` | `id,usuarioEmail,usuarioRol,metodoHttp,endpoint,requestBody,responseStatus,ipAddress,anoEscolarId,fechaHora` | `@Data @Builder` |
| `EventoAuditoriaPageResponse` | `eventos[],totalElements,totalPages,currentPage` | `@Data @Builder` |

### Response DTOs

| DTO | Campos principales | Builder/Lombok |
|---|---|---|
| `AuthResponse` | `token,accessToken,refreshToken,tipo,id,email,nombre,apellido,rol,profesorId,apoderadoId` | `@Data @Builder` |
| `ApiErrorResponse` | `code,message,status,field,path,timestamp,details` | `@Data @Builder` |
| `AnoEscolarResponse` | `id,ano,fechaInicioPlanificacion,fechaInicio,fechaFin,estado,createdAt,updatedAt` | `@Data @Builder` |
| `MateriaResponse` | `id,nombre,icono,createdAt,updatedAt` | `@Data @Builder` |
| `MateriaPageResponse` | `content,page,size,totalElements,totalPages,sortBy,sortDir,hasNext,hasPrevious` | `@Data @Builder` |
| `ProfesorResponse` | `id,rut,nombre,apellido,email,telefono,fechaContratacion,horasPedagogicasContrato,horasAsignadas,activo,materias,createdAt,updatedAt` | `@Data @Builder` |
| `ProfesorPageResponse` | `content,page,size,totalElements,totalPages,sortBy,sortDir,hasNext,hasPrevious` | `@Data @Builder` |
| `ProfesorResponse.MateriaInfo` | `id,nombre,icono` | `@Data @Builder` |
| `CursoResponse` | `id,nombre,letra,gradoId,gradoNombre,anoEscolarId,anoEscolar,activo,alumnosMatriculados,cantidadMaterias,totalHorasPedagogicas,materias,createdAt,updatedAt` | `@Data @Builder` |
| `CursoPageResponse` | `content,page,size,totalElements,totalPages,sortBy,sortDir,hasNext,hasPrevious` | `@Data @Builder` |
| `CursoResponse.MateriaCargaResponse` | `materiaId,materiaNombre,materiaIcono,horasPedagogicas` | `@Data @Builder` |
| `AlumnoResponse` | datos personales + compatibilidad (`apoderadoNombre/apellido/email/telefono/vinculo`) + `apoderado` (objeto) + auditoría + matrícula opcional (`matriculaId,cursoId,cursoNombre,gradoNombre,estadoMatricula,fechaMatricula`) | `@Data @Builder` |
| `AlumnoResponse.ApoderadoInfo` | `id,nombre,apellido,rut,vinculo` | `@Data @Builder` |
| `AlumnoPageResponse` | contrato paginado equivalente a materias | `@Data @Builder` |
| `MallaCurricularResponse` | `id,materiaId,materiaNombre,materiaIcono,gradoId,gradoNombre,gradoNivel,anoEscolarId,anoEscolar,horasPedagogicas,activo,createdAt,updatedAt` | `@Data @Builder` |
| `MallaCurricularPageResponse` | `content,page,size,totalElements,totalPages,sortBy,sortDir,hasNext,hasPrevious` | `@Data @Builder` |
| `MatriculaPageResponse` | `content,page,size,totalElements,totalPages,sortBy,sortDir,hasNext,hasPrevious` | `@Data @Builder` |
| `MatriculaResponse` | `id,alumno*,curso*,gradoNombre,anoEscolar*,fechaMatricula,estado,createdAt,updatedAt` | `@Data @Builder` |
| `BloqueHorarioResponse` | `id,numeroBloque,horaInicio,horaFin,tipo,materia*,profesor*` | `@Getter/@Setter @Builder` |
| `JornadaDiaResponse` | `diaSemana,nombreDia,bloques,totalBloquesClase,horaInicio,horaFin` | `@Getter/@Setter @Builder` |
| `JornadaCursoResponse` | `cursoId,cursoNombre,dias,resumen` | `@Getter/@Setter @Builder` |
| `JornadaResumenResponse` | `cursoId,diasConfigurados,bloquesClasePorDia,totalBloquesClaseSemana` | `@Getter/@Setter @Builder` |
| `MateriaDisponibleResponse` | `materiaId,materiaNombre,materiaIcono,horasPedagogicas,minutosSemanalesPermitidos,minutosAsignados,minutosDisponibles,asignable,asignadaEnEsteBloque` | `@Data @Builder` |
| `MateriasDisponiblesResponse` | `bloqueId,bloqueDuracionMinutos,materias[]` | `@Data @Builder` |
| `AsignacionMateriaResumenResponse` | resumen curso + `materias[]` + bloques asignados por materia | `@Data @Builder` |
| `ConflictoHorarioResponse` | `cursoNombre,materiaNombre,horaInicio,horaFin,bloqueId` | `@Data @Builder` |
| `DashboardAdminResponse` | `totalAlumnos,totalCursos,totalProfesores` | `@Data @Builder` |
| `GradoResponse` | `id,nombre,nivel,createdAt,updatedAt` | `@Data @Builder` |
| `EstadoClaseHoy` | `PENDIENTE,DISPONIBLE,EXPIRADA` | `enum` |
| `ClaseHoyResponse` | `bloqueId,numeroBloque,horaInicio,horaFin,cursoId,cursoNombre,materiaId,materiaNombre,materiaIcono,cantidadAlumnos,estado,asistenciaTomada` | `@Data @Builder` |
| `ClasesHoyResponse` | `fecha,diaSemana,nombreDia,diaNoLectivo?,clases[]` | `@Data @Builder` |
| `AsistenciaClaseResponse` | `asistenciaClaseId,bloqueHorarioId,fecha,tomadaEn,registradoPorNombre,registros[]` | `@Data @Builder` |
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

Implementado en `AnoEscolar.calcularEstado(LocalDate fechaReferencia)`:

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

### Resolución de año escolar en endpoints (migración gradual)

- Se incorporó resolución por header `X-Ano-Escolar-Id` mediante:
  - `AnoEscolarHeaderInterceptor`
  - `AnoEscolarArgumentResolver`
  - anotación `@AnoEscolarActivo`
  - registro en `WebMvcConfig`
- Regla de prioridad en endpoints migrados: `header > query/body`.
- Compatibilidad temporal: se mantienen query params/campos body legacy `anoEscolarId` como fallback.
- El ID aceptado en header es `UUID`, consistente con `ano_escolar.id` actual.

### Generación automática de curso (nombre + letra)

`CrearCurso` y `ActualizarCurso`:

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
- Escritura bloqueada cuando el año consultado está `CERRADO` (`POST /api/malla-curricular` y `POST /api/malla-curricular/bulk`).

### Escritura sobre año escolar cerrado

- `POST /api/matriculas`: bloquea en use case `MatricularAlumno` cuando el año está `CERRADO`.
- `POST /api/cursos`: bloquea cuando el año está `CERRADO`.
- `POST /api/malla-curricular` y `POST /api/malla-curricular/bulk`: bloquean cuando el año está `CERRADO`.

### Validaciones de RUT, email, teléfono duplicados

Implementadas con validación transversal en `RutValidationService` + reglas específicas por flujo:

- `RutValidationService` (paquete `common.rut`) centraliza:
  - validación de formato de RUT chileno (`7-8 dígitos + guion + DV 0-9/K`)
  - validación de dígito verificador (módulo 11)
  - validación de disponibilidad cross-tabla entre `alumno`, `profesor` y `apoderado`
- `TipoPersona` define contexto de validación (`ALUMNO`, `PROFESOR`, `APODERADO`)
- En profesores:
  - `POST /api/profesores`: valida formato/DV + cross-tabla y mantiene duplicados rut/email/teléfono
  - `PUT /api/profesores/{id}`: valida formato de RUT y mantiene regla `PROFESOR_RUT_INMUTABLE`
- `horasPedagogicasContrato` opcional en profesor:
  - si viene valor, rango válido `1..50`
  - en update puede enviarse `null` para limpiar
- `GET /api/profesores/{id}` calcula `horasAsignadas` en año escolar `ACTIVO`:
  - suma minutos de bloques `CLASE` asignados al profesor
  - convierte a horas pedagógicas con `ceil(minutos / 45.0)`
  - si no existe año `ACTIVO`, retorna `horasAsignadas=null`

Para alumnos:

- `POST /api/alumnos` y `PUT /api/alumnos/{id}` validan formato/DV + cross-tabla y además mantienen unicidad interna de `rut`.
- `POST /api/alumnos/con-apoderado` agrega reglas transaccionales:
  - normalización de RUT alumno/apoderado
  - validación de formato/DV en ambos RUT
  - validación cross-tabla para RUT de alumno
  - validación cross-tabla para RUT de apoderado solo si es persona nueva
  - RUT alumno no duplicado en tabla `alumno`
  - reuso de apoderado existente por RUT o creación de nuevo apoderado + usuario
  - password inicial de usuario apoderado = RUT normalizado (BCrypt)
  - creación de vínculo `apoderado_alumno` con `vinculo` (`MADRE`, `PADRE`, `TUTOR_LEGAL`, `ABUELO`, `OTRO`)

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
- En matrícula (`MatricularAlumno`):
  - se bloquea crear matrícula cuando `AnoEscolar.calcularEstado(clockProvider.today()) == CERRADO`
- Gestión de apoderados (MVP):
  - RUT de apoderado normalizado y validado (formato/DV) antes de crear/vincular.
  - validación cross-tabla solo cuando el apoderado es persona nueva.
  - creación de usuario `ROL=APODERADO` con password inicial = RUT normalizado (BCrypt).
  - máximo un apoderado por alumno en flujo admin actual (`existsByAlumnoId`).
  - si el alumno ya tiene vínculo, responde conflicto (`409`).
  - `GET /api/apoderados/buscar-por-rut` retorna alumnos vinculados; incluye `cursoNombre` cuando hay matrícula activa.
- En asistencia (`GuardarAsistenciaClase`):
  - `PROFESOR`: cierre estricto (`fecha == hoy` + ventana ±15 min), error `ASISTENCIA_CERRADA` al incumplir.
  - `ADMIN`: bypass de cierre temporal y ownership para gestión excepcional.
  - se bloquean fines de semana
  - se bloquean fechas marcadas como `dia_no_lectivo` en el año escolar del curso
  - la fecha debe coincidir con el `diaSemana` del bloque
  - la fecha debe estar dentro del rango del año escolar del curso
  - no se permite registrar asistencia si el año escolar del curso está `CERRADO`
  - persistencia de registros por conciliación in-place (actualiza existentes, elimina huérfanos e inserta nuevos)
- Días no lectivos:
  - alta por rango con límite de 60 días corridos
  - exclusión automática de sábados y domingos
  - no permite crear fuera del rango del año escolar
  - no permite duplicado por fecha para el mismo año escolar
  - create/delete bloqueado cuando el año escolar está `CERRADO`
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
- `GET /api/profesores/{profesorId}/horario` retorna:
  - horario consolidado agrupado por día y ordenado por hora
  - resumen semanal (`totalBloques`, `diasConClase`)
  - `horasAsignadas` calculadas sobre el año consultado
- `GET /api/apoderado/mis-alumnos` retorna:
  - hijos vinculados al apoderado autenticado
  - `cursoId/cursoNombre/anoEscolarId` cuando existe matrícula `ACTIVA` en año activo
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/mensual` retorna:
  - lista `dias[]` solo con fechas que tienen registros
  - por día: `totalBloques`, `bloquesPresente`, `bloquesAusente`, `estado`
  - lista `diasNoLectivos[]` para el mismo mes (`id,fecha,tipo,descripcion`)
- `GET /api/profesor/mis-clases-hoy` retorna:
  - `diaNoLectivo` cuando la fecha actual está marcada como excepción en calendario
  - `diaNoLectivo = null` en días lectivos normales
- `GET /api/dias-no-lectivos` retorna:
  - lista ordenada ascendente por fecha
  - cada elemento con `id,fecha,tipo,descripcion`
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/resumen` retorna:
  - `totalClases`, `totalPresente`, `totalAusente`, `porcentajeAsistencia`
- `GET /api/apoderados/buscar-por-rut?rut=...` retorna:
  - datos del apoderado
  - `alumnos[]` vinculados con `id,nombre,apellido,cursoNombre`
- `POST /api/alumnos/con-apoderado` retorna:
  - `AlumnoResponse` con el objeto `apoderado` poblado (`id,nombre,apellido,rut,vinculo`)

- `GET /api/alumnos` y `GET /api/alumnos/{id}`:
  - si se envía año escolar (prioridad `X-Ano-Escolar-Id` sobre `anoEscolarId` query), agrega datos de matrícula activa (`curso`, `grado`, `estado`, `fechaMatricula`).
  - en `GET /api/alumnos/{id}`, además agrega `apoderado` principal si existe vínculo.
- `GET /api/alumnos/buscar-por-rut?rut=...`:
  - endpoint dedicado para búsqueda exacta por RUT en frontend
  - acepta RUT con o sin formato (`9.057.419-9`, `9057419-9`, `90574199`)
  - puede enriquecer con matrícula si se envía año escolar (header o query fallback).

### Paginación

Contratos usados (`MateriaPageResponse`, `AlumnoPageResponse`, `CursoPageResponse`, `ProfesorPageResponse`, `MallaCurricularPageResponse`, `MatriculaPageResponse`):

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
  - filtros `cursoId`, `gradoId` y año escolar por `X-Ano-Escolar-Id` (o `anoEscolarId` query fallback)
- `GET /api/cursos`:
  - año escolar por `X-Ano-Escolar-Id` (o `anoEscolarId` query fallback), `gradoId`
  - paginación/orden: `page,size,sortBy,sortDir`
- `GET /api/malla-curricular`:
  - año escolar obligatorio por `X-Ano-Escolar-Id` (o `anoEscolarId` query fallback)
  - paginación: `page,size` (orden fijo backend)
- `GET /api/malla-curricular/materia/{materiaId}`:
  - año escolar por `X-Ano-Escolar-Id` (o `anoEscolarId` query fallback)
  - paginación: `page,size` (orden fijo backend)
- `GET /api/malla-curricular/grado/{gradoId}`:
  - año escolar por `X-Ano-Escolar-Id` (o `anoEscolarId` query fallback)
  - paginación: `page,size` (orden fijo backend)
- `GET /api/profesores`:
  - paginación/orden: `page,size,sortBy,sortDir`
- `GET /api/matriculas/curso/{cursoId}`:
  - paginación/orden: `page,size,sortBy,sortDir`
- `GET /api/matriculas/alumno/{alumnoId}`:
  - paginación/orden: `page,size,sortBy,sortDir`
- `GET /api/cursos/{cursoId}/jornada`:
  - filtro opcional `diaSemana`
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/mensual`:
  - `mes`, `anio`
- `GET /api/dias-no-lectivos`:
  - filtros opcionales `mes`, `anio` (deben enviarse juntos)
- `GET /api/apoderado/alumnos/{alumnoId}/asistencia/resumen`:
  - año escolar por `X-Ano-Escolar-Id` (o `anoEscolarId` query fallback)

### Uso de año escolar para enriquecer respuestas

Regla práctica actual:

- Prioridad de resolución: `X-Ano-Escolar-Id` > `anoEscolarId` query/body.
- En alumnos, el año escolar activa la resolución de matrícula y permite filtrar por curso/grado.
- Sin año escolar, el backend devuelve solo ficha personal del alumno.

### Contrato de caching HTTP en GET

- Respuestas `GET /api/*` incluyen `ETag` calculado por backend.
- Si el cliente envía `If-None-Match` con el mismo valor, la API responde `304 Not Modified` (sin body).
- `Cache-Control` depende del grupo de endpoint:
  - `max-age=120` para `/api/dias-no-lectivos`.
  - `no-cache` para catálogos/configuración (revalidación siempre con servidor).
  - `no-store` para datos transaccionales.
  - `no-store, private` para datos sensibles de usuario.

---

## SECCIÓN 13: CONFIGURACIÓN

Archivos:

- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/resources/application.yml`
- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/resources/application-dev.yml`
- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/resources/application-prod.yml`
- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/config/AuditoriaAspect.java`
- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/config/CacheConfig.java`
- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/config/CacheControlInterceptor.java`
- `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/src/main/java/com/schoolmate/api/config/JacksonConfig.java`

### Propiedades relevantes

- `spring.profiles.active=dev`
- `server.port=8080`
- datasource Postgres Supabase (`url`, `username`, `password`, `driver-class-name`)
- JPA:
  - `ddl-auto=validate`
  - dialect PostgreSQL
  - `open-in-view=false` (OSIV desactivado)
  - `show-sql` true en dev / false en prod
- Flyway:
  - enabled
  - location `classpath:db/migration`
  - en dev: `baseline-on-migrate=true`, `validate-on-migrate=true`

### JWT

- `jwt.secret`
- `jwt.expiration=900000` (15 min para access token)

### CORS

`CorsConfig` permite orígenes:

- `http://localhost:5173`
- `http://localhost:8080`
- `http://localhost:3000`

Métodos permitidos: `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

Headers permitidos: `*` (incluye `X-Ano-Escolar-Id`).

### Variables de entorno necesarias

No hay uso explícito de `${ENV_VAR}` en YAML actual; credenciales y secretos están hardcodeados en archivos de configuración.

### Caching HTTP operativo

- `ETag` automático en `GET /api/*` mediante `ShallowEtagHeaderFilter`.
- `Cache-Control` asignado por `CacheControlInterceptor` según patrón de ruta.
- `/api/dias-no-lectivos` usa `Cache-Control: max-age=120`.
- Seguridad no fuerza `Cache-Control` global (se deshabilitó el header default de Spring Security).

---

## SECCIÓN 14: ÍNDICES Y PERFORMANCE

### Índices existentes (migraciones)

- `usuario`: `idx_usuario_email`, `idx_usuario_rol`
- `sesion_usuario`: `idx_sesion_usuario_usuario`, `idx_sesion_usuario_created`, `idx_sesion_usuario_usuario_created`
- `evento_auditoria`: `idx_evento_auditoria_usuario`, `idx_evento_auditoria_created`, `idx_evento_auditoria_endpoint`, `idx_evento_auditoria_usuario_created`, `idx_evento_auditoria_metodo`, `idx_evento_auditoria_request_body (GIN)`
- `ano_escolar`: `idx_ano_escolar_activo`, `idx_ano_escolar_ano`
- `grado`: `idx_grado_nivel`
- `profesor`: `idx_profesor_email`, `idx_profesor_activo`
- `curso`: `idx_curso_grado`, `idx_curso_ano_escolar`, `idx_curso_activo`, `uq_curso_grado_ano_letra`
- `malla_curricular`: `idx_malla_curricular_ano_escolar`, `idx_malla_curricular_materia_ano`, `idx_malla_curricular_grado_ano`, `idx_malla_curricular_activo`
- `matricula`: `idx_matricula_alumno`, `idx_matricula_curso`, `idx_matricula_ano_escolar`, `idx_matricula_estado`, `uq_matricula_alumno_ano_activa`
- `dia_no_lectivo`: `idx_dia_no_lectivo_ano_escolar`, `idx_dia_no_lectivo_fecha`, `uq_dia_no_lectivo_ano_fecha`
- `asistencia_clase`: índice de trazabilidad `idx_asistencia_clase_registrado_por`
- `bloque_horario`: tabla existente en Supabase; índices/constraints no versionados en migraciones del repo

### Índices recomendados pendientes

- Compuesto para query frecuente de matrículas activas por curso:
  - `(curso_id, estado)` en `matricula`.
- Compuesto para filtrado por año + estado en `matricula`:
  - `(ano_escolar_id, estado)` si crece volumen.
- Si se intensifica búsqueda por RUT parcial en alumnos, considerar estrategia específica (índice funcional/GIN según patrón real).

### Queries potencialmente problemáticas a escala

- Filtrado por grado en alumnos (`getAlumnoIdsByMatriculaFilters`) hace parte en memoria tras consulta por año.
- `ObtenerDetalleCurso.execute`: calcula malla y agregados por request; puede crecer en costo si se vuelve endpoint masivo.

### Optimizaciones aplicadas recientemente

- OSIV desactivado (`spring.jpa.open-in-view=false`) para evitar lazy loading implícito en serialización.
- Cargas explícitas de relaciones en curso/jornada con `@EntityGraph` y `JOIN FETCH` para reducir N+1.
- Listados de `cursos`, `profesores`, `malla curricular` y `matrículas` migrados a paginación en BD para evitar respuestas masivas no acotadas.
- `ObtenerProfesores` usa `findPageWithMaterias` (`@EntityGraph`) para evitar lazy/N+1 al mapear `materias` en `ProfesorResponse`.
- `ObtenerClasesHoyProfesor` eliminó N+1 en `cantidadAlumnos`/`asistenciaTomada` usando consultas batch por curso y por bloque.
- `AlumnoController.getMatriculaMap` ahora consulta por `alumno_id IN (...)` (IDs de la página actual) en vez de traer todas las matrículas activas del año.
- Validación de solapamiento en años escolares movida a queries de existencia (`existsSolapamiento*`) en BD, eliminando `findAll()+loop`.
- Endpoints GET con CRUD/repository directo marcados con `@Transactional(readOnly = true)` para reducir dirty-checking/flush innecesario.
- Revalidación HTTP en GET con `ETag` + `If-None-Match` (`304`) y políticas `Cache-Control` por endpoint.
- Auditoría automática de mutaciones HTTP con persistencia desacoplada (AOP + `REQUIRES_NEW`) para trazabilidad operativa.
- Guardado de asistencia refactorizado a merge in-place de `registro_asistencia` (sin `DELETE + INSERT` masivo), preservando UUIDs y trazabilidad.

---

## SECCIÓN 15: ESTADO ACTUAL Y LO QUE FALTA

### Estado por módulo

| Módulo | Estado | Endpoints implementados |
|---|---|---|
| Auth | ✅ | `/api/auth/login`, `/api/auth/refresh`, `/api/auth/me` |
| Años Escolares | ✅ | list/get/get activo/create/update |
| Grados | ✅ | list/get |
| Materias | ✅ | list/get/create/update/delete |
| Malla Curricular | ✅ | list por año, por materia, por grado, create, update, bulk, delete lógico |
| Calendario Días No Lectivos | ✅ | list (`GET`), create por rango (`POST`), delete (`DELETE`) |
| Cursos | ✅ | list/get/create/update |
| Profesores | ✅ | list/get/create/update + sesiones de login por profesor |
| Auditoría | ✅ | `GET /api/auditoria` (consulta paginada con filtros) + captura automática en mutaciones HTTP |
| Apoderados (admin) | ✅ | create/vincular, buscar por RUT, obtener por alumno |
| Portal Apoderado | ✅ parcial | mis-alumnos, asistencia mensual, resumen asistencia |
| Alumnos | ✅ | list/get/create/update + `POST /api/alumnos/con-apoderado` |
| Matrículas | ✅ | create, list por curso, historial por alumno, cambio estado |
| Jornada escolar por curso | ✅ | guardar día, obtener jornada/resumen (admin + apoderado con ownership en GET), copiar día, eliminar día, asignar/quitar materia y profesor por bloque, resúmenes de asignación |
| Ownership por rol no-admin | ⚠️ parcial | profesor (horario, asistencia, matrículas por curso) + apoderado (portal y lectura de jornada por curso) |
| Asistencia | ✅ parcial | registro/consulta por profesor + consulta mensual/resumen por apoderado |
| Reportes | ❌ | no existe |
| Dashboards | ✅ parcial | `GET /api/dashboard/admin/resumen` (KPIs base administrativos) |

### Qué falta para que frontend deje DataContext completamente

- Completar la segunda fase de migración de `anoEscolarId`: eliminar parámetros/body legacy cuando frontend use 100% `X-Ano-Escolar-Id`.
- Completar ownership en backend para más dominios (`principal.profesorId/apoderadoId`) fuera de los ya cubiertos.
- Extender capacidades de `APODERADO` a más vistas académicas (más allá de asistencia y jornada).
- Expandir dashboard con KPIs adicionales (asistencia, riesgo, tendencia temporal, cortes por grado/curso).
- Completar módulos pendientes (reportes y dashboard avanzado) que normalmente DataContext simula.

### Próximos módulos lógicos

1. Reportes (académico, matrícula, carga docente).
2. Dashboards avanzados (KPIs por año/curso/docente + tendencias).
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

### Convención Lombok en entidades JPA

- No usar `@Data` en clases `@Entity` con relaciones JPA.
- Usar `@Getter` + `@Setter` y constructores explícitos.
- Reservar `@Data` para DTOs o value objects; en claves embebidas (`@Embeddable`) se permite cuando se requiere igualdad por valor.

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

### Skills locales para agentes

- Catálogo local en `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/AGENTS.md`.
- Skills propias del repo:
  - `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/skills/spring-architecture-auditor/SKILL.md`
  - `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/skills/spring-architecture-reviewer/SKILL.md`
  - `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/skills/spring-antipattern-sniper/SKILL.md`
  - `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/skills/spring-clean-refactorer/SKILL.md`
- Referencia de reglas de refactor para la skill ejecutora:
  - `/Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/skills/spring-clean-refactorer/references/architecture-rules.md`

---

## SECCIÓN FINAL: CÓDIGO MUERTO, INCONSISTENCIAS Y RIESGOS

### Hallazgos críticos

1. No hay hallazgos críticos nuevos abiertos tras `V23` para bootstrap de esquema base.
2. Riesgo operativo residual: asegurar ejecución de `V23` en todos los entornos no productivos para eliminar diferencias históricas.

### Hallazgos importantes

3. `UnauthorizedException` existe pero no se usa en ningún flujo.

4. Reglas de ownership implementadas de forma parcial:
- ya existe ownership para `PROFESOR` (horario/asistencia/matrículas por curso) y `APODERADO` (portal + lectura de jornada por curso), pero faltan más dominios con permisos finos por rol.

5. Secretos hardcodeados:
- datasource password y `jwt.secret` están en YAML versionado.

### Impacto operativo

- `V23` reduce de forma significativa el riesgo de fallas de arranque en entornos nuevos por desalineación de esquema.
- Riesgo de seguridad por exposición de secretos en repositorio.
