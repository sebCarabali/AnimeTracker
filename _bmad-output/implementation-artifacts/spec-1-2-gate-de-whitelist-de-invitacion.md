---
title: 'Story 1.2 — Gate de Whitelist de Invitación'
type: 'feature'
created: '2026-08-18'
status: 'done'
review_loop_iteration: 0
baseline_commit: '3f9b6b95311c64cdc044cce158b73c6f15be4f70'
context: [_bmad-output/implementation-artifacts/epic-1-context.md, _bmad-output/planning-artifacts/ux-designs/ux-AnimeList-2026-08-13/mockups/login.html]
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Tras el login OAuth (Story 1.1), cualquier usuario autenticado técnicamente cae en `/` sin control de invitación: el punto de extensión en `OAuthLoginSuccessHandler` está marcado pero vacío, y el proyecto no tiene persistencia todavía.

**Approach:** Insertar el whitelist gate en `OAuthLoginSuccessHandler` (AD-5): resolver `WhitelistedUser` por id de AniList vía JPA; si falta, redirigir a `/acceso-denegado` sin tocar `AppUser`/sesión; si está, `auth` ejecuta `findOrCreate(AppUser)` transaccional y abre sesión. Bootstrapea Spring Data JPA + Flyway + PostgreSQL (Testcontainers en tests), con `AppUser` en `domain/` y `WhitelistedUser` + su repo en `auth/`.

## Boundaries & Constraints

**Always:**
- El gate se evalúa antes de crear sesión o fila `AppUser` (AD-5); `auth` es el único owner de `findOrCreate(AppUser)`.
- `WhitelistedUser` + repositorio: package-private en `auth/`. `AppUser` + repositorio: `domain/` (a diferencia de `TrackingEntry`/`Snapshot`, confinados a `sync/` por AD-2).
- Cada entidad mantiene su propia PK + columna indexada separada para el id de AniList; timestamps `Instant` UTC.
- `findOrCreate(AppUser)` corre en una transacción; la migración agrega un constraint único en la columna de AniList id como red de seguridad contra la carrera de doble creación en primer login concurrente (AD-5).
- Esquema versionado vía Flyway (`V1__...`); nunca `ddl-auto` para gestionar el esquema.
- Credenciales de PostgreSQL vía `application.yml` + variables de entorno.
- Acceso Denegado replica el mockup: headline exacto "Tu cuenta de AniList no está habilitada todavía." (nunca "Acceso denegado" a secas), página dedicada distinta de Login, sin redirección automática de reintento (UX-DR12).

**Ask First:**
- Docker Desktop está instalado pero el daemon no corre en este entorno ahora mismo — necesario para Testcontainers. Si sigue sin estar disponible al implementar, preguntar antes de sustituir la estrategia de test de la capa JPA.
- Si `/acceso-denegado` debe sumarse al `permitAll` de `SecurityConfig` — el usuario ya está autenticado por OAuth2 en ese punto, solo no está en whitelist; confirmar antes de decidir.

**Never:**
- No implementar UI de administración de Whitelist (deferred, OQ-1); alta sigue siendo manual por DB directa.
- No implementar sync (Epic 2), theme toggle (Epic 3), ni entorno de desarrollo local de Postgres (deferred, ver deferred-work.md).
- No usar `ddl-auto` de Hibernate para crear/actualizar el esquema.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Whitelisted, primer login | AniList id en `WhitelistedUser`, sin `AppUser` previo | `findOrCreate` crea el `AppUser`, sesión válida, redirect a `/` | N/A |
| Whitelisted, login recurrente | `AppUser` ya existe | `findOrCreate` recupera el existente (no duplica), sesión válida | N/A |
| No whitelisted | AniList id ausente de `WhitelistedUser` | Redirect a `/acceso-denegado`, sin sesión ni fila `AppUser` | N/A |
| Agregado tras intento fallido | Operador agrega la fila después de un rechazo previo | Siguiente intento completa el acceso normalmente | N/A |

</frozen-after-approval>

## Code Map

