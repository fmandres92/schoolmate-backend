# API Contracts Backend -> Frontend (TypeScript Sync)

Fuente de verdad: código Java actual en `schoolmate-hub-api`.
Fecha de corte: 2026-02-26.

## 0) Corrección de rutas (discrepancias detectadas)

- `GET /api/admin/cumplimiento-asistencia` **NO existe**.
  - Ruta real: `GET /api/profesores/{profesorId}/cumplimiento-asistencia`
- `GET /api/admin/dashboard` **NO existe**.
  - Ruta real: `GET /api/dashboard/admin`

---

## 1) Error contract global (aplica a endpoints protegidos y de negocio)

```json
{
  "schema": "ApiErrorResponse",
  "type": "object",
  "propiedades": {
    "code": { "tipo": "string", "nullable": false, "ejemplo": "RESOURCE_NOT_FOUND" },
    "message": { "tipo": "string", "nullable": false, "ejemplo": "Profesor no encontrado" },
    "status": { "tipo": "number", "nullable": false, "ejemplo": 404 },
    "field": { "tipo": "string", "nullable": true, "ejemplo": null },
    "path": { "tipo": "string", "nullable": false, "ejemplo": "/api/profesores/uuid/cumplimiento-asistencia" },
    "timestamp": { "tipo": "string(datetime)", "nullable": false, "ejemplo": "2026-02-26T11:35:17.551" },
    "details": {
      "tipo": "object<string,string>",
      "nullable": true,
      "ejemplo": { "alumnosInvalidos": "uuid1,uuid2" }
    }
  }
}
```

Status de error comunes por seguridad/plataforma:
- `400`: validación/request inválido (`VALIDATION_FAILED`, `BUSINESS_RULE`, `ASISTENCIA_CERRADA`)
- `401`: no autenticado/token inválido (`UNAUTHORIZED`, `AUTH_BAD_CREDENTIALS`)
- `403`: sin permisos (`ACCESS_DENIED`)
- `404`: recurso no encontrado (`RESOURCE_NOT_FOUND`)
- `409`: conflicto/integridad (`CONFLICT`, `DATA_INTEGRITY`, reglas de colisión)
- `500`: error no controlado (`INTERNAL_SERVER_ERROR`)

---

## 2) Enums (valores exactos)

```json
{
  "EstadoCumplimiento": ["TOMADA", "NO_TOMADA", "EN_CURSO", "PROGRAMADA"],
  "EstadoAsistencia": ["PRESENTE", "AUSENTE"],
  "EstadoMatricula": ["ACTIVA", "RETIRADO", "TRASLADADO"],
  "EstadoClaseHoy": ["PENDIENTE", "DISPONIBLE", "EXPIRADA"],
  "TipoBloque": ["CLASE", "RECREO", "ALMUERZO"],
  "TipoDiaNoLectivo": ["FERIADO_LEGAL", "VACACIONES", "SUSPENSION", "INTERFERIADO", "ADMINISTRATIVO"],
  "Rol": ["ADMIN", "PROFESOR", "APODERADO"],
  "EstadoAnoEscolar": ["FUTURO", "PLANIFICACION", "ACTIVO", "CERRADO"]
}
```

---

## 3) PRIORIDAD ALTA

## 3.1 Cumplimiento de Asistencia (Admin)

