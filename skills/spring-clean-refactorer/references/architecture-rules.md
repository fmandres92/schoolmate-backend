# SchoolMate Hub — Plan de Refactorización y Arquitectura Backend

> **Propósito:** Este documento define la arquitectura pragmática, las reglas de diseño y el plan de acción para refactorizar el backend de SchoolMate Hub. Pasaremos de un "MVP funcional pero desordenado" a un sistema sólido, escalable y 100% testeable.
> **Audiencia:** Desarrolladores humanos y Agentes de IA. Las reglas aquí descritas son **INNEGOCIABLES**.
> **Stack:** Java 21, Spring Boot 4.0.x, JPA, PostgreSQL.

---

## 1. EL PROBLEMA (Lo que dejamos atrás) 🚫
Actualmente el proyecto sufre del "Síndrome del MVP":
* **Controladores Obesos (Fat Controllers):** Los `@RestController` están haciendo validaciones de negocio, cálculos de estado y orquestación de repositorios.
* **Modelos Anémicos (Anemic Domain):** Las entidades JPA (`@Entity`) son solo contenedores de datos (getters y setters). Toda la lógica vive fuera de ellas.
* **In-testeabilidad:** Al estar la lógica acoplada al contexto HTTP (Controllers), es imposible escribir pruebas unitarias rápidas y confiables.

## 2. LA SOLUCIÓN: ARQUITECTURA PRAGMÁTICA 🟢
No usaremos Arquitectura Hexagonal estricta (sin puertos ni adaptadores innecesarios). Usaremos un enfoque de 3 capas limpias con **Modelo de Dominio Rico**:

### Capa 1: Controllers "Tontos" (Delivery)
* **Responsabilidad:** Parsear HTTP, validar DTOs (con `@Valid`), delegar al Use Case y devolver respuestas HTTP (200, 201, 400).
* **Prohibido:** No pueden tener `if` de negocio, no pueden llamar a múltiples repositorios, no pueden calcular fechas ni estados.
* **Excepción:** Solo se permite que el Controller llame directo al `Repository` para operaciones `GET` (lecturas puras) que no tengan lógica de negocio.

### Capa 2: Casos de Uso (Orquestación)
* **Responsabilidad:** Orquestar el flujo. Recuperar entidades de la base de datos, decirle a las entidades que ejecuten acciones, y guardar los cambios. Transaccionalidad (`@Transactional`).
* **Estructura:** 1 Acción = 1 Clase (Ej: `MatricularAlumno.java`). Un solo método público `execute()`.
* **Prohibido:** No "servicios gigantes" (`AlumnoService` con 20 métodos).

### Capa 3: Modelo de Dominio Rico (Entidades Inteligentes)
* **Responsabilidad:** Las entidades de negocio protegen su propia integridad. Las validaciones de estado y cálculos pertenecen a la Entidad, no al Use Case.
* **Ejemplo Correcto:** `matricula.trasladar()` (la entidad cambia su estado y valida que no estuviera ya trasladada).
* **Ejemplo Incorrecto:** `if (matricula.getEstado() != TRASLADADO) { matricula.setEstado(TRASLADADO); }` dentro del Use Case.

---

## 3. REGLAS PARA AGENTES DE IA 🤖
Si eres un agente de IA modificando este código, **DEBES CUMPLIR** lo siguiente:
1.  **Cero Sobreingeniería:** No crees interfaces para los Use Cases ni mappers genéricos. Clases concretas son suficientes.
2.  **Mutaciones aisladas:** Todo POST, PUT, PATCH, DELETE **debe** tener su propio archivo Use Case.
3.  **Lógica a la Entidad:** Si te encuentras escribiendo un `if` de negocio en un Use Case que solo evalúa datos de una entidad, muévelo a un método dentro de la entidad JPA.
4.  **DTOs de entrada/salida:** Los Controllers y UseCases consumen `Requests` y devuelven `Responses` o entidades. No devolver entidades con lazy loading directo al cliente si causan N+1.
5.  **Excepciones:** Usar las excepciones centralizadas (`BusinessException`, `ResourceNotFoundException`, `ConflictException`) que serán capturadas por el `GlobalExceptionHandler`.

---

## 4. CÓMO HACER UN REFACTOR (Paso a Paso)

Cuando tomemos un flujo existente para refactorizarlo, seguiremos este algoritmo estricto:

### Paso 1: Análisis y Extracción
Tomar el endpoint del Controller (ej. `POST /alumnos`). Mover toda la lógica de validación cruzada y guardado a una clase UseCase (ej. `CrearAlumno.java`).

### Paso 2: Enriquecer el Dominio
Revisar el UseCase. Si hay lógicas intrínsecas (ej. formato de RUT, validación de edades, asignación de estados iniciales), moverlas a constructores o métodos dentro de la entidad `Alumno`.

### Paso 3: Pruebas Unitarias (Tests)
Crear el archivo de test para el UseCase (`CrearAlumnoTest.java`) usando **JUnit 5 y Mockito**.
* Probar el caso de éxito (Happy Path).
* Probar los casos de error (excepciones de negocio).
* *Nota: Estos tests no deben levantar Spring Boot (`@SpringBootTest` prohibido aquí). Deben ejecutar en milisegundos.*

### Paso 4: Limpieza del Controller
Dejar el Controller recibiendo el DTO, llamando a `crearAlumno.execute(request)` y devolviendo `ResponseEntity.ok()`.

---

## 5. HOJA DE RUTA (Roadmap de Refactorización) 🗺️

Avanzaremos módulo por módulo para no romper el MVP funcional.

* [ ] **Fase 1: Dominio de Alumnos y Apoderados (La Base)**
    * *Objetivo:* Limpiar la creación compleja (`CrearAlumnoConApoderado`) y las actualizaciones.
    * *Acción:* Extraer lógica a Casos de Uso, crear métodos de dominio, implementar tests unitarios exhaustivos para la lógica de RUT y cruces de datos.
* [ ] **Fase 2: Dominio de Cursos y Malla (Lógica de Generación)**
    * *Objetivo:* Limpiar la generación automática de letras de cursos y validaciones de malla curricular.
    * *Acción:* Mover la lógica de `CursoController` a `CrearCurso` y `ActualizarCurso`.
* [ ] **Fase 3: Dominio de Asistencia (Lógica Temporal Crítica)**
    * *Objetivo:* Refactorizar `GuardarAsistenciaClase`.
    * *Acción:* Mover la lógica de cálculo de "Ventana de tiempo" y "Cierre estricto" a objetos de dominio (ej. `PoliticaAsistencia`). Hacerlo 100% testeable con mocks de tiempo (`Clock`).
* [ ] **Fase 4: Dominio de Jornada Escolar (Colisiones)**
    * *Objetivo:* Limpiar validaciones de solapamiento de horarios y asignación de profesores.
    * *Acción:* Refactorizar `jornadaService` y Use Cases, moviendo la validación de colisiones al dominio.

---
**Última Actualización:** Febrero 2026
**Estado:** Iniciando Fase 1.