- `src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java:26-36` -- insertar el gate (TODO ya marcado), reemplaza el `sendRedirect` incondicional
- `src/main/java/com/animetracker/config/SecurityConfig.java:32-35` -- ajustar `permitAll` según Ask First
- `build.gradle.kts` -- agregar `spring-boot-starter-data-jpa`, `flyway-core`, `org.postgresql:postgresql`, `spring-boot-testcontainers` + `org.testcontainers:junit-jupiter`/`postgresql` (test)
- `src/main/resources/application.yml` -- `spring.datasource.*`, `spring.jpa.hibernate.ddl-auto: validate`, `spring.flyway.*` vía env vars
- `src/main/resources/db/migration/V1__create_whitelisted_user_and_app_user.sql` -- nuevo, ambas tablas + constraint único en columna de AniList id
- `src/main/java/com/animetracker/domain/AppUser.java` + `AppUserRepository.java` -- nuevo `@Entity` (PK propia, `anilist_user_id` indexado, `theme_preference`) + `findByAnilistUserId`
- `src/main/java/com/animetracker/auth/WhitelistedUser.java` + `WhitelistedUserRepository.java` -- nuevo `@Entity` package-private + `existsByAnilistUserId`
- `src/main/java/com/animetracker/auth/AccessDeniedController.java` + `templates/auth/acceso-denegado.html` -- nuevo, `GET /acceso-denegado`, CSS embebido como `login.html`, copy/layout del mockup (`.tag`, `.headline`, `.btn-secondary`)
- `src/test/java/com/animetracker/auth/OAuthLoginFlowTest.java:87-98` -- reescribir el test de handoff, roto por el nuevo constructor del handler
- `src/test/java/com/animetracker/auth/WhitelistGateTest.java` -- nuevo, cubre la I/O Matrix con Testcontainers Postgres + `oauth2Login()`

## Tasks & Acceptance

**Execution:**
- [x] `build.gradle.kts` + `application.yml` -- dependencias y config de JPA/Flyway/Postgres/Testcontainers -- fundamento de persistencia, no existe ninguna todavía
- [x] `db/migration/V1__create_whitelisted_user_and_app_user.sql` -- tablas + constraint único -- red de seguridad de la carrera de doble creación (AD-5)
- [x] `domain/AppUser.java` + `AppUserRepository.java` -- entidad compartida y repositorio
- [x] `auth/WhitelistedUser.java` + `WhitelistedUserRepository.java` (package-private) -- solo `auth` consulta la whitelist (AD-5)
- [x] `auth/OAuthLoginSuccessHandler.java` -- implementar el gate: whitelist -> si falta, `/acceso-denegado`; si está, `findOrCreate(AppUser)` + sesión
- [x] `auth/AccessDeniedController.java` + `templates/auth/acceso-denegado.html` -- página dedicada fiel al mockup -- UX-DR12
- [x] `config/SecurityConfig.java` -- ajustar rutas públicas según Ask First
- [x] `test/auth/WhitelistGateTest.java` -- cubrir las 4 filas de la I/O Matrix -- sin esto AD-5 no es verificable end-to-end
- [x] `test/auth/OAuthLoginFlowTest.java` -- reescribir el test roto por el nuevo constructor

**Acceptance Criteria:**
- Given un usuario completa el OAuth exitosamente, when el callback se procesa, then se consulta `WhitelistedUser` por su id de AniList antes de crear sesión o fila `AppUser` (AD-5)
- Given el id de AniList está en la Whitelist, when se evalúa el gate, then `auth` invoca `findOrCreate(AppUser)` y se abre una sesión válida
- Given un usuario fue agregado a la Whitelist después de un intento fallido, when vuelve a intentar el login, then completa el acceso sin re-registro
- Given la pantalla de Acceso Denegado, when se muestra, then es una página dedicada, distinta de Login, sin botón de reintento automático (UX-DR12)

## Design Notes

- **`findOrCreate` a prueba de carrera:** buscar por `anilist_user_id`; si no existe, insertar con `theme_preference` por defecto, todo en una transacción. Ante `DataIntegrityViolationException` por el constraint único, releer la fila insertada por el request concurrente en vez de propagar el error.
- **Orden exacto ya documentado** en el Javadoc actual de `OAuthLoginSuccessHandler` (Story 1.1): whitelist -> denegado/`findOrCreate` -> sesión. Preservarlo literalmente.

## Verification

**Commands:**
- `./gradlew build` -- expected: `BUILD SUCCESSFUL`, incluyendo `WhitelistGateTest` en verde
- `./gradlew test --tests "*WhitelistGateTest"` -- expected: las 4 filas de la I/O Matrix pasan contra Postgres real (Testcontainers)