```json
{
  "endpoint": "GET /api/profesores/{profesorId}/cumplimiento-asistencia",
  "descripcion": "Obtiene cumplimiento de asistencia de un profesor para una fecha",
  "queryParams": ["fecha (optional, yyyy-MM-dd)"],
  "headers": ["X-Ano-Escolar-Id (required)"],
  "responses": {
    "200": {
      "descripcion": "Éxito",
      "ejemplo": {
        "profesorId": "66d02c3f-718b-4108-bb57-1d7f9a47a567",
        "profesorNombre": "Miguel Perez",
        "fecha": "2026-02-25",
        "diaSemana": 3,
        "nombreDia": "Miércoles",
        "esDiaHabil": true,
        "diaNoLectivo": null,
        "resumen": { "totalBloques": 5, "tomadas": 2, "noTomadas": 1, "enCurso": 1, "programadas": 1 },
        "bloques": [
          {
            "bloqueId": "11111111-1111-1111-1111-111111111111",
            "numeroBloque": 1,
            "horaInicio": "08:00",
            "horaFin": "08:45",
            "cursoId": "22222222-2222-2222-2222-222222222222",
            "cursoNombre": "1° Básico A",
            "materiaId": "33333333-3333-3333-3333-333333333333",
            "materiaNombre": "Matemática",
            "materiaIcono": "Calculator",
            "cantidadAlumnos": 32,
            "estadoCumplimiento": "TOMADA",
            "asistenciaClaseId": "44444444-4444-4444-4444-444444444444",
            "tomadaEn": "2026-02-25T08:05:11",
            "resumenAsistencia": { "presentes": 30, "ausentes": 2, "total": 32 }
          }
        ]
      },
      "schema": {
        "type": "object",
        "propiedades": {
          "profesorId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "66d02c3f-718b-4108-bb57-1d7f9a47a567" },
          "profesorNombre": { "tipo": "string", "nullable": false, "ejemplo": "Miguel Perez" },
          "fecha": { "tipo": "string(date)", "nullable": false, "ejemplo": "2026-02-25" },
          "diaSemana": { "tipo": "number", "nullable": false, "ejemplo": 3 },
          "nombreDia": { "tipo": "string", "nullable": false, "ejemplo": "Miércoles" },
          "esDiaHabil": { "tipo": "boolean", "nullable": false, "ejemplo": true },
          "diaNoLectivo": {
            "tipo": "object",
            "nullable": true,
            "propiedades": {
              "tipo": { "tipo": "string(enum TipoDiaNoLectivo)", "nullable": false, "ejemplo": "FERIADO_LEGAL" },
              "descripcion": { "tipo": "string", "nullable": true, "ejemplo": "Feriado nacional" }
            }
          },
          "resumen": {
            "tipo": "object",
            "nullable": false,
            "propiedades": {
              "totalBloques": { "tipo": "number", "nullable": false, "ejemplo": 5 },
              "tomadas": { "tipo": "number", "nullable": false, "ejemplo": 2 },
              "noTomadas": { "tipo": "number", "nullable": false, "ejemplo": 1 },
              "enCurso": { "tipo": "number", "nullable": false, "ejemplo": 1 },
              "programadas": { "tipo": "number", "nullable": false, "ejemplo": 1 }
            }
          },
          "bloques": {
            "tipo": "array",
            "nullable": false,
            "items": {
              "type": "object",
              "propiedades": {
                "bloqueId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "11111111-1111-1111-1111-111111111111" },
                "numeroBloque": { "tipo": "number", "nullable": false, "ejemplo": 1 },
                "horaInicio": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "08:00" },
                "horaFin": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "08:45" },
                "cursoId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "22222222-2222-2222-2222-222222222222" },
                "cursoNombre": { "tipo": "string", "nullable": false, "ejemplo": "1° Básico A" },
                "materiaId": { "tipo": "string(uuid)", "nullable": true, "ejemplo": "33333333-3333-3333-3333-333333333333" },
                "materiaNombre": { "tipo": "string", "nullable": true, "ejemplo": "Matemática" },
                "materiaIcono": { "tipo": "string", "nullable": true, "ejemplo": "Calculator" },
                "cantidadAlumnos": { "tipo": "number", "nullable": false, "ejemplo": 32 },
                "estadoCumplimiento": { "tipo": "string(enum EstadoCumplimiento)", "nullable": false, "ejemplo": "TOMADA" },
                "asistenciaClaseId": { "tipo": "string(uuid)", "nullable": true, "ejemplo": "44444444-4444-4444-4444-444444444444" },
                "tomadaEn": { "tipo": "string(datetime)", "nullable": true, "ejemplo": "2026-02-25T08:05:11" },
                "resumenAsistencia": {
                  "tipo": "object",
                  "nullable": true,
                  "propiedades": {
                    "presentes": { "tipo": "number", "nullable": false, "ejemplo": 30 },
                    "ausentes": { "tipo": "number", "nullable": false, "ejemplo": 2 },
                    "total": { "tipo": "number", "nullable": false, "ejemplo": 32 }
                  }
                }
              }
            }
          }
        }
      }
    },
    "400": { "descripcion": "Header/query inválido", "schemaRef": "ApiErrorResponse" },
    "401": { "descripcion": "No autenticado", "schemaRef": "ApiErrorResponse" },
    "403": { "descripcion": "Solo ADMIN", "schemaRef": "ApiErrorResponse" },
    "404": { "descripcion": "Profesor no encontrado", "schemaRef": "ApiErrorResponse" },
    "500": { "descripcion": "Error interno", "schemaRef": "ApiErrorResponse" }
  }
}
```

## 3.2 Dashboard Admin

