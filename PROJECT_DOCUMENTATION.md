# SchoolMate Hub API - Radiografía Técnica

> Proyecto: `schoolmate-hub-api`
> Stack: Java 21 + Spring Boot 4.0.2 + Spring Security 7 + Spring Data JPA + Flyway + PostgreSQL (Supabase)
> Estado funcional: módulos administrativos operativos (Años Escolares, Grados, Materias, Malla Curricular, Cursos, Profesores, Alumnos, Matrículas)

---

## 1. Propósito y enfoque arquitectónico

SchoolMate Hub API es el backend del sistema escolar. El diseño sigue una regla práctica:

- CRUD simple: `Controller -> Repository` directo.
- Lógica de negocio con validaciones cruzadas o reglas de estado: `Use Case` dedicado.

Esto reduce sobreingeniería y mantiene claridad operacional. El sistema prioriza:

- Trazabilidad por año escolar.
- Integridad referencial en base de datos.
- Separación entre catálogo (ej. `materia`) y asignación académica (ej. `malla_curricular`).

---

## 2. Estructura del proyecto

Paquetes principales en `src/main/java/com/schoolmate/api`:

- `config`: seguridad y CORS.
- `security`: JWT filter, provider, user principal.
- `controller`: endpoints REST.
- `dto/request`, `dto/response`: contratos API.
- `entity`: modelo JPA.
- `repository`: acceso a datos.
- `specification`: filtros dinámicos (actualmente Alumno).
- `usecase`: reglas de negocio con orquestación.
- `exception`: manejo centralizado de errores.

Entidades actuales:

- `Usuario`, `Profesor`, `Alumno`
- `AnoEscolar`, `Grado`, `Materia`, `Curso`, `SeccionCatalogo`
- `MallaCurricular`, `Matricula`

Use cases actuales:

- `usecase/auth/LoginUsuario`
- `usecase/matricula/MatricularAlumno`
- `usecase/matricula/CambiarEstadoMatricula`

---

## 3. Modelo de datos y decisiones clave

### 3.1 Catálogo vs operación

- `materia`: catálogo de asignaturas (nombre, icono).
- `malla_curricular`: define qué materia aplica a qué `grado` y `ano_escolar`, con `horas_semanales` y `activo`.

Decisión: no guardar grados en `materia`; la relación está en `malla_curricular`.

### 3.2 Cursos

- `curso` representa una sección concreta (`1° Básico A`) para un año escolar.
- letra se asigna desde `seccion_catalogo` (A, B, C, ...), no la envía frontend.

Decisión: backend genera `nombre` y `letra` para mantener nomenclatura consistente.

### 3.3 Alumnos y matrícula

Refactor aplicado:

- `alumno` ya no guarda `curso_id` ni `fecha_inscripcion`.
- `matricula` es el vínculo temporal `alumno <-> curso <-> ano_escolar`.

Beneficios:

- Permite crear alumno sin asignación inmediata.
- Permite historial por año.
- Evita acoplar persona con estado académico puntual.

### 3.4 Estado de matrícula

Enum `EstadoMatricula`:

- `ACTIVA`
- `RETIRADO`
- `TRASLADADO`

Regla de integridad en BD:

- Índice único parcial: una sola matrícula `ACTIVA` por alumno y año escolar.

---

## 4. Seguridad y autorización

Autenticación:

- `POST /api/auth/login` emite JWT.
- `JwtAuthenticationFilter` valida token por request.

Autorización:

- Módulos administrativos usan `@PreAuthorize("hasRole('ADMIN')")`.

Nota operativa:

- Un error de negocio/DB nunca debe transformarse en 403.
- Se mantiene manejo global de excepciones para diferenciar validación, conflicto e interno.

---

## 5. Migraciones Flyway

Carpeta: `src/main/resources/db/migration`

- `V1__create_usuario_table.sql`
- `V2__seed_usuarios.sql`
- `V3__create_catalogo_base.sql`
- `V4__seed_catalogo_base.sql`
- `V5__create_profesores_cursos.sql`
- `V6__seed_profesores_cursos.sql`
- `V7__create_alumnos.sql`
- `V8__seed_alumnos.sql`
- `V9__create_malla_curricular.sql`
- `V10__create_seccion_catalogo.sql`
- `V11__create_matricula_refactor_alumno.sql`

Criterio aplicado:

- No tocar migraciones históricas.
- Nuevos cambios siempre por nueva versión.
- Si Supabase ya ejecutó cambios manuales, se deja migración de tracking compatible (`IF EXISTS` / `IF NOT EXISTS`).

---

## 6. Endpoints actuales (radiografía funcional)

## 6.1 Auth

- `POST /api/auth/login`: login con email/password.
- `GET /api/auth/me`: datos del usuario autenticado.

## 6.2 Años escolares

- `GET /api/anos-escolares`
- `GET /api/anos-escolares/{id}`
- `GET /api/anos-escolares/activo`
- `POST /api/anos-escolares`
- `PUT /api/anos-escolares/{id}`

## 6.3 Grados

- `GET /api/grados`
- `GET /api/grados/{id}`

## 6.4 Materias

- `GET /api/materias` (paginado + sort)
- `GET /api/materias/{id}`
- `POST /api/materias`
- `PUT /api/materias/{id}`
- `DELETE /api/materias/{id}`

## 6.5 Malla curricular

