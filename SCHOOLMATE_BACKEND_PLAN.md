# SchoolMate Hub — Backend API

> **Proyecto**: schoolmate-hub-api  
> **Stack**: Java 17+ · Spring Boot 3.x · Spring Data JPA · PostgreSQL (Supabase) · Spring Security + JWT  
> **Versión del Plan**: 1.2.0  
> **Fecha**: Febrero 2026

---

## 1. ¿QUÉ ES ESTE PROYECTO?

Este es el backend REST API de **SchoolMate Hub**, un Sistema de Gestión Escolar que actualmente funciona como una aplicación frontend en React con datos mock en memoria. El objetivo es reemplazar esa capa de datos ficticios por un backend real con persistencia en base de datos.

### ¿Por qué nace?

El frontend de SchoolMate Hub ya está completo y funcional con tres portales (Administrador, Profesor, Apoderado), pero toda la información se pierde al recargar la página porque vive en memoria. Para que el sistema sea usable en un entorno real necesitamos:

- **Persistencia real** de datos en base de datos
  - **Autenticación segura** con JWT (no credenciales hardcodeadas)
  - **Control de acceso** a nivel de API (que un apoderado no pueda ver datos de otro alumno)
  - **Escalabilidad** para múltiples usuarios concurrentes

### ¿Quién lo consume?

El frontend React existente (`schoolmate-hub`). La migración se hará vista por vista: cada módulo del frontend dejará de usar el `DataContext` local y pasará a consumir endpoints REST de este backend.

---

## 2. ARQUITECTURA

```
┌─────────────────┐         ┌──────────────────────────┐         ┌─────────────────┐
│                 │  HTTP   │                          │  JDBC   │                 │
│  React Frontend │────────▶│   Spring Boot API        │────────▶│   PostgreSQL    │
│  (puerto 5173)  │◀────────│   (puerto 8080)          │◀────────│   (Supabase)    │
│                 │  JSON   │                          │         │                 │
└─────────────────┘         │  ┌────────────────────┐  │         └─────────────────┘
                            │  │ Controllers (REST)  │  │
                            │  │ UseCases (Acciones) │  │
                            │  │ Repositories (JPA)  │  │
                            │  │ Security (JWT)      │  │
                            │  └────────────────────┘  │
                            └──────────────────────────┘
```

### Capas del Backend

| Capa | Responsabilidad |
|------|----------------|
| **Controller** | Recibir requests HTTP, validar entrada, llamar al use case, retornar response |
| **UseCase** | UNA acción de negocio concreta. Clase con un solo método público `execute()` |
| **Repository** | Acceso a base de datos via Spring Data JPA |
| **Security** | Autenticación JWT, autorización por rol, filtros |
| **DTO** | Objetos de transferencia entre capas (request/response) |
| **Entity** | Mapeo JPA de tablas de la base de datos |

---

## 3. USE CASES — FILOSOFÍA Y REGLAS

### ¿Por qué use cases en vez de services?

Con services tradicionales terminas con clases como `AsistenciaService.java` de 500 líneas con 15 métodos mezclados. Cuando necesitas modificar "guardar asistencia", tienes que navegar toda la clase para encontrar qué tocar y qué no romper.

Con use cases, cada acción es una clase independiente. Si necesitas cambiar cómo se guarda la asistencia, abres `GuardarAsistenciaClase.java` y ahí está todo. Nada más.

### Reglas (mantenerlo simple)

1. **Un use case = una clase = una acción de negocio**
   2. **Un solo método público**: `execute(...)` — siempre se llama `execute`
   3. **La clase se nombra como verbo + sustantivo**: `GuardarAsistenciaClase`, `ObtenerClasesHoyProfesor`, `LoginUsuario`
   4. **Sin interfaces ni abstracciones**: la clase es concreta, directa, inyecta repositorios y listo
   5. **Sin herencia**: no hay `BaseUseCase<T>` ni genéricos
   6. **Se agrupa por dominio en carpetas**, no en una carpeta gigante
   7. **Si la acción es CRUD simple** (listar, obtener por ID), va directo del controller al repository. No forzar use cases donde no hay lógica.

### ¿Cuándo SÍ crear un use case?

- Hay lógica de negocio (validaciones, cálculos, reglas)
  - Se tocan múltiples repositorios en una operación
  - El controller tendría más de 5 líneas de lógica

### ¿Cuándo NO crear un use case?

- CRUD simple: listar todos, obtener por ID, crear/actualizar sin reglas especiales
  - En esos casos el controller llama directo al repository

### Anatomía de un use case