```json
{
  "endpoint": "GET /api/dashboard/admin",
  "descripcion": "Resumen admin + cumplimiento global de hoy",
  "queryParams": [],
  "headers": ["X-Ano-Escolar-Id (required)"],
  "responses": {
    "200": {
      "descripcion": "Éxito",
      "ejemplo": {
        "stats": { "totalAlumnosMatriculados": 9, "totalCursos": 9, "totalProfesoresActivos": 4 },
        "cumplimientoHoy": {
          "fecha": "2026-02-25",
          "diaSemana": 3,
          "nombreDia": "Miércoles",
          "esDiaHabil": true,
          "diaNoLectivo": null,
          "resumenGlobal": {
            "totalBloques": 5,
            "tomadas": 0,
            "pendientes": 1,
            "programadas": 4,
            "profesoresConClase": 3,
            "profesoresCumplimiento100": 0
          },
          "profesores": [
            {
              "profesorId": "67a50e42-0e41-45f2-a701-ea3e3a2bc8f2",
              "nombre": "Carlos primero",
              "apellido": "mota",
              "totalBloques": 3,
              "tomadas": 0,
              "pendientes": 1,
              "programadas": 2,
              "porcentajeCumplimiento": null,
              "ultimaActividadHora": null,
              "bloquesPendientesDetalle": [
                { "horaInicio": "08:00", "horaFin": "08:45", "cursoNombre": "1° Básico A", "materiaNombre": "Historia" }
              ]
            }
          ]
        }
      },
      "schema": {
        "type": "object",
        "propiedades": {
          "stats": {
            "tipo": "object",
            "nullable": false,
            "propiedades": {
              "totalAlumnosMatriculados": { "tipo": "number", "nullable": false, "ejemplo": 9 },
              "totalCursos": { "tipo": "number", "nullable": false, "ejemplo": 9 },
              "totalProfesoresActivos": { "tipo": "number", "nullable": false, "ejemplo": 4 }
            }
          },
          "cumplimientoHoy": {
            "tipo": "object",
            "nullable": false,
            "propiedades": {
              "fecha": { "tipo": "string(date)", "nullable": false, "ejemplo": "2026-02-25" },
              "diaSemana": { "tipo": "number", "nullable": false, "ejemplo": 3 },
              "nombreDia": { "tipo": "string", "nullable": false, "ejemplo": "Miércoles" },
              "esDiaHabil": { "tipo": "boolean", "nullable": false, "ejemplo": true },
              "diaNoLectivo": {
                "tipo": "object",
                "nullable": true,
                "propiedades": {
                  "tipo": { "tipo": "string(enum TipoDiaNoLectivo)", "nullable": false, "ejemplo": "FERIADO_LEGAL" },
                  "descripcion": { "tipo": "string", "nullable": true, "ejemplo": "Feriado nacional" }
                }
              },
              "resumenGlobal": {
                "tipo": "object",
                "nullable": false,
                "propiedades": {
                  "totalBloques": { "tipo": "number", "nullable": false, "ejemplo": 5 },
                  "tomadas": { "tipo": "number", "nullable": false, "ejemplo": 0 },
                  "pendientes": { "tipo": "number", "nullable": false, "ejemplo": 1 },
                  "programadas": { "tipo": "number", "nullable": false, "ejemplo": 4 },
                  "profesoresConClase": { "tipo": "number", "nullable": false, "ejemplo": 3 },
                  "profesoresCumplimiento100": { "tipo": "number", "nullable": false, "ejemplo": 0 }
                }
              },
              "profesores": {
                "tipo": "array",
                "nullable": false,
                "items": {
                  "type": "object",
                  "propiedades": {
                    "profesorId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "67a50e42-0e41-45f2-a701-ea3e3a2bc8f2" },
                    "nombre": { "tipo": "string", "nullable": false, "ejemplo": "Carlos primero" },
                    "apellido": { "tipo": "string", "nullable": false, "ejemplo": "mota" },
                    "totalBloques": { "tipo": "number", "nullable": false, "ejemplo": 3 },
                    "tomadas": { "tipo": "number", "nullable": false, "ejemplo": 0 },
                    "pendientes": { "tipo": "number", "nullable": false, "ejemplo": 1 },
                    "programadas": { "tipo": "number", "nullable": false, "ejemplo": 2 },
                    "porcentajeCumplimiento": { "tipo": "number", "nullable": true, "ejemplo": 66.7 },
                    "ultimaActividadHora": { "tipo": "string(HH:mm)", "nullable": true, "ejemplo": "08:05" },
                    "bloquesPendientesDetalle": {
                      "tipo": "array",
                      "nullable": false,
                      "items": {
                        "type": "object",
                        "propiedades": {
                          "horaInicio": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "08:00" },
                          "horaFin": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "08:45" },
                          "cursoNombre": { "tipo": "string", "nullable": false, "ejemplo": "1° Básico A" },
                          "materiaNombre": { "tipo": "string", "nullable": true, "ejemplo": "Historia" }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "400": { "descripcion": "Header inválido", "schemaRef": "ApiErrorResponse" },
    "401": { "descripcion": "No autenticado", "schemaRef": "ApiErrorResponse" },
    "403": { "descripcion": "Solo ADMIN", "schemaRef": "ApiErrorResponse" },
    "500": { "descripcion": "Error interno", "schemaRef": "ApiErrorResponse" }
  }
}
```

## 3.3 Jornada Escolar (`/api/cursos/{cursoId}/jornada/*`)

### GET /api/cursos/{cursoId}/jornada

```json
{
  "endpoint": "GET /api/cursos/{cursoId}/jornada",
  "descripcion": "Obtiene jornada completa o filtrada por diaSemana",
  "queryParams": ["diaSemana (optional)"],
  "responses": {
    "200": {
      "descripcion": "Éxito",
      "schema": {
        "type": "object",
        "propiedades": {
          "cursoId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "5f076705-746e-4376-a25a-6e16204f04ba" },
          "cursoNombre": { "tipo": "string", "nullable": false, "ejemplo": "1° Básico A" },
          "dias": {
            "tipo": "object(map<int,JornadaDiaResponse>)",
            "nullable": false,
            "ejemplo": {
              "1": {
                "diaSemana": 1,
                "nombreDia": "Lunes",
                "bloques": [
                  {
                    "id": "11111111-1111-1111-1111-111111111111",
                    "numeroBloque": 1,
                    "horaInicio": "08:00",
                    "horaFin": "08:45",
                    "tipo": "CLASE",
                    "materiaId": "33333333-3333-3333-3333-333333333333",
                    "materiaNombre": "Matemática",
                    "materiaIcono": "Calculator",
                    "profesorId": "66d02c3f-718b-4108-bb57-1d7f9a47a567",
                    "profesorNombre": "Miguel Perez"
                  }
                ],
                "totalBloquesClase": 6,
                "horaInicio": "08:00",
                "horaFin": "13:30"
              }
            }
          },
          "resumen": {
            "tipo": "object",
            "nullable": false,
            "propiedades": {
              "cursoId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "5f076705-746e-4376-a25a-6e16204f04ba" },
              "diasConfigurados": { "tipo": "array<number>", "nullable": false, "ejemplo": [1, 2, 3, 4, 5] },
              "bloquesClasePorDia": { "tipo": "object(map<int,int>)", "nullable": false, "ejemplo": { "1": 6, "2": 6 } },
              "totalBloquesClaseSemana": { "tipo": "number", "nullable": false, "ejemplo": 30 }
            }
          }
        }
      }
    },
    "401": { "descripcion": "No autenticado", "schemaRef": "ApiErrorResponse" },
    "403": { "descripcion": "Sin permiso", "schemaRef": "ApiErrorResponse" },
    "404": { "descripcion": "Curso no encontrado", "schemaRef": "ApiErrorResponse" },
    "500": { "descripcion": "Error interno", "schemaRef": "ApiErrorResponse" }
  }
}
```

