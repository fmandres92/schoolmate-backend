# SchoolMate Hub API

Backend REST API para SchoolMate Hub - Sistema de Gestión Escolar

## Estado

✅ **FASE 0 COMPLETADA** - Backend operativo con autenticación JWT y Supabase PostgreSQL

## Requisitos

- Java 21 LTS
- Maven 3.9+

## Ejecución Rápida

```bash
# Clonar y ejecutar
git clone https://github.com/fmandres92/schoolmate-backend.git
cd schoolmate-backend
mvn spring-boot:run

# La API estará disponible en http://localhost:8080
```

## Endpoints

### Autenticación

| Método | Endpoint | Descripción | Acceso |
|--------|----------|-------------|--------|
| POST | `/api/auth/login` | Login con email y password | Público |
| GET | `/api/auth/me` | Datos del usuario autenticado | Autenticado |

### Ejemplos

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@edugestio.cl","password":"admin123"}'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tipo": "Bearer",
  "id": "admin-1",
  "email": "admin@edugestio.cl",
  "nombre": "Carlos",
  "apellido": "Mendoza",
  "rol": "ADMIN"
}
```

**Obtener datos del usuario:**
```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

## Usuarios de Prueba

| Email | Password | Rol |
|-------|----------|-----|
| admin@edugestio.cl | admin123 | ADMIN |
| profesor@edugestio.cl | prof123 | PROFESOR |
| apoderado@edugestio.cl | apod123 | APODERADO |

## Stack Tecnológico

- **Java 21 LTS**
- **Spring Boot 4.0.2**
- **Spring Security + JWT**
- **PostgreSQL** (Supabase)
- **Flyway** para migraciones
- **Maven** para build

## Documentación

Ver [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md) para documentación técnica completa.

## Compilación

```bash
mvn clean compile    # Compilar
mvn test             # Tests
mvn package          # Crear JAR
```

---

**Repositorio:** https://github.com/fmandres92/schoolmate-backend