```java
// src/main/java/com/schoolmate/api/usecase/asistencia/GuardarAsistenciaClase.java

@Component
@RequiredArgsConstructor
public class GuardarAsistenciaClase {

    private final AsistenciaClaseRepository asistenciaRepo;
    private final AsignacionRepository asignacionRepo;
    private final AlumnoRepository alumnoRepo;

    @Transactional
    public AsistenciaClase execute(GuardarAsistenciaRequest request, String profesorId) {
        // 1. Validar que la asignación existe y pertenece al profesor
        var asignacion = asignacionRepo.findById(request.getAsignacionId())
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));
        
        if (!asignacion.getProfesorId().equals(profesorId)) {
            throw new UnauthorizedException("No tienes permiso para esta asignación");
        }

        // 2. Validar ventana de tiempo
        validarVentanaDeTiempo(asignacion);

        // 3. Crear o actualizar asistencia
        var asistencia = asistenciaRepo
            .findByAsignacionIdAndFecha(request.getAsignacionId(), request.getFecha())
            .orElse(new AsistenciaClase());
        
        asistencia.setFecha(request.getFecha());
        asistencia.setAsignacion(asignacion);
        asistencia.setCompletada(true);
        
        // 4. Crear registros
        var registros = request.getRegistros().stream()
            .map(r -> RegistroAsistencia.builder()
                .alumno(alumnoRepo.getReferenceById(r.getAlumnoId()))
                .estado(r.getEstado())
                .observacion(r.getObservacion())
                .horaRegistro(LocalTime.now())
                .build())
            .toList();
        
        asistencia.setRegistros(registros);
        return asistenciaRepo.save(asistencia);
    }

    private void validarVentanaDeTiempo(Asignacion asignacion) {
        // Lógica de estados: bloqueada, disponible, en_curso, finalizando, expirada
    }
}
```

### Cómo se usa desde el controller

```java
@RestController
@RequestMapping("/api/asistencia")
@RequiredArgsConstructor
public class AsistenciaController {

    private final GuardarAsistenciaClase guardarAsistenciaClase;
    private final ObtenerAsistenciaClase obtenerAsistenciaClase;

    @PostMapping("/clase")
    public ResponseEntity<?> guardar(@Valid @RequestBody GuardarAsistenciaRequest request,
                                      @AuthenticationPrincipal UserPrincipal user) {
        var result = guardarAsistenciaClase.execute(request, user.getProfesorId());
        return ResponseEntity.ok(result);
    }

    // CRUD simple → directo al repo, sin use case
    @GetMapping("/clase/{asignacionId}")
    public ResponseEntity<?> obtener(@PathVariable String asignacionId,
                                      @RequestParam String fecha) {
        var result = obtenerAsistenciaClase.execute(asignacionId, fecha);
        return ResponseEntity.ok(result);
    }
}
```

---

## 4. ROLES, PERMISOS Y SEGURIDAD

### 4.1 Los tres roles del sistema

| Rol | Quién es | Qué puede hacer | Qué NO puede hacer |
|-----|----------|-----------------|---------------------|
| **ADMIN** | Director/Coordinador del colegio | Todo: gestionar años, grados, materias, cursos, profesores, alumnos, ver reportes globales, cambiar estados de reportes | Tomar asistencia (no es profesor) |
| **PROFESOR** | Docente con clases asignadas | Tomar asistencia de SUS clases, ver SUS horarios, crear reportes de comportamiento, ver SUS alumnos | Ver datos de otros profesores, gestionar el catálogo (materias, cursos), cambiar estado de reportes |
| **APODERADO** | Padre/Madre/Tutor de UN alumno | Ver info de SU hijo, ver horario del curso de SU hijo, ver calendario de asistencia de SU hijo | Ver datos de otros alumnos, modificar cualquier dato, crear reportes |

### 4.2 Matriz de permisos detallada

#### Gestión académica (solo Admin)

| Recurso | ADMIN | PROFESOR | APODERADO |
|---------|-------|----------|-----------|
| Años escolares (CRUD) | ✅ | ❌ | ❌ |
| Grados (lectura) | ✅ | ❌ | ❌ |
| Materias (CRUD) | ✅ | ❌ | ❌ |
| Cursos (CRUD) | ✅ | ❌ | ❌ |
| Profesores (CRUD) | ✅ | ❌ | ❌ |
| Alumnos (CRUD) | ✅ | ❌ | ❌ |
| Carga académica | ✅ | ❌ | ❌ |

#### Datos compartidos (con restricción de propiedad)

