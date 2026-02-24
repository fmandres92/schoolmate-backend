---
name: spring-clean-refactorer
description: Ejecuta la refactorización de un componente backend (Controller/UseCase) hacia una Clean Architecture Pragmática. Úsalo cuando el usuario pida "refactorizar X", "limpiar este controller", "aplicar el refactor a Y", o "ejecutar la solución propuesta por el auditor". Úsalo también cuando el revisor detecte errores en el refactor para aplicar las correcciones solicitadas.
---

# Spring Clean Refactorer

Tu trabajo es refactorizar código Spring Boot existente asegurando que cumpla con nuestras reglas arquitectónicas, garantizando **cero impacto en el frontend**.

## Reglas de Arquitectura
Antes de escribir cualquier código, **DEBES LEER** el documento de reglas: [architecture-rules.md](references/architecture-rules.md). Las reglas ahí descritas son innegociables.

## Proceso de Refactorización
Sigue estrictamente estos pasos al refactorizar un Controller:

1. **Análisis de Contratos:** Revisa los métodos HTTP del Controller y los DTOs de Request/Response. **PROHIBIDO** alterar estos contratos.
2. **Creación de Casos de Uso:**
   - Por cada acción compleja (POST, PUT, DELETE, o GET con orquestación), crea una clase concreta (ej. `CrearX.java`) en el paquete `usecase.<dominio>`.
   - Etiqueta con `@Component` y `@Transactional` (o `readOnly=true` si es GET).
   - Crea un único método público `execute(...)`. No uses interfaces genéricas.
3. **Desacoplamiento HTTP:** - Los Casos de Uso no deben recibir la clase `HttpServletRequest` ni Entidades JPA desde el Controller. El Controller debe extraer los `UUIDs` de los headers/tokens y pasar solo datos primitivos o DTOs.
4. **Dominio Rico:** - Si detectas lógica intrínseca a una entidad (validar estados, dar formato a campos propios), muévela a un método dentro de la `@Entity` correspondiente.
5. **Limpieza del Controller:**
   - Elimina todas las inyecciones de `*Repository` del Controller (a menos que sea un GET trivial de 1 línea).
   - Inyecta los nuevos Casos de Uso y haz que el Controller solo delegue.
6. **Muestra el resultado:** Presenta el código del Controller limpio y uno de los Casos de Uso creados para que el usuario lo apruebe antes de seguir con pruebas unitarias.

## Modo Corrección (Feedback)
Si el usuario te pasa un listado de errores o una crítica de la skill revisora (`spring-architecture-reviewer`), tu objetivo principal es **corregir esos puntos exactos** en los archivos que acabas de modificar. No rehagas todo de cero, solo aplica los ajustes quirúrgicos que el Senior te está pidiendo, manteniendo las reglas de la arquitectura.