**Manual checks (if no CLI):**
- Insertar una fila en `whitelisted_user` con un id de AniList real, completar el login, verificar que se crea `app_user` y la sesión queda abierta
- Completar el login con una cuenta sin fila en `whitelisted_user`, verificar que aterriza en `/acceso-denegado` con el copy exacto del mockup y sin sesión creada

## Suggested Review Order

**Gate de whitelist y manejo de fallas (el corazón del cambio)**

- Punto de entrada: orden completo whitelist -> denegado/provisioning -> sesión, con las 3 salidas de error
  [`OAuthLoginSuccessHandler.java:52`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L52)

- Id de AniList no numérico en el principal se trata como falla de login, no como 500
  [`OAuthLoginSuccessHandler.java:54`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L54)

- Rechazo de whitelist invalida sesión/SecurityContext explícitamente antes de redirigir
  [`OAuthLoginSuccessHandler.java:64`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L64)

- Fallas de DB en el whitelist-check o en `findOrCreate` también redirigen a `/login?error` en vez de propagar
  [`OAuthLoginSuccessHandler.java:75`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L75)

**Provisioning transaccional a prueba de carrera**

- `findOrCreate`: pre-check por `anilist_user_id` con fallback a inserción aislada
  [`AppUserProvisioningService.java:44`](../../src/main/java/com/animetracker/auth/AppUserProvisioningService.java#L44)

- Inserción en transacción `REQUIRES_NEW` propia + relectura tras perder la carrera del constraint único
  [`AppUserProvisioningService.java:49`](../../src/main/java/com/animetracker/auth/AppUserProvisioningService.java#L49)

**Esquema y entidades**

- Dos tablas con PK propia cada una; `UNIQUE` en `anilist_user_id` de `app_user` es la red de seguridad de la carrera
  [`V1__create_whitelisted_user_and_app_user.sql:15`](../../src/main/resources/db/migration/V1__create_whitelisted_user_and_app_user.sql#L15)

- `WhitelistedUser`: entidad package-private, solo `auth` la consulta (AD-5)
  [`WhitelistedUser.java:20`](../../src/main/java/com/animetracker/auth/WhitelistedUser.java#L20)

- `AppUser`: entidad compartida en `domain/`, a diferencia de `TrackingEntry`/`Snapshot` confinados a `sync/`
  [`AppUser.java:25`](../../src/main/java/com/animetracker/domain/AppUser.java#L25)

**Configuración de seguridad y ruteo**

- `/acceso-denegado` se suma al `permitAll`: la sesión ya fue invalidada antes de llegar acá, por eso puede ser pública
  [`SecurityConfig.java:42`](../../src/main/java/com/animetracker/config/SecurityConfig.java#L42)

- Controller dedicado de Acceso Denegado (UX-DR12), sin lógica más allá del mapping
  [`AccessDeniedController.java:15`](../../src/main/java/com/animetracker/auth/AccessDeniedController.java#L15)

**Persistencia y config (peripherals)**

- Datasource requiere `DB_USERNAME`/`DB_PASSWORD` explícitos (falla rápido si faltan, sin fallback silencioso)
  [`application.yml:6`](../../src/main/resources/application.yml#L6)

- `ddl-auto: validate` -- Flyway, no Hibernate, es la única fuente de verdad del esquema
  [`application.yml:14`](../../src/main/resources/application.yml#L14)

- Dependencias JPA/Flyway/Testcontainers agregadas, con nota sobre el módulo de Flyway separado en Spring Boot 4
  [`build.gradle.kts:28`](../../build.gradle.kts#L28)

**Tests**

- `WhitelistGateTest`: cubre las 4 filas de la I/O Matrix contra Postgres real vía Testcontainers
  [`WhitelistGateTest.java:71`](../../src/test/java/com/animetracker/auth/WhitelistGateTest.java#L71)

- Nueva cobertura de la invalidación real de sesión (rama `session.invalidate()` del logout handler)
  [`WhitelistGateTest.java:123`](../../src/test/java/com/animetracker/auth/WhitelistGateTest.java#L123)

- Nueva cobertura de que `/acceso-denegado` es alcanzable a través del filtro real de Spring Security
  [`OAuthLoginFlowTest.java:75`](../../src/test/java/com/animetracker/auth/OAuthLoginFlowTest.java#L75)

- Test de handoff reescrito para el nuevo constructor del handler (Story 1.1 -> 1.2)
  [`OAuthLoginFlowTest.java:124`](../../src/test/java/com/animetracker/auth/OAuthLoginFlowTest.java#L124)
