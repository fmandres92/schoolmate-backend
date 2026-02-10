# SchoolMate Hub API

Backend REST API para SchoolMate Hub - Sistema de Gestión Escolar

## Requisitos

- Java 17+
- Maven 3.8+

## Estructura del Proyecto

```
schoolmate-hub-api/
├── src/main/java/com/schoolmate/api/
│   ├── config/         # Configuraciones (Security, CORS)
│   ├── security/       # JWT, UserPrincipal, Filtros
│   ├── entity/         # Entidades JPA
│   ├── enums/          # Enums (Rol, etc.)
│   ├── repository/     # Repositorios Spring Data
│   ├── usecase/        # Casos de uso
│   ├── controller/     # Controladores REST
│   ├── dto/            # DTOs (Request/Response)
│   └── exception/      # Manejo de excepciones
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/   # Migraciones Flyway
└── pom.xml
```

## Configuración

### Desarrollo (H2)

La aplicación usa H2 en memoria por defecto (perfil `dev`).

```bash
./mvnw spring-boot:run
```

### Producción (PostgreSQL)

Configurar variables de entorno:

```bash
export DB_HOST=db.xxx.supabase.co
export DB_PORT=5432
export DB_NAME=postgres
export DB_USERNAME=postgres
export DB_PASSWORD=tu_password
export JWT_SECRET=tu_secret_largo
```

```bash
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

## Endpoints

### Auth

- `POST /api/auth/login` - Login (público)
- `GET /api/auth/me` - Datos del usuario autenticado

### Consola H2 (solo dev)

- `http://localhost:8080/h2-console`

## Usuarios de Prueba

| Email | Password | Rol |
|-------|----------|-----|
| admin@edugestio.cl | admin123 | ADMIN |
| profesor@edugestio.cl | prof123 | PROFESOR |
| apoderado@edugestio.cl | apod123 | APODERADO |

## Compilación

```bash
./mvnw clean compile
./mvnw test
./mvnw package
```
