# AGENTS.md instructions for /Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api

## Skills
A skill is a set of local instructions to follow that is stored in a `SKILL.md` file. Below is the list of skills that can be used. Each entry includes a name, description, and file path so you can open the source for full instructions when using a specific skill.

### Available skills
- spring-architecture-auditor: Audita un backend Spring Boot para detectar antipatrones de arquitectura (fat controllers, entidades anemicas, fugas de frontera) y proponer top 3 prioridades de refactorizacion. (file: /Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/skills/spring-architecture-auditor/SKILL.md)
- spring-architecture-reviewer: Revisa un refactor no commiteado como Tech Lead Senior, validando reglas de arquitectura y emitiendo veredicto aprobado/rechazado con correcciones accionables. (file: /Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/skills/spring-architecture-reviewer/SKILL.md)
- spring-clean-refactorer: Ejecuta refactorizaciones de controllers/use cases Spring Boot aplicando clean architecture pragmatica y sin romper contratos de API/frontend. (file: /Users/aflores/Documents/proyecto/colegios/backend-hub/schoolmate-hub-api/skills/spring-clean-refactorer/SKILL.md)
- backend-conventions: Guardrails y convenciones del proyecto backend schoolmate-hub-api durante desarrollo. Se activa automáticamente cuando el usuario pide implementar, crear, modificar o migrar cualquier funcionalidad del backend. Define qué hacer, qué no hacer, archivos protegidos, patrones obligatorios y estructura del proyecto para evitar que el agente invente, rompa o ignore convenciones existentes. (file: /Users/aflores/.codex/skills/backend-conventions/SKILL.md)
- db-schema-sync: Extraer y actualizar la estructura de bases PostgreSQL/Supabase en archivos locales del proyecto. Usar cuando el usuario pida actualizar informacion de la BD, listar tablas/columnas/indices/relaciones, o generar `db/schema.sql` y `db/schema_inventory.md` para documentacion y analisis con agentes. (file: /Users/aflores/.codex/skills/db-schema-sync/SKILL.md)
- doc-sync-backend: Sincronizar documentación técnica backend de forma incremental en BACKEND_DOCUMENTATION.md después de cambios de código. Usar cuando el usuario pida actualizar/sincronizar/documentar cambios recientes del backend (entidades, controllers, DTOs, use cases, repositorios, migraciones SQL, seguridad o configuración) sin reescribir todo el documento. (file: /Users/aflores/.codex/skills/doc-sync-backend/SKILL.md)
- find-skills: Helps users discover and install agent skills when they ask questions like "how do I do X", "find a skill for X", "is there a skill that can...", or express interest in extending capabilities. This skill should be used when the user is looking for functionality that might exist as an installable skill. (file: /Users/aflores/.agents/skills/find-skills/SKILL.md)
- skill-creator: Guide for creating effective skills. This skill should be used when users want to create a new skill (or update an existing skill) that extends Claude's capabilities with specialized knowledge, workflows, or tool integrations. (file: /Users/aflores/.agents/skills/skill-creator/SKILL.md)
- skill-creator: Guide for creating effective skills. This skill should be used when users want to create a new skill (or update an existing skill) that extends Codex's capabilities with specialized knowledge, workflows, or tool integrations. (file: /Users/aflores/.codex/skills/.system/skill-creator/SKILL.md)
- skill-installer: Install Codex skills into $CODEX_HOME/skills from a curated list or a GitHub repo path. Use when a user asks to list installable skills, install a curated skill, or install a skill from another repo (including private repos). (file: /Users/aflores/.codex/skills/.system/skill-installer/SKILL.md)

### How to use skills
- Discovery: The list above is the skills available in this session (name + description + file path). Skill bodies live on disk at the listed paths.
- Trigger rules: If the user names a skill (with `$SkillName` or plain text) OR the task clearly matches a skill's description shown above, you must use that skill for that turn. Multiple mentions mean use them all. Do not carry skills across turns unless re-mentioned.
- Missing/blocked: If a named skill isn't in the list or the path can't be read, say so briefly and continue with the best fallback.
- How to use a skill (progressive disclosure):
  1) After deciding to use a skill, open its `SKILL.md`. Read only enough to follow the workflow.
  2) When `SKILL.md` references relative paths (e.g., `scripts/foo.py`), resolve them relative to the skill directory listed above first, and only consider other paths if needed.
  3) If `SKILL.md` points to extra folders such as `references/`, load only the specific files needed for the request; don't bulk-load everything.
  4) If `scripts/` exist, prefer running or patching them instead of retyping large code blocks.
  5) If `assets/` or templates exist, reuse them instead of recreating from scratch.
- Coordination and sequencing:
  - If multiple skills apply, choose the minimal set that covers the request and state the order you'll use them.
  - Announce which skill(s) you're using and why (one short line). If you skip an obvious skill, say why.
- Context hygiene:
  - Keep context small: summarize long sections instead of pasting them; only load extra files when needed.
  - Avoid deep reference-chasing: prefer opening only files directly linked from `SKILL.md` unless you're blocked.
  - When variants exist (frameworks, providers, domains), pick only the relevant reference file(s) and note that choice.
- Safety and fallback: If a skill can't be applied cleanly (missing files, unclear instructions), state the issue, pick the next-best approach, and continue.