| Recurso | ADMIN | PROFESOR | APODERADO |
|---------|-------|----------|-----------|
| Horario de un curso | ✅ Todos | ✅ Solo sus cursos | ✅ Solo el curso de su hijo |
| Alumnos de un curso | ✅ Todos | ✅ Solo sus cursos | ❌ |
| Detalle de alumno | ✅ Todos | ❌ | ✅ Solo su hijo |

#### Asistencia

| Acción | ADMIN | PROFESOR | APODERADO |
|--------|-------|----------|-----------|
| Tomar/guardar asistencia | ❌ | ✅ Solo sus asignaciones | ❌ |
| Ver asistencia de una clase | ✅ Todos | ✅ Solo sus clases | ❌ |
| Ver asistencia de un alumno (resumen) | ✅ Todos | ❌ | ✅ Solo su hijo |
| Ver calendario mensual de un alumno | ❌ | ❌ | ✅ Solo su hijo |
| Ver asistencia promedio de un curso | ✅ | ❌ | ❌ |

#### Reportes de comportamiento

| Acción | ADMIN | PROFESOR | APODERADO |
|--------|-------|----------|-----------|
| Listar todos los reportes | ✅ | ❌ | ❌ |
| Ver reportes de un alumno | ✅ | ✅ Solo sus reportes | ✅ Solo su hijo |
| Crear reporte | ❌ | ✅ Solo alumnos de sus cursos | ❌ |
| Cambiar estado de reporte | ✅ | ❌ | ❌ |

#### Dashboards

| Dashboard | ADMIN | PROFESOR | APODERADO |
|-----------|-------|----------|-----------|
| Admin (stats globales) | ✅ | ❌ | ❌ |
| Profesor (mis clases hoy) | ❌ | ✅ Solo el suyo | ❌ |
| Apoderado (info de mi hijo) | ❌ | ❌ | ✅ Solo el suyo |

### 4.3 Regla de propiedad (ownership)

La mayoría de restricciones no son solo de rol sino de **propiedad**. No basta con ser PROFESOR, hay que ser EL profesor de esa asignación. Esto se valida en dos niveles:

**Nivel 1 — Rol** (¿tiene el rol correcto?): Se resuelve con anotaciones de Spring Security en el controller.

**Nivel 2 — Propiedad** (¿es SU recurso?): Se resuelve en el use case comparando el ID del usuario autenticado contra el recurso.

```
Request llega
    ↓
¿Tiene JWT válido? → No → 401 Unauthorized
    ↓ Sí
¿Tiene el rol requerido? → No → 403 Forbidden     ← Spring Security (anotación)
    ↓ Sí
¿Es dueño del recurso? → No → 403 Forbidden        ← Use case (lógica)
    ↓ Sí
Ejecutar operación ✅
```

### 4.4 Implementación con Spring Security

Spring Security tiene anotaciones nativas que resuelven el Nivel 1 sin código manual. Se usa `@PreAuthorize` con el método `hasRole()` o `hasAnyRole()`.

#### Configuración base

```java
// config/SecurityConfig.java

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← Esto habilita @PreAuthorize en controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()   // Login público
                .anyRequest().authenticated()                   // Todo lo demás requiere JWT
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### UserPrincipal — El usuario autenticado

```java
// security/UserPrincipal.java