### GET /api/cursos/{cursoId}/jornada/resumen

```json
{
  "endpoint": "GET /api/cursos/{cursoId}/jornada/resumen",
  "descripcion": "Resumen semanal de bloques clase del curso",
  "queryParams": [],
  "responses": {
    "200": {
      "descripcion": "Éxito",
      "schema": {
        "type": "object",
        "propiedades": {
          "cursoId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "5f076705-746e-4376-a25a-6e16204f04ba" },
          "diasConfigurados": { "tipo": "array<number>", "nullable": false, "ejemplo": [1, 2, 3, 4, 5] },
          "bloquesClasePorDia": { "tipo": "object(map<int,int>)", "nullable": false, "ejemplo": { "1": 6, "2": 6 } },
          "totalBloquesClaseSemana": { "tipo": "number", "nullable": false, "ejemplo": 30 }
        }
      }
    },
    "403": { "descripcion": "Solo ADMIN", "schemaRef": "ApiErrorResponse" },
    "404": { "descripcion": "Curso no encontrado", "schemaRef": "ApiErrorResponse" }
  }
}
```

### GET /api/cursos/{cursoId}/jornada/asignacion-materias

```json
{
  "endpoint": "GET /api/cursos/{cursoId}/jornada/asignacion-materias",
  "descripcion": "Resumen de asignación de materias vs malla curricular",
  "queryParams": [],
  "responses": {
    "200": {
      "descripcion": "Éxito",
      "schema": {
        "type": "object",
        "propiedades": {
          "cursoId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "5f076705-746e-4376-a25a-6e16204f04ba" },
          "cursoNombre": { "tipo": "string", "nullable": false, "ejemplo": "1° Básico A" },
          "gradoNombre": { "tipo": "string", "nullable": false, "ejemplo": "1° Básico" },
          "totalBloquesClase": { "tipo": "number", "nullable": false, "ejemplo": 30 },
          "totalBloquesAsignados": { "tipo": "number", "nullable": false, "ejemplo": 27 },
          "totalBloquesSinMateria": { "tipo": "number", "nullable": false, "ejemplo": 3 },
          "totalMinutosClase": { "tipo": "number", "nullable": false, "ejemplo": 1350 },
          "totalMinutosAsignados": { "tipo": "number", "nullable": false, "ejemplo": 1215 },
          "materias": {
            "tipo": "array",
            "nullable": false,
            "items": {
              "type": "object",
              "propiedades": {
                "materiaId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "33333333-3333-3333-3333-333333333333" },
                "materiaNombre": { "tipo": "string", "nullable": false, "ejemplo": "Matemática" },
                "materiaIcono": { "tipo": "string", "nullable": false, "ejemplo": "Calculator" },
                "horasPedagogicas": { "tipo": "number", "nullable": false, "ejemplo": 6 },
                "minutosPermitidos": { "tipo": "number", "nullable": false, "ejemplo": 270 },
                "minutosAsignados": { "tipo": "number", "nullable": false, "ejemplo": 225 },
                "estado": { "tipo": "string(enum)", "nullable": false, "ejemplo": "PARCIAL" },
                "bloquesAsignados": {
                  "tipo": "array",
                  "nullable": false,
                  "items": {
                    "type": "object",
                    "propiedades": {
                      "bloqueId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "11111111-1111-1111-1111-111111111111" },
                      "diaSemana": { "tipo": "number", "nullable": false, "ejemplo": 1 },
                      "numeroBloque": { "tipo": "number", "nullable": false, "ejemplo": 1 },
                      "horaInicio": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "08:00" },
                      "horaFin": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "08:45" },
                      "duracionMinutos": { "tipo": "number", "nullable": false, "ejemplo": 45 }
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "403": { "descripcion": "Solo ADMIN", "schemaRef": "ApiErrorResponse" },
    "404": { "descripcion": "Curso no encontrado", "schemaRef": "ApiErrorResponse" }
  }
}
```

### GET /api/cursos/{cursoId}/jornada/asignacion-profesores