- `GET /api/malla-curricular?anoEscolarId={id}`
- `GET /api/malla-curricular/materia/{materiaId}?anoEscolarId={id}`
- `GET /api/malla-curricular/grado/{gradoId}?anoEscolarId={id}`
- `POST /api/malla-curricular`
- `PUT /api/malla-curricular/{id}`
- `POST /api/malla-curricular/bulk`
- `DELETE /api/malla-curricular/{id}` (borrado lógico)

## 6.6 Cursos

- `GET /api/cursos?anoEscolarId={id}&gradoId={id}`
- `GET /api/cursos/{id}`
- `POST /api/cursos`
- `PUT /api/cursos/{id}`

`GET /api/cursos/{id}` ahora retorna detalle enriquecido:

- base del curso
- `alumnosMatriculados` (matrículas activas)
- `cantidadMaterias`
- `totalHorasSemanales`
- `materias[]` con nombre/icono/horas

## 6.7 Profesores

- `GET /api/profesores`
- `GET /api/profesores/{id}`
- `POST /api/profesores`
- `PUT /api/profesores/{id}`

Reglas relevantes:

- `rut` único
- validaciones de duplicado declarativas (rut/email/teléfono)

## 6.8 Alumnos

- `GET /api/alumnos`
- `GET /api/alumnos/{id}`
- `POST /api/alumnos`
- `PUT /api/alumnos/{id}`

Comportamiento nuevo:

- No recibe `cursoId` ni `fechaInscripcion`.
- `GET` acepta `anoEscolarId` opcional para enriquecer con matrícula activa del año.
- Filtros `cursoId`/`gradoId` se resuelven vía `matricula`.

## 6.9 Matrículas

- `POST /api/matriculas`
- `GET /api/matriculas/curso/{cursoId}`
- `GET /api/matriculas/alumno/{alumnoId}`
- `PATCH /api/matriculas/{id}/estado`

Reglas de negocio:

- curso debe pertenecer al año indicado.
- alumno no puede tener dos matrículas activas en el mismo año.
- transiciones de estado controladas por use case.

---

## 7. Flujos críticos

### 7.1 Crear curso

Entrada mínima frontend:

- `gradoId`
- `anoEscolarId`

Backend:

1. valida existencia de grado/año
2. obtiene letra disponible desde `seccion_catalogo`
3. construye nombre (`<grado> <letra>`)
4. persiste curso

### 7.2 Configurar carga académica

- La vista de Materias consume:
  - catálogo (`/api/materias`)
  - asignación por año (`/api/malla-curricular`)
- Persistencia masiva por `POST /api/malla-curricular/bulk`.

### 7.3 Crear alumno + asignar curso

1. crear persona por `POST /api/alumnos`.
2. matricular por `POST /api/matriculas`.

Separación intencional: alta de persona y asignación académica son procesos distintos.

---

## 8. Performance y escalabilidad

Principios aplicados:

- Se evita persistir datos derivados (ej. conteos) para no generar inconsistencia.
- Se calculan agregados con queries enfocadas e índices.

Índices recomendados en Supabase para consultas actuales:

```sql
CREATE INDEX IF NOT EXISTS idx_matricula_curso_estado
ON matricula (curso_id, estado);

CREATE INDEX IF NOT EXISTS idx_malla_curricular_grado_ano_activo
ON malla_curricular (grado_id, ano_escolar_id, activo);
```

Motivo:

- acelera `alumnosMatriculados` por curso.
- acelera resumen académico por curso (materias/horas).

---

## 9. Contratos clave para frontend

### 9.1 Curso detalle enriquecido

`GET /api/cursos/{id}` incluye:

- `alumnosMatriculados: number`
- `cantidadMaterias: number`
- `totalHorasSemanales: number`
- `materias: [{ materiaId, materiaNombre, materiaIcono, horasSemanales }]`

### 9.2 Alumno

Request (`POST/PUT /api/alumnos`):

- solo datos personales + apoderado
- no `cursoId`

Response (`GET /api/alumnos` y `/api/alumnos/{id}`):

- datos de alumno siempre
- datos de matrícula solo si se envía `anoEscolarId`

---

## 10. Convenciones del proyecto

- Evitar interfaces/capas innecesarias.
- No crear services genéricos.
- Use case solo cuando hay reglas de negocio reales.
- IDs string (`VARCHAR(36)`), evitando acoplar frontend a UUID binario.
- Errores con mensajes claros y accionables.

---

## 11. Estado actual y próximos pasos recomendados

Estado implementado:

- Malla curricular activa por año escolar.
- Cursos con nomenclatura automática.
- Alumno desacoplado de curso.
- Matrícula temporal con historial.
- Detalle de curso enriquecido para operación académica.

Próximos pasos naturales:

1. Dashboard de curso con métricas de asistencia (cuando exista módulo de asistencia).
2. Endpoint paginado de matrículas por año/curso para vistas masivas.
3. Auditoría de cambios (quién cambia estados de matrícula y cuándo).
4. Pruebas de integración para reglas de matrícula y malla bulk.

---

## 12. Resumen ejecutivo

La arquitectura actual está alineada con una operación escolar real:

- Catálogos estables (`materia`, `grado`, `año`) separados de vínculos temporales (`malla_curricular`, `matricula`).
- Reglas críticas en base de datos + use cases.
- Endpoints simples para CRUD y enriquecidos para vistas operativas.
- Diseño preparado para crecer sin deuda estructural inmediata.