@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {
    
    private final UUID id;
    private final String email;
    private final String password;
    private final Rol rol;
    private final UUID profesorId;   // null si no es PROFESOR
    private final UUID alumnoId;     // null si no es APODERADO

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        // Retorna: ROLE_ADMIN, ROLE_PROFESOR, o ROLE_APODERADO
    }

    @Override
    public String getUsername() { return email; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
```

#### Uso en controllers — @PreAuthorize

```java
@RestController
@RequestMapping("/api/anos-escolares")
@RequiredArgsConstructor
public class AnoEscolarController {

    private final AnoEscolarRepository repository;
    private final ActivarAnoEscolar activarAnoEscolar;

    // Solo ADMIN puede acceder
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AnoEscolar> listar() {
        return repository.findAll();
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public AnoEscolar activar(@PathVariable UUID id) {
        return activarAnoEscolar.execute(id);
    }
}
```

```java
@RestController
@RequestMapping("/api/asistencia")
@RequiredArgsConstructor
public class AsistenciaController {

    private final GuardarAsistenciaClase guardarAsistenciaClase;

    // Solo PROFESOR puede guardar asistencia
    // La validación de propiedad (¿es SU asignación?) va dentro del use case
    @PostMapping("/clase")
    @PreAuthorize("hasRole('PROFESOR')")
    public ResponseEntity<?> guardar(@Valid @RequestBody GuardarAsistenciaRequest request,
                                      @AuthenticationPrincipal UserPrincipal user) {
        var result = guardarAsistenciaClase.execute(request, user.getProfesorId());
        return ResponseEntity.ok(result);
    }
}
```

```java
@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
public class CursoController {

    // Múltiples roles pueden ver el horario
    @GetMapping("/{id}/horario")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESOR', 'APODERADO')")
    public List<Asignacion> horario(@PathVariable UUID id,
                                     @AuthenticationPrincipal UserPrincipal user) {
        // Nivel 2: validar propiedad según rol
        // ADMIN → puede ver cualquier curso
        // PROFESOR → solo si tiene asignación en ese curso
        // APODERADO → solo si su alumno está en ese curso
        return obtenerHorarioCurso.execute(id, user);
    }
}
```

#### Validación de propiedad dentro del use case

```java
@Component
@RequiredArgsConstructor
public class GuardarAsistenciaClase {

    private final AsignacionRepository asignacionRepo;

    @Transactional
    public AsistenciaClase execute(GuardarAsistenciaRequest request, UUID profesorId) {
        
        var asignacion = asignacionRepo.findById(request.getAsignacionId())
            .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));
        
        // ← VALIDACIÓN DE PROPIEDAD: ¿esta asignación es del profesor que hizo el request?
        if (!asignacion.getProfesor().getId().equals(profesorId)) {
            throw new UnauthorizedException("Esta asignación no te pertenece");
        }

        // ... resto de la lógica
    }
}
```

### 4.5 Resumen de anotaciones

| Anotación | Qué hace | Dónde se usa |
|-----------|----------|-------------|
| `@EnableMethodSecurity` | Habilita `@PreAuthorize` en toda la app | `SecurityConfig.java` (una sola vez) |
| `@PreAuthorize("hasRole('ADMIN')")` | Solo deja pasar al rol indicado | En cada método del controller |
| `@PreAuthorize("hasAnyRole('ADMIN','PROFESOR')")` | Deja pasar a cualquiera de los roles | Endpoints compartidos |
| `@AuthenticationPrincipal UserPrincipal user` | Inyecta el usuario autenticado | Parámetro del método del controller |

### 4.6 Reglas para el agente

- **Todo endpoint tiene `@PreAuthorize`** excepto los de `/api/auth/**`
  - **Nunca confiar solo en el rol**: si el recurso tiene dueño, validar propiedad en el use case
  - **El `profesorId` y `alumnoId` siempre vienen del token**, nunca del request body. El frontend no decide quién es, el backend lo sabe por el JWT
  - **No crear roles custom ni permisos granulares**: 3 roles fijos (`ADMIN`, `PROFESOR`, `APODERADO`) resuelven todo el sistema. No necesitamos `ROLE_TOMAR_ASISTENCIA` ni `ROLE_VER_REPORTES` — eso es sobreingeniería para este proyecto

---

## 5. CATÁLOGO DE USE CASES POR DOMINIO

### 5.1 Auth

| Use Case | Clase | Descripción |
|----------|-------|-------------|
| Login | `LoginUsuario` | Valida credenciales, genera JWT, retorna datos del usuario |
| Refresh | `RefrescarToken` | Valida token existente, genera uno nuevo |

### 5.2 Años Escolares

| Operación | ¿Use case? | Descripción |
|-----------|------------|-------------|
| Listar / Obtener / Crear / Actualizar | No — CRUD directo | Controller → Repository |
| Activar año | **Sí** → `ActivarAnoEscolar` | Desactiva el actual, activa el nuevo |

### 5.3 Materias

| Operación | ¿Use case? | Descripción |
|-----------|------------|-------------|
| CRUD completo | No | Sin lógica de negocio |

### 5.4 Cursos

| Operación | ¿Use case? | Descripción |
|-----------|------------|-------------|
| CRUD básico | No | Controller → Repository |
| Obtener detalle | **Sí** → `ObtenerDetalleCurso` | Agrega alumnos, horario, profesores |

### 5.5 Profesores

| Operación | ¿Use case? | Descripción |
|-----------|------------|-------------|
| CRUD | No | Directo |
| Obtener detalle | **Sí** → `ObtenerDetalleProfesor` | Incluye asignaciones, cursos, stats |
| Clases de hoy | **Sí** → `ObtenerClasesHoyProfesor` | Filtra por día, calcula estado de clase, verifica asistencia tomada |

### 5.6 Alumnos

| Operación | ¿Use case? | Descripción |
|-----------|------------|-------------|
| CRUD | No | Directo |
| Obtener detalle | **Sí** → `ObtenerDetalleAlumno` | Incluye curso, asistencia, reportes |

### 5.7 Asistencia ⭐ (dominio más denso)

| Use Case | Clase | Descripción |
|----------|-------|-------------|
| Guardar | `GuardarAsistenciaClase` | Valida propiedad, ventana de tiempo, crea/actualiza en transacción |
| Obtener de clase | `ObtenerAsistenciaClase` | Por asignacionId + fecha, retorna registros con nombres |
| Mensual alumno | `ObtenerAsistenciaMensualAlumno` | Agrupa por día, calcula asistió/no asistió, para calendario apoderado |
| Resumen alumno | `ObtenerResumenAsistenciaAlumno` | Porcentaje, total presentes/ausentes |
| Promedio curso | `ObtenerAsistenciaPromedioCurso` | % asistencia promedio para reportes admin |

### 5.8 Reportes de Comportamiento

| Operación | ¿Use case? | Descripción |
|-----------|------------|-------------|
| Listar / Obtener | No | Filtros simples via query params |
| Crear | **Sí** → `CrearReporte` | Valida que profesor tiene asignación con el curso del alumno |
| Cambiar estado | **Sí** → `CambiarEstadoReporte` | Solo admin, valida transiciones válidas |

### 5.9 Dashboards

| Use Case | Clase | Descripción |
|----------|-------|-------------|
| Admin | `ObtenerDashboardAdmin` | Queries agregadas: totales, promedios |
| Profesor | `ObtenerDashboardProfesor` | Clases de hoy + estados, asistencia promedio |
| Apoderado | `ObtenerDashboardApoderado` | Info alumno, clases hoy, % asistencia |

### Resumen total

| Dominio | Use Cases | CRUD Directo |
|---------|-----------|-------------|
| Auth | 2 | 1 endpoint |
| Año Escolar | 1 | 4 endpoints |
| Materias | 0 | 5 endpoints |
| Cursos | 1 | 6 endpoints |
| Profesores | 2 | 5 endpoints |
| Alumnos | 1 | 5 endpoints |
| **Asistencia** | **5** | 0 |
| Reportes | 2 | 2 endpoints |
| Dashboards | 3 | 0 |
| **Total** | **~17 use cases** | **~28 endpoints directos** |

---

## 6. ESTRUCTURA DE CARPETAS

```
schoolmate-hub-api/
├── src/main/java/com/schoolmate/api/
│   ├── SchoolmateApiApplication.java
│   │
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   └── JpaConfig.java
│   │
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── UserPrincipal.java
│   │   └── CustomUserDetailsService.java
│   │
│   ├── entity/
│   │   ├── Usuario.java
│   │   ├── AnoEscolar.java
│   │   ├── Grado.java
│   │   ├── Materia.java
│   │   ├── Curso.java
│   │   ├── Profesor.java
│   │   ├── Alumno.java
│   │   ├── Asignacion.java
│   │   ├── AsistenciaClase.java
│   │   ├── RegistroAsistencia.java
│   │   └── Reporte.java
│   │
│   ├── repository/
│   │   ├── UsuarioRepository.java
│   │   ├── AnoEscolarRepository.java
│   │   ├── GradoRepository.java
│   │   ├── MateriaRepository.java
│   │   ├── CursoRepository.java
│   │   ├── ProfesorRepository.java
│   │   ├── AlumnoRepository.java
│   │   ├── AsignacionRepository.java
│   │   ├── AsistenciaClaseRepository.java
│   │   ├── RegistroAsistenciaRepository.java
│   │   └── ReporteRepository.java
│   │
│   ├── usecase/
│   │   ├── auth/
│   │   │   ├── LoginUsuario.java
│   │   │   └── RefrescarToken.java
│   │   ├── anoescolar/
│   │   │   └── ActivarAnoEscolar.java
│   │   ├── curso/
│   │   │   └── ObtenerDetalleCurso.java
│   │   ├── profesor/
│   │   │   ├── ObtenerDetalleProfesor.java
│   │   │   └── ObtenerClasesHoyProfesor.java
│   │   ├── alumno/
│   │   │   └── ObtenerDetalleAlumno.java
│   │   ├── asistencia/
│   │   │   ├── GuardarAsistenciaClase.java
│   │   │   ├── ObtenerAsistenciaClase.java
│   │   │   ├── ObtenerAsistenciaMensualAlumno.java
│   │   │   ├── ObtenerResumenAsistenciaAlumno.java
│   │   │   └── ObtenerAsistenciaPromedioCurso.java
│   │   ├── reporte/
│   │   │   ├── CrearReporte.java
│   │   │   └── CambiarEstadoReporte.java
│   │   └── dashboard/
│   │       ├── ObtenerDashboardAdmin.java
│   │       ├── ObtenerDashboardProfesor.java
│   │       └── ObtenerDashboardApoderado.java
│   │
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── AnoEscolarController.java
│   │   ├── GradoController.java
│   │   ├── MateriaController.java
│   │   ├── CursoController.java
│   │   ├── ProfesorController.java
│   │   ├── AlumnoController.java
│   │   ├── AsistenciaController.java
│   │   ├── ReporteController.java
│   │   └── DashboardController.java
│   │
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java
│   │   │   ├── GuardarAsistenciaRequest.java
│   │   │   ├── CrearReporteRequest.java
│   │   │   └── ...Request.java
│   │   └── response/
│   │       ├── AuthResponse.java
│   │       ├── DetalleCursoResponse.java
│   │       ├── ClaseHoyResponse.java
│   │       ├── AsistenciaDiariaResponse.java
│   │       ├── DashboardAdminResponse.java
│   │       └── ...Response.java
│   │
│   ├── enums/
│   │   ├── Rol.java
│   │   ├── EstadoAsistencia.java
│   │   ├── GravedadReporte.java
│   │   └── EstadoReporte.java
│   │
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── ResourceNotFoundException.java
│       ├── UnauthorizedException.java
│       └── BusinessException.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/
│       ├── V1__create_tables.sql
│       ├── V2__seed_data.sql
│       └── V3__create_indexes.sql
│
├── pom.xml
└── README.md
```

---

## 7. FLUJO COMPLETO: DEL CLICK AL DATO

### Ejemplo: Profesor guarda asistencia (con use case)

```
1. Profesor toca "Sí, guardar lista" en el frontend
                    ↓
2. React hace POST /api/asistencia/clase con JWT en header
                    ↓
3. JwtAuthenticationFilter → valida token → extrae usuario
                    ↓
4. AsistenciaController.guardar()
   - Valida DTO con @Valid
   - Extrae profesorId del token
   - Llama: guardarAsistenciaClase.execute(request, profesorId)
                    ↓
5. GuardarAsistenciaClase.execute()
   - Busca asignación → valida que pertenece al profesor
   - Valida ventana de tiempo
   - Crea/actualiza AsistenciaClase + RegistroAsistencia
   - repository.save() → PostgreSQL
                    ↓
6. Controller retorna 200 + JSON
                    ↓
7. React muestra toast "Asistencia guardada" ✅
```

### Ejemplo: Listar materias (CRUD directo, sin use case)

```
1. Admin abre la página de materias
                    ↓
2. React hace GET /api/materias con JWT
                    ↓
3. JwtAuthenticationFilter → valida token → rol ADMIN
                    ↓
4. MateriaController.listar()
   - materiaRepository.findAll()
   - Retorna JSON
                    ↓
5. React renderiza la tabla
```

---

## 8. MODELO DE BASE DE DATOS

### 7.1 Diagrama Entidad-Relación

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  ano_escolar │1───*│    curso     │*───1│    grado     │
│              │     │              │     │              │
│ id (PK)      │     │ id (PK)      │     │ id (PK)      │
│ ano          │     │ nombre       │     │ nombre       │
│ fecha_inicio │     │ letra        │     │ nivel        │
│ fecha_fin    │     │ grado_id(FK) │     └──────────────┘
│ activo       │     │ ano_id (FK)  │
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
                     │ estado       │  ← 'PRESENTE' | 'AUSENTE'
                     │ observacion  │
                     │ hora_registro│
                     └──────────────┘

   ┌──────────────┐     ┌──────────────┐
   │   materia    │     │   usuario    │
   │              │     │              │
   │ id (PK)      │     │ id (PK)      │
   │ nombre       │     │ email        │
   │ icono        │     │ password_hash│
   └──────────────┘     │ rol          │  ← ADMIN | PROFESOR | APODERADO
                        │ profesor_id  │  ← FK nullable
   ┌──────────────┐     │ alumno_id    │  ← FK nullable
   │ materia_grado│     │ activo       │
   │              │     └──────────────┘
   │ materia_id   │
   │ grado_id     │
   └──────────────┘
```

### 9.2 Notas

- **`usuario`** está separada de `profesor` y `alumno`. PROFESOR tiene `profesor_id`, APODERADO tiene `alumno_id`.
  - **`registro_asistencia`** se normaliza como tabla hija de `asistencia_clase`.
  - **Estado de asistencia binario**: solo `PRESENTE` o `AUSENTE`.

---

## 9. ENDPOINTS DE LA API

### 9.1 Autenticación

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| POST | `/api/auth/login` | `LoginUsuario` | Público |
| POST | `/api/auth/refresh` | `RefrescarToken` | Autenticado |
| GET | `/api/auth/me` | — (del token) | Autenticado |

### 9.2 Años Escolares

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| GET | `/api/anos-escolares` | — directo | Admin |
| GET | `/api/anos-escolares/{id}` | — directo | Admin |
| POST | `/api/anos-escolares` | — directo | Admin |
| PUT | `/api/anos-escolares/{id}` | — directo | Admin |
| PATCH | `/api/anos-escolares/{id}/activar` | `ActivarAnoEscolar` | Admin |

### 9.3 Grados y Materias

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| GET | `/api/grados` | — directo | Admin |
| GET | `/api/grados/{id}` | — directo | Admin |
| GET/POST/PUT/DELETE | `/api/materias[/{id}]` | — directo | Admin |

### 9.4 Cursos

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| GET | `/api/cursos` | — directo (filtros: anoEscolarId, gradoId) | Admin |
| GET | `/api/cursos/{id}` | `ObtenerDetalleCurso` | Admin |
| POST | `/api/cursos` | — directo | Admin |
| PUT | `/api/cursos/{id}` | — directo | Admin |
| GET | `/api/cursos/{id}/alumnos` | — directo | Admin, Profesor* |
| GET | `/api/cursos/{id}/horario` | — directo | Admin, Profesor*, Apoderado* |

### 9.5 Profesores

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| GET | `/api/profesores` | — directo | Admin |
| GET | `/api/profesores/{id}` | `ObtenerDetalleProfesor` | Admin |
| POST/PUT | `/api/profesores[/{id}]` | — directo | Admin |
| GET | `/api/profesores/{id}/clases-hoy` | `ObtenerClasesHoyProfesor` | Profesor* |
| GET | `/api/profesores/{id}/asignaciones` | — directo | Admin, Profesor* |
| GET | `/api/profesores/{id}/reportes` | — directo | Admin, Profesor* |

### 9.6 Alumnos

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| GET | `/api/alumnos` | — directo (filtro: cursoId) | Admin |
| GET | `/api/alumnos/{id}` | `ObtenerDetalleAlumno` | Admin, Apoderado* |
| POST/PUT | `/api/alumnos[/{id}]` | — directo | Admin |
| GET | `/api/alumnos/{id}/asistencia/mensual` | `ObtenerAsistenciaMensualAlumno` | Apoderado* |
| GET | `/api/alumnos/{id}/asistencia/resumen` | `ObtenerResumenAsistenciaAlumno` | Admin, Apoderado* |
| GET | `/api/alumnos/{id}/reportes` | — directo | Admin, Apoderado* |

### 9.7 Asistencia

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| POST | `/api/asistencia/clase` | `GuardarAsistenciaClase` | Profesor* |
| GET | `/api/asistencia/clase/{asignacionId}` | `ObtenerAsistenciaClase` | Profesor* |
| GET | `/api/asistencia/curso/{cursoId}` | `ObtenerAsistenciaPromedioCurso` | Admin |

### 9.8 Reportes

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| GET | `/api/reportes` | — directo (filtros) | Admin |
| GET | `/api/reportes/{id}` | — directo | Admin, Profesor* |
| POST | `/api/reportes` | `CrearReporte` | Profesor |
| PATCH | `/api/reportes/{id}/estado` | `CambiarEstadoReporte` | Admin |

### 9.9 Dashboards

| Método | Endpoint | Use Case | Acceso |
|--------|----------|----------|--------|
| GET | `/api/dashboard/admin` | `ObtenerDashboardAdmin` | Admin |
| GET | `/api/dashboard/profesor` | `ObtenerDashboardProfesor` | Profesor |
| GET | `/api/dashboard/apoderado` | `ObtenerDashboardApoderado` | Apoderado |

> *Profesor: solo sus datos. Apoderado: solo datos de su alumno.

---

## 10. DEPENDENCIAS (pom.xml)

```xml
<!-- Core -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation

<!-- Base de datos -->
postgresql
flyway-core

<!-- JWT -->
jjwt-api (io.jsonwebtoken 0.12.x)
jjwt-impl
jjwt-jackson

<!-- Utilidades -->
lombok

<!-- Testing -->
spring-boot-starter-test
spring-security-test
h2
```

---

## 11. PLAN DE MIGRACIÓN POR FASES

La migración es **módulo por módulo**. Cada fase crea el backend para un módulo y actualiza el frontend. El resto sigue con datos mock. **No hay breaking changes.**

El `DataContext` del frontend se va vaciando progresivamente. TanStack Query (ya instalado) reemplaza cada función.

---

### FASE 0 — Fundación (no toca frontend)

**Objetivo**: Spring Boot + BD + JWT funcionando.

**Backend**: Proyecto base, conexión Supabase, Spring Security + JWT, tabla `usuario`, 3 usuarios seed, endpoints de auth.

**Use cases**: `LoginUsuario`, `RefrescarToken`

**Criterio**: Login via API retorna JWT. Endpoints protegidos rechazan sin token.

---

### FASE 1 — Auth en frontend

**Objetivo**: Login real desde React.

**Frontend**: Servicio HTTP con JWT, AuthContext usa API en vez de mockUsers.

**Criterio**: Login contra backend. Sesión persiste. 3 roles redirigen bien.

---

### FASE 2 — Catálogo base

**Objetivo**: Años, grados, materias desde BD.

**Backend**: Tablas + seed + endpoints CRUD.

**Use cases**: `ActivarAnoEscolar`

**Frontend**: Hooks TanStack Query reemplazan DataContext en 3 páginas admin.

---

### FASE 3 — Profesores y Cursos

**Backend**: Tablas + seed + endpoints.

**Use cases**: `ObtenerDetalleCurso`, `ObtenerDetalleProfesor`

**Frontend**: Migrar páginas de profesores y cursos.

---

### FASE 4 — Alumnos

**Backend**: Tabla + seed + endpoints. Vincular apoderado.

**Use cases**: `ObtenerDetalleAlumno`

**Frontend**: Migrar páginas de alumnos.

---

### FASE 5 — Asignaciones y Horarios

**Backend**: Tabla + seed + endpoints.

**Use cases**: `ObtenerClasesHoyProfesor`

**Frontend**: Migrar horarios en los 3 portales.

---

### FASE 6 — Asistencia ⭐

**Backend**: Tablas + 5 use cases de asistencia.

**Use cases**: `GuardarAsistenciaClase`, `ObtenerAsistenciaClase`, `ObtenerAsistenciaMensualAlumno`, `ObtenerResumenAsistenciaAlumno`, `ObtenerAsistenciaPromedioCurso`

**Frontend**: Migrar toma de lista, calendario apoderado, reportes admin.

---

### FASE 7 — Reportes

**Backend**: Tabla + endpoints.

**Use cases**: `CrearReporte`, `CambiarEstadoReporte`

**Frontend**: Migrar reportes de profesor y admin.

---

### FASE 8 — Dashboards

**Use cases**: `ObtenerDashboardAdmin`, `ObtenerDashboardProfesor`, `ObtenerDashboardApoderado`

**Frontend**: Migrar los 3 dashboards.

---

### FASE 9 — Limpieza

Eliminar DataContext.tsx, datos mock, imports huérfanos. Revisar seguridad, agregar indexes, logging.

---

## 12. CONFIGURACIÓN DE SUPABASE

### Crear Proyecto
1. [supabase.com](https://supabase.com) → crear proyecto
   2. Guardar password de BD

### Connection String
Dashboard → Settings → Database → JDBC:
```
jdbc:postgresql://db.[PROJECT-REF].supabase.co:5432/postgres
```

### application-dev.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://db.[PROJECT-REF].supabase.co:5432/postgres
    username: postgres
    password: [TU_PASSWORD]
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: [GENERAR_SECRET_LARGO]
  expiration: 86400000

server:
  port: 8080
```

---

## 13. CONVENCIONES

### Código Java
- **Entidades**: singular PascalCase (`Alumno.java`)
  - **Repositories**: `{Entity}Repository`
  - **Use cases**: `{Verbo}{Sustantivo}` (`GuardarAsistenciaClase.java`)
  - **Controllers**: `{Entity}Controller`, prefijo `/api/`
  - **DTOs**: sufijo `Request` / `Response`
  - **Sin interfaces innecesarias**: clases concretas, no `IAlumnoRepository` ni `UseCase<T>`
  - **Sin services genéricos**: lógica → use case. CRUD simple → controller directo a repo.

### API REST
- Español, kebab-case: `/api/anos-escolares`
  - JSON, errores: `{ "error": "mensaje", "status": 404 }`
  - Fechas ISO 8601

### Base de datos
- snake_case, PKs UUID, timestamps automáticos, soft delete con `activo`

---

## 14. CHECKLIST POR FASE

- [ ] Migración SQL ejecuta sin errores
  - [ ] Entities JPA mapean correctamente
  - [ ] Use cases tienen tests unitarios
  - [ ] Endpoints protegidos por rol
  - [ ] Frontend consume la API
  - [ ] Datos mock del módulo ya no se usan
  - [ ] No hay breaking changes en módulos no migrados
  - [ ] `npm run build` compila
  - [ ] `mvn spring-boot:run` arranca