```json
{
  "endpoint": "GET /api/cursos/{cursoId}/jornada/asignacion-profesores",
  "descripcion": "Resumen de asignación de profesores por bloque clase",
  "queryParams": [],
  "responses": {
    "200": {
      "descripcion": "Éxito",
      "schema": {
        "type": "object",
        "propiedades": {
          "cursoId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "5f076705-746e-4376-a25a-6e16204f04ba" },
          "cursoNombre": { "tipo": "string", "nullable": false, "ejemplo": "1° Básico A" },
          "totalBloquesClase": { "tipo": "number", "nullable": false, "ejemplo": 30 },
          "bloquesConProfesor": { "tipo": "number", "nullable": false, "ejemplo": 25 },
          "bloquesSinProfesor": { "tipo": "number", "nullable": false, "ejemplo": 5 },
          "bloquesConMateriaSinProfesor": { "tipo": "number", "nullable": false, "ejemplo": 4 },
          "bloquesSinMateria": { "tipo": "number", "nullable": false, "ejemplo": 1 },
          "profesores": {
            "tipo": "array",
            "nullable": false,
            "items": {
              "type": "object",
              "propiedades": {
                "profesorId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "66d02c3f-718b-4108-bb57-1d7f9a47a567" },
                "profesorNombre": { "tipo": "string", "nullable": false, "ejemplo": "Miguel" },
                "profesorApellido": { "tipo": "string", "nullable": false, "ejemplo": "Perez" },
                "materias": { "tipo": "array<string>", "nullable": false, "ejemplo": ["Matemática", "Historia"] },
                "cantidadBloques": { "tipo": "number", "nullable": false, "ejemplo": 12 },
                "totalMinutos": { "tipo": "number", "nullable": false, "ejemplo": 540 },
                "bloques": {
                  "tipo": "array",
                  "nullable": false,
                  "items": {
                    "type": "object",
                    "propiedades": {
                      "bloqueId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "11111111-1111-1111-1111-111111111111" },
                      "diaSemana": { "tipo": "number", "nullable": false, "ejemplo": 1 },
                      "numeroBloque": { "tipo": "number", "nullable": false, "ejemplo": 1 },
                      "horaInicio": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "08:00" },
                      "horaFin": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "08:45" },
                      "materiaNombre": { "tipo": "string", "nullable": true, "ejemplo": "Matemática" }
                    }
                  }
                }
              }
            }
          },
          "bloquesPendientes": {
            "tipo": "array",
            "nullable": false,
            "items": {
              "type": "object",
              "propiedades": {
                "bloqueId": { "tipo": "string(uuid)", "nullable": false, "ejemplo": "99999999-9999-9999-9999-999999999999" },
                "diaSemana": { "tipo": "number", "nullable": false, "ejemplo": 3 },
                "numeroBloque": { "tipo": "number", "nullable": false, "ejemplo": 4 },
                "horaInicio": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "10:15" },
                "horaFin": { "tipo": "string(HH:mm)", "nullable": false, "ejemplo": "11:00" },
                "materiaNombre": { "tipo": "string", "nullable": false, "ejemplo": "Ciencias" },
                "materiaIcono": { "tipo": "string", "nullable": false, "ejemplo": "FlaskConical" }
              }
            }
          }
        }
      }
    },
    "403": { "descripcion": "Solo ADMIN", "schemaRef": "ApiErrorResponse" },
    "404": { "descripcion": "Curso no encontrado", "schemaRef": "ApiErrorResponse" }
  }
}
```

---

## 4) Catálogo resumido de schemas de éxito (resto de endpoints)

> Todos los wrappers paginados siguen el mismo patrón:
- `content`: `array`
- `page`: `number`
- `size`: `number`
- `totalElements`: `number`
- `totalPages`: `number`
- `hasNext`: `boolean`
- `hasPrevious`: `boolean`
- `sortBy/sortDir` cuando aplique

### `AlumnoResponse`
- `id(uuid)`, `rut`, `nombre`, `apellido`, `fechaNacimiento`, `activo(boolean)`, `createdAt(datetime)`, `updatedAt(datetime)`
- Apoderado flat: `apoderadoNombre?`, `apoderadoApellido?`, `apoderadoEmail?`, `apoderadoTelefono?`, `apoderadoVinculo?`
- Apoderado object: `apoderado?` -> `{ id(uuid), nombre, apellido, rut, vinculo }`
- Matrícula opcional por año: `matriculaId?`, `cursoId?`, `cursoNombre?`, `gradoNombre?`, `estadoMatricula?`, `fechaMatricula?`

### `ProfesorResponse`
- `id(uuid)`, `rut`, `nombre`, `apellido`, `email`, `telefono?`, `fechaContratacion`, `horasPedagogicasContrato?`, `horasAsignadas?`, `activo(boolean)`, `materias[]`, `createdAt`, `updatedAt`
- `materias[]`: `{ id(uuid), nombre, icono }`

### `CursoResponse`
- `id(uuid)`, `nombre`, `letra`, `gradoId(uuid)`, `gradoNombre`, `anoEscolarId(uuid)`, `anoEscolar(number)`, `activo(boolean)`
- métricas: `alumnosMatriculados(number)`, `cantidadMaterias(number)`, `totalHorasPedagogicas(number)`
- `materias[]`: `{ materiaId(uuid), materiaNombre, materiaIcono, horasPedagogicas }`
- `createdAt`, `updatedAt`

### `MatriculaResponse`
- `id(uuid)`, `alumnoId(uuid)`, `alumnoNombre`, `alumnoApellido`, `alumnoRut`, `cursoId(uuid)`, `cursoNombre`, `gradoNombre`
- `anoEscolarId(uuid)`, `anoEscolar(number)`, `fechaMatricula(date)`, `estado(enum EstadoMatricula)`, `createdAt`, `updatedAt`

### `MateriaResponse`
- `id(uuid)`, `nombre`, `icono`, `createdAt`, `updatedAt`

### `MallaCurricularResponse`
- `id(uuid)`, `materiaId(uuid)`, `materiaNombre`, `materiaIcono`, `gradoId(uuid)`, `gradoNombre`, `gradoNivel(number)`
- `anoEscolarId(uuid)`, `anoEscolar(number)`, `horasPedagogicas(number)`, `activo(boolean)`, `createdAt`, `updatedAt`

### `AnoEscolarResponse`
- `id(uuid)`, `ano(number)`, `fechaInicioPlanificacion(date)`, `fechaInicio(date)`, `fechaFin(date)`
- `estado(enum EstadoAnoEscolar)`, `createdAt(datetime string)`, `updatedAt(datetime string)`

### `DiaNoLectivoResponse`
- `id(uuid)`, `fecha(date)`, `tipo(enum TipoDiaNoLectivo)`, `descripcion?`

### `AsistenciaClaseResponse`
- `asistenciaClaseId(uuid)`, `bloqueHorarioId(uuid)`, `fecha(date)`, `tomadaEn(datetime)`, `registradoPorNombre?`, `registros[]`
- `registros[]`: `{ alumnoId(uuid), alumnoNombre, alumnoApellido, estado(enum EstadoAsistencia), observacion? }`

### `ProfesorHorarioResponse`
- base: `profesorId(uuid)`, `profesorNombre`, `anoEscolarId(uuid)`, `anoEscolar(number)`, `horasPedagogicasContrato?`, `horasAsignadas?`
- `resumenSemanal`: `{ totalBloques(number), diasConClase(number[]) }`
- `dias[]`: `{ diaSemana(number), diaNombre, bloques[] }`
- `bloques[]`: `{ bloqueId(uuid), horaInicio, horaFin, duracionMinutos, cursoId(uuid), cursoNombre, materiaId(uuid), materiaNombre, materiaIcono }`

### `SesionProfesorPageResponse`
- `profesorId(uuid)`, `profesorNombre`, `sesiones[]`, `totalElements(number)`, `totalPages(number)`, `currentPage(number)`
- `sesiones[]`: `{ id(uuid), fechaHora(datetime), ipAddress, latitud?(number), longitud?(number), precisionMetros?(number), userAgent? }`

### `ClasesHoyResponse`
- `fecha(date)`, `diaSemana(number)`, `nombreDia`, `diaNoLectivo?`, `clases[]`
- `diaNoLectivo?`: `DiaNoLectivoResponse`
- `clases[]`: `{ bloqueId(uuid), numeroBloque, horaInicio, horaFin, cursoId(uuid), cursoNombre, materiaId(uuid), materiaNombre, materiaIcono, cantidadAlumnos, estado(enum EstadoClaseHoy), asistenciaTomada(boolean) }`

### `AsistenciaMensualResponse`
- `alumnoId(uuid)`, `alumnoNombre`, `mes(number)`, `anio(number)`, `dias[]`, `diasNoLectivos[]`
- `dias[]`: `{ fecha(date string), totalBloques, bloquesPresente, bloquesAusente, estado }`
- `diasNoLectivos[]`: `DiaNoLectivoResponse`

### `ResumenAsistenciaResponse`
- `alumnoId(uuid)`, `alumnoNombre`, `totalClases(number)`, `totalPresente(number)`, `totalAusente(number)`, `porcentajeAsistencia(number)`

### `ApoderadoResponse`
- `id(uuid)`, `nombre`, `apellido`, `rut`, `email`, `telefono?`, `usuarioId(uuid)?`, `cuentaActiva(boolean)`, `alumnos[]`
- `alumnos[]`: `{ id(uuid), nombre, apellido }`

### `ApoderadoBuscarResponse`
- `id(uuid)?`, `nombre?`, `apellido?`, `rut?`, `email?`, `telefono?`, `existe(boolean)`, `alumnos[]`
- `alumnos[]`: `{ id(uuid), nombre, apellido, cursoNombre? }`

### `AlumnoApoderadoResponse`
- `id(uuid)`, `nombre`, `apellido`, `cursoId(uuid)?`, `cursoNombre?`, `anoEscolarId(uuid)?`

### `EventoAuditoriaResponse`
- `id(uuid)`, `usuarioEmail`, `usuarioRol`, `metodoHttp`, `endpoint`, `requestBody(object|string|null)`, `responseStatus(number)`, `ipAddress`, `anoEscolarId(uuid)?`, `fechaHora(datetime)`

### `BloqueHorarioResponse`
- `id(uuid)`, `numeroBloque(number)`, `horaInicio(HH:mm)`, `horaFin(HH:mm)`, `tipo(enum TipoBloque)`
- `materiaId(uuid)?`, `materiaNombre?`, `materiaIcono?`
- `profesorId(uuid)?`, `profesorNombre?`

### `MateriasDisponiblesResponse`
- `bloqueId(uuid)`, `bloqueDuracionMinutos(number)`, `materias[]`
- `materias[]`: `{ materiaId(uuid), materiaNombre, materiaIcono, horasPedagogicas, minutosSemanalesPermitidos, minutosAsignados, minutosDisponibles, asignable(boolean), asignadaEnEsteBloque(boolean) }`

### `ProfesoresDisponiblesResponse`
- `bloqueId(uuid)`, `bloqueDiaSemana(number)`, `bloqueHoraInicio(HH:mm)`, `bloqueHoraFin(HH:mm)`, `materiaId(uuid)`, `materiaNombre`, `profesores[]`
- `profesores[]`: `{ profesorId(uuid), profesorNombre, profesorApellido, horasPedagogicasContrato?, horasAsignadas, excedido, disponible, asignadoEnEsteBloque, conflicto? }`
- `conflicto?`: `{ cursoNombre, materiaNombre?, horaInicio(HH:mm), horaFin(HH:mm), bloqueId(uuid) }`

### `AuthResponse`
- `token`, `accessToken`, `refreshToken`, `tipo`, `id(uuid)`, `email`, `nombre`, `apellido`, `rol`, `profesorId(uuid)?`, `apoderadoId(uuid)?`

### `GET /api/auth/me` response map
- `{ id(uuid), email, nombre, apellido, rol, profesorId(uuid|null), apoderadoId(uuid|null) }`

### `Dev clock` response map
- `{ currentDateTime: string(datetime), isOverridden: boolean }`

---

## 5) Contratos endpoint por endpoint (todos)

> Formato compacto: éxito + status de error aplicables. Cuando se indica `schemaRef`, usar catálogo de sección 3/4.

### 5.1 Auth

```json
{
  "endpoint": "POST /api/auth/login",
  "responses": {
    "200": { "schemaRef": "AuthResponse" },
    "400": { "schemaRef": "ApiErrorResponse" },
    "401": { "schemaRef": "ApiErrorResponse" },
    "500": { "schemaRef": "ApiErrorResponse" }
  }
}
```

```json
{
  "endpoint": "POST /api/auth/refresh",
  "responses": {
    "200": { "schemaRef": "AuthResponse" },
    "400": { "schemaRef": "ApiErrorResponse" },
    "401": { "schemaRef": "ApiErrorResponse" },
    "500": { "schemaRef": "ApiErrorResponse" }
  }
}
```

```json
{
  "endpoint": "GET /api/auth/me",
  "responses": {
    "200": { "schemaRef": "AuthMeMap" },
    "401": { "schemaRef": "ApiErrorResponse" },
    "500": { "schemaRef": "ApiErrorResponse" }
  }
}
```

### 5.2 Dashboard

```json
{ "endpoint": "GET /api/dashboard/admin", "responses": { "200": { "schemaRef": "DashboardAdminResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "401": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" }, "500": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.3 Profesores

```json
{ "endpoint": "GET /api/profesores", "responses": { "200": { "schemaRef": "ProfesorPageResponse" }, "401": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/profesores/{id}", "responses": { "200": { "schemaRef": "ProfesorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "401": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/profesores", "responses": { "201": { "schemaRef": "ProfesorResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PUT /api/profesores/{id}", "responses": { "200": { "schemaRef": "ProfesorResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/profesores/{profesorId}/sesiones", "responses": { "200": { "schemaRef": "SesionProfesorPageResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/profesores/{profesorId}/cumplimiento-asistencia", "responses": { "200": { "schemaRef": "CumplimientoAsistenciaResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/profesores/{profesorId}/horario", "responses": { "200": { "schemaRef": "ProfesorHorarioResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/profesor/mis-clases-hoy", "responses": { "200": { "schemaRef": "ClasesHoyResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.4 Alumnos

```json
{ "endpoint": "GET /api/alumnos", "responses": { "200": { "schemaRef": "AlumnoPageResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/alumnos/{id}", "responses": { "200": { "schemaRef": "AlumnoResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/alumnos/buscar-por-rut", "responses": { "200": { "schemaRef": "AlumnoResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/alumnos", "responses": { "201": { "schemaRef": "AlumnoResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PUT /api/alumnos/{id}", "responses": { "200": { "schemaRef": "AlumnoResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/alumnos/con-apoderado", "responses": { "201": { "schemaRef": "AlumnoResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.5 Apoderados

```json
{ "endpoint": "POST /api/apoderados", "responses": { "201": { "schemaRef": "ApoderadoResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/apoderados/buscar-por-rut", "responses": { "200": { "schemaRef": "ApoderadoBuscarResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/apoderados/por-alumno/{alumnoId}", "responses": { "200": { "schemaRef": "ApoderadoResponse" }, "204": { "descripcion": "Sin apoderado vinculado", "body": null }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.6 Portal Apoderado

```json
{ "endpoint": "GET /api/apoderado/mis-alumnos", "responses": { "200": { "schemaRef": "AlumnoApoderadoPageResponse" }, "403": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/apoderado/alumnos/{alumnoId}/asistencia/mensual", "responses": { "200": { "schemaRef": "AsistenciaMensualResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/apoderado/alumnos/{alumnoId}/asistencia/resumen", "responses": { "200": { "schemaRef": "ResumenAsistenciaResponse" }, "403": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.7 Cursos

```json
{ "endpoint": "GET /api/cursos", "responses": { "200": { "schemaRef": "CursoPageResponse" }, "400": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/cursos/{id}", "responses": { "200": { "schemaRef": "CursoResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/cursos", "responses": { "201": { "schemaRef": "CursoResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PUT /api/cursos/{id}", "responses": { "200": { "schemaRef": "CursoResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.8 Matrículas

```json
{ "endpoint": "POST /api/matriculas", "responses": { "201": { "schemaRef": "MatriculaResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/matriculas/curso/{cursoId}", "responses": { "200": { "schemaRef": "MatriculaPageResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/matriculas/alumno/{alumnoId}", "responses": { "200": { "schemaRef": "MatriculaPageResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PATCH /api/matriculas/{id}/estado", "responses": { "200": { "schemaRef": "MatriculaResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.9 Asistencia

```json
{ "endpoint": "POST /api/asistencia/clase", "responses": { "201": { "schemaRef": "AsistenciaClaseResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/asistencia/clase", "responses": { "200": { "schemaRef": "AsistenciaClaseResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "403": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.10 Jornada (resto)

```json
{ "endpoint": "PUT /api/cursos/{cursoId}/jornada/{diaSemana}", "responses": { "200": { "schemaRef": "JornadaDiaResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/cursos/{cursoId}/jornada/{diaSemanaOrigen}/copiar", "responses": { "200": { "schemaRef": "JornadaCursoResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "DELETE /api/cursos/{cursoId}/jornada/{diaSemana}", "responses": { "204": { "body": null }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/cursos/{cursoId}/jornada/materias-disponibles?bloqueId=...", "responses": { "200": { "schemaRef": "MateriasDisponiblesResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PATCH /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/materia", "responses": { "200": { "schemaRef": "BloqueHorarioResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "DELETE /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/materia", "responses": { "200": { "schemaRef": "BloqueHorarioResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesores-disponibles", "responses": { "200": { "schemaRef": "ProfesoresDisponiblesResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PATCH /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesor", "responses": { "200": { "schemaRef": "BloqueHorarioResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "DELETE /api/cursos/{cursoId}/jornada/bloques/{bloqueId}/profesor", "responses": { "200": { "schemaRef": "BloqueHorarioResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.11 Malla Curricular

```json
{ "endpoint": "GET /api/malla-curricular", "responses": { "200": { "schemaRef": "MallaCurricularPageResponse" }, "400": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/malla-curricular/materia/{materiaId}", "responses": { "200": { "schemaRef": "MallaCurricularPageResponse" }, "400": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/malla-curricular/grado/{gradoId}", "responses": { "200": { "schemaRef": "MallaCurricularPageResponse" }, "400": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/malla-curricular", "responses": { "201": { "schemaRef": "MallaCurricularResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PUT /api/malla-curricular/{id}", "responses": { "200": { "schemaRef": "MallaCurricularResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/malla-curricular/bulk", "responses": { "200": { "schemaRef": "List<MallaCurricularResponse>" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "DELETE /api/malla-curricular/{id}", "responses": { "204": { "body": null }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.12 Materias

```json
{ "endpoint": "GET /api/materias", "responses": { "200": { "schemaRef": "MateriaPageResponse" } } }
```
```json
{ "endpoint": "GET /api/materias/{id}", "responses": { "200": { "schemaRef": "MateriaResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/materias", "responses": { "201": { "schemaRef": "MateriaResponse" }, "400": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PUT /api/materias/{id}", "responses": { "200": { "schemaRef": "MateriaResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "DELETE /api/materias/{id}", "responses": { "204": { "body": null }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.13 Grados

```json
{ "endpoint": "GET /api/grados", "responses": { "200": { "schemaRef": "GradoPageResponse" } } }
```
```json
{ "endpoint": "GET /api/grados/{id}", "responses": { "200": { "schemaRef": "GradoResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.14 Años escolares

```json
{ "endpoint": "GET /api/anos-escolares", "responses": { "200": { "schemaRef": "AnoEscolarPageResponse" } } }
```
```json
{ "endpoint": "GET /api/anos-escolares/{id}", "responses": { "200": { "schemaRef": "AnoEscolarResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "GET /api/anos-escolares/activo", "responses": { "200": { "schemaRef": "AnoEscolarResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/anos-escolares", "responses": { "201": { "schemaRef": "AnoEscolarResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "PUT /api/anos-escolares/{id}", "responses": { "200": { "schemaRef": "AnoEscolarResponse" }, "400": { "schemaRef": "ApiErrorResponse" }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.15 Días no lectivos

```json
{ "endpoint": "GET /api/dias-no-lectivos", "responses": { "200": { "schemaRef": "DiaNoLectivoPageResponse" }, "400": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "POST /api/dias-no-lectivos", "responses": { "201": { "schemaRef": "List<DiaNoLectivoResponse>" }, "400": { "schemaRef": "ApiErrorResponse" }, "409": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "DELETE /api/dias-no-lectivos/{id}", "responses": { "204": { "body": null }, "404": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.16 Auditoría

```json
{ "endpoint": "GET /api/auditoria", "responses": { "200": { "schemaRef": "EventoAuditoriaPageResponse" }, "403": { "schemaRef": "ApiErrorResponse" } } }
```

### 5.17 Dev tools (solo profile dev)

```json
{ "endpoint": "GET /api/dev/clock", "responses": { "200": { "schemaRef": "DevClockMap" } } }
```
```json
{ "endpoint": "POST /api/dev/clock", "responses": { "200": { "schemaRef": "DevClockMap" }, "400": { "schemaRef": "ApiErrorResponse" } } }
```
```json
{ "endpoint": "DELETE /api/dev/clock", "responses": { "200": { "schemaRef": "DevClockMap" } } }
```

---

## 6) Notas de nullability críticas para frontend

1. `DashboardAdminResponse.ProfesorCumplimiento.porcentajeCumplimiento` puede ser `null`.
2. `DashboardAdminResponse.ProfesorCumplimiento.ultimaActividadHora` puede ser `null`.
3. `DashboardAdminResponse.BloquePendienteDetalle.materiaNombre` puede ser `null`.
4. `CumplimientoAsistenciaResponse.diaNoLectivo` puede ser `null`.
5. En `BloqueCumplimiento`, `materiaId/materiaNombre/materiaIcono` pueden ser `null`.
6. En `BloqueCumplimiento`, `asistenciaClaseId/tomadaEn/resumenAsistencia` pueden ser `null` si no está `TOMADA`.
7. `ProfesorResponse.horasAsignadas` puede ser `null` en algunos flujos.
8. `AlumnoResponse` en endpoints sin contexto de año puede traer campos de matrícula en `null`.
9. `GET /api/apoderados/por-alumno/{alumnoId}` puede devolver `204` sin body.
10. Endpoints `DELETE` que retornan `204` siempre devuelven body vacío.

---

## 7) Recomendación TS (rápida)

- Modelar `ApiErrorResponse` único para todo error backend.
- Marcar como opcionales/nullables exactamente los puntos de la sección 6.
- Para los enum strings, usar union types literales exactos.
- Tratar `Map<Integer,...>` de Java como `Record<string, ...>` en JSON.
