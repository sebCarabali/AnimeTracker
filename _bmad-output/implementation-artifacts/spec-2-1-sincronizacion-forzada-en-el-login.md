---
title: 'Story 2.1 — Sincronización forzada en el login'
type: 'feature'
created: '2026-08-20'
status: 'done'
review_loop_iteration: 0
baseline_commit: '912091174f15308339ad1be2ce95a6647a75a3fa'
context: [_bmad-output/implementation-artifacts/epic-2-context.md]
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Tras el login (Story 1.2), el usuario cae en `/` con un `AppUser` recién creado/recuperado pero sin ningún dato de AniList sincronizado -- no existe todavía el package `sync` ni una fuente local de datos, así que cualquier vista futura tendría que consultar AniList en vivo (violaría AD-9/NFR-1).

**Approach:** Crear el package `sync` (`SyncService`, repositorios JPA package-private, `SyncedDataQueryService`) y `integration.anilist.AniListMediaListClient` (nueva query GraphQL `MediaListCollection`, `type: ANIME`). Invocar `SyncService.syncUser` de forma síncrona en `OAuthLoginSuccessHandler` justo después de `findOrCreate(AppUser)`, usando el access token vía `OAuth2AuthorizedClientService`; reconciliar `TrackingEntry` (upsert + baja lógica) y persistir un `Snapshot` por entrada de esa corrida.

## Boundaries & Constraints

**Always:**
- El acceso a AniList para el listado pasa exclusivamente por `integration.anilist.AniListMediaListClient` (AD-1, AD-3) -- solo lectura, ninguna mutation GraphQL.
- `TrackingEntry`/`Snapshot` viven en `domain` (entidades compartidas); sus repositorios JPA son package-private dentro de `sync` (AD-2). Toda lectura futura de otras features pasa exclusivamente por `SyncedDataQueryService`.
- `TrackingStatus` (domain, nuevo enum) mapea 1:1 a los 5 estados estándar del producto (CURRENT/PLANNING/COMPLETED/DROPPED/REPEATING). Cada `TrackingEntry` mantiene su propia PK + `anilist_media_id` indexado separado.
- Reconciliación completa por corrida: upsert de las entradas presentes + baja lógica (`active=false`) de las que ya no aparecen.
- Un `Snapshot` por (`TrackingEntry`, corrida), con `taken_at` `Instant` UTC compartido entre todas las entradas de la misma corrida.
- La llamada de login-sync es síncrona/bloqueante, antes de redirigir a `/` (AD-7).

**Ask First:**
- Manejo de entradas de AniList con `status=PAUSED` (no mapea a ningún `TrackingStatus`): default propuesto es no upsertearla en esta corrida (queda ausente hasta que el usuario la mueva a un estado soportado) -- confirmar antes de implementar.
- Obtención del access token post-login: usar `OAuth2AuthorizedClientService.loadAuthorizedClient("anilist", principalName)` (no requiere `HttpServletRequest`) -- confirmar que alcanza con el bean autoconfigurado por Spring Boot antes de introducir el `OAuth2AuthorizedClientRepository` basado en sesión.

**Never:**
- No implementar el job periódico (Story 2.2) ni el limitador de concurrencia de `integration.anilist` (AD-6 -- corresponde a 2.2).
- No implementar el Stale Banner ni el flujo de degradación explícito (Story 2.3) -- una falla de sync durante el login se loggea y el login continúa igual, sin bloquear al usuario.
- No exponer `TrackingEntryRepository`/`SnapshotRepository` fuera de `sync`.
- No persistir título/póster ni ningún otro campo de `Media` fuera de `anilist_media_id` -- eso es scope de las vistas de lectura (Epic 3+).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Primer sync de un usuario nuevo | `AppUser` sin `TrackingEntry` previos; AniList devuelve 3 entradas | Se crean 3 `TrackingEntry` activos + 3 `Snapshot` de la misma corrida | N/A |
| Sync recurrente, anime removido de la lista | `TrackingEntry` activo previo cuyo media ya no aparece en la respuesta | Ese `TrackingEntry` pasa a `active=false`; no se crea `Snapshot` para él en esta corrida | N/A |
| Sync recurrente, progreso actualizado | `TrackingEntry` activo previo, AniList devuelve mismo media con `progress` mayor | Se actualiza `last_episode` in place (misma fila) + nuevo `Snapshot` con el progreso nuevo | N/A |
| Entrada con status no mapeado (`PAUSED`) | AniList devuelve una entrada `status=PAUSED` | Se omite del upsert de esta corrida (ver Ask First) | N/A |
| Falla de red/timeout hacia AniList durante login-sync | `AniListMediaListClient` lanza excepción | No se crea/actualiza ningún `TrackingEntry`/`Snapshot` de esta corrida; el login continúa y redirige a `/` igual | Excepción capturada en el success handler, nunca 500 |

</frozen-after-approval>

## Code Map

- `src/main/java/com/animetracker/domain/TrackingStatus.java` -- nuevo enum, 5 valores 1:1 con `MediaListStatus` de AniList
- `src/main/java/com/animetracker/domain/TrackingEntry.java` -- nueva `@Entity` (PK propia, `appUser` FK, `anilistMediaId` indexado, `status`, `lastEpisode`, `active`)
- `src/main/java/com/animetracker/domain/Snapshot.java` -- nueva `@Entity` (PK propia, `trackingEntry` FK, `episodeProgress`, `takenAt` `Instant`)
- `src/main/java/com/animetracker/sync/TrackingEntryRepository.java` -- nuevo, package-private, `JpaRepository<TrackingEntry, Long>` + `findByAppUserId`
- `src/main/java/com/animetracker/sync/SnapshotRepository.java` -- nuevo, package-private, `JpaRepository<Snapshot, Long>`
- `src/main/java/com/animetracker/sync/SyncService.java` -- nuevo, público, `syncUser(AppUser, String accessToken)` transaccional: fetch + reconciliación + snapshots (AD-7, único codepath que reutilizará el job de 2.2)
- `src/main/java/com/animetracker/sync/SyncedDataQueryService.java` -- nueva interfaz pública, `findActiveEntries(Long appUserId)`
- `src/main/java/com/animetracker/sync/SyncedDataQueryServiceImpl.java` -- nuevo, package-private, implementa la interfaz vía `TrackingEntryRepository`
- `src/main/java/com/animetracker/integration/anilist/AniListMediaListClient.java` -- nuevo, mismo patrón que `AniListViewerClient.java` (RestClient + timeouts), query `MediaListCollection(userId, type: ANIME)`
- `src/main/java/com/animetracker/integration/anilist/AniListMediaListEntry.java` -- nuevo record `(Long mediaId, TrackingStatus status, int progress)`
- `src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java:35-82` -- inyectar `SyncService` + `OAuth2AuthorizedClientService`; invocar `syncUser` tras `findOrCreate`, envuelto en try/catch que solo loggea
- `src/main/resources/db/migration/V2__create_tracking_entry_and_snapshot.sql` -- nuevo, ambas tablas + FKs + unique `(app_user_id, anilist_media_id)`
- `src/test/java/com/animetracker/sync/SyncServiceTest.java` -- nuevo, Testcontainers Postgres + `AniListMediaListClient` mockeado (Mockito), cubre la I/O Matrix
- `src/test/java/com/animetracker/auth/WhitelistGateTest.java` -- ajustar si el constructor del handler cambia (nuevas dependencias)

## Tasks & Acceptance

**Execution:**
- [x] `db/migration/V2__create_tracking_entry_and_snapshot.sql` -- tablas `tracking_entry` (FK a `app_user`, unique por `app_user_id`+`anilist_media_id`) y `snapshot` (FK a `tracking_entry`) -- base de reconciliación y de Snapshots
- [x] `domain/TrackingStatus.java` -- enum de 5 estados -- fuente única de verdad del estado de dominio
- [x] `domain/TrackingEntry.java` + `domain/Snapshot.java` -- entidades compartidas
- [x] `sync/TrackingEntryRepository.java` + `sync/SnapshotRepository.java` (package-private) -- único punto de escritura sobre estas tablas (AD-2)
- [x] `integration/anilist/AniListMediaListEntry.java` + `AniListMediaListClient.java` -- query `MediaListCollection` mapeada a DTOs propios, sin exponer el shape crudo de AniList (AD-1)
- [x] `sync/SyncService.java` -- `syncUser`: fetch, upsert + baja lógica, Snapshot por entrada, todo en una transacción
- [x] `sync/SyncedDataQueryService.java` + `SyncedDataQueryServiceImpl.java` -- interfaz de lectura publicada para el resto de features (AD-9)
- [x] `auth/OAuthLoginSuccessHandler.java` -- disparar `syncUser` de forma síncrona antes del redirect final, tolerando fallas sin romper el login
- [x] `test/sync/SyncServiceTest.java` -- cubrir las 5 filas de la I/O Matrix contra Postgres real
- [x] `test/auth/WhitelistGateTest.java` -- ajustar construcción del handler si el constructor cambió (no requirió cambios: el handler se obtiene vía `@Autowired`, no instanciación manual -- ver Spec Change Log)

**Acceptance Criteria:**
- Given una sesión recién creada tras el gate de whitelist, when se abre la sesión, then AnimeTracker ejecuta de forma síncrona/bloqueante una consulta a AniList para ese usuario antes del primer render post-login (AD-7)
- Given el flujo de sync, when se implementa el acceso a AniList, then pasa exclusivamente por `integration.anilist`, sin ninguna mutation GraphQL (AD-1, AD-3)
- Given los repositorios JPA de `TrackingEntry`/`Snapshot`, when se implementan, then viven en el package `sync` con visibilidad package-private (AD-2)
- Given cualquier feature de lectura futura, when necesite datos sincronizados, then solo puede obtenerlos vía `SyncedDataQueryService`, nunca consultando AniList en vivo (AD-9)

## Spec Change Log

- **Reconciliación: se cargan TODAS las `TrackingEntry` del usuario (activas e inactivas), no solo las activas.** El Design Note original decía "cargar los TrackingEntry activos... en un Map". Implementado así, un anime que había sido dado de baja lógica en una corrida anterior y reaparece en la respuesta de AniList no se encontraría en el map (por estar filtrado a solo activos) y `SyncService` intentaría un `INSERT` de una fila nueva con el mismo `(app_user_id, anilist_media_id)` -- violando el `UNIQUE` de `V2__...sql` y rompiendo la corrida completa (la transacción entera hace rollback) en vez de reactivar la fila existente. Se implementó cargando todas las filas del usuario (`TrackingEntryRepository.findByAppUserId`, sin filtrar por `active`) para que la reconciliación pueda reactivar in-place. El comportamiento externo (las 5 filas de la I/O Matrix) es idéntico; el único caso que cambia es la reactivación de un anime previamente dado de baja, que no estaba explícitamente en la I/O Matrix pero cae directamente bajo "Reconciliación completa por corrida" (Boundaries & Constraints, frozen).
- **Access token post-login:** confirmado el uso de `OAuth2AuthorizedClientService.loadAuthorizedClient("anilist", principalName)` contra el bean autoconfigurado por Spring Boot (`InMemoryOAuth2AuthorizedClientService`, envuelto en `AuthenticatedPrincipalOAuth2AuthorizedClientRepository` por defecto al no haber un `OAuth2AuthorizedClientRepository` propio en `SecurityConfig`) -- no se introdujo ningún bean ni configuración adicional.
- **Entradas con `status=PAUSED` (u otro no mapeado):** confirmado el default propuesto -- se descartan en `AniListMediaListClient` antes de construir `AniListMediaListEntry`, así que nunca llegan a `SyncService`; quedan ausentes de esa corrida como cualquier otro anime no presente en la respuesta.

## Design Notes

- **Access token:** `OAuth2AuthorizedClientService.loadAuthorizedClient("anilist", authentication.getName())` dentro del success handler -- mismo principal-name (id de AniList) que ya usa el gate de whitelist.
- **Reconciliación:** cargar los `TrackingEntry` activos del usuario en un `Map<Long, TrackingEntry>` por `anilistMediaId` antes de iterar la respuesta; actualizar/crear por cada entrada remota, y al final desactivar los que quedaron sin tocar en el map.
- **Mapeo de status:** `TrackingStatus.valueOf(rawStatus)` envuelto en try/catch de `IllegalArgumentException` -- valores no reconocidos (`PAUSED`, etc.) se descartan silenciosamente para esa corrida, según lo confirmado en Ask First.

## Verification

**Commands:**
- `./gradlew build` -- expected: `BUILD SUCCESSFUL`, incluyendo `SyncServiceTest` en verde
- `./gradlew test --tests "*SyncServiceTest"` -- expected: las 5 filas de la I/O Matrix pasan contra Postgres real (Testcontainers)

**Manual checks (if no CLI):**
- Completar un login real (o con una cuenta de prueba de AniList con algunas entradas en distintos estados) y verificar en la base que se crearon `tracking_entry` + `snapshot` coherentes con la lista real antes de que se renderice cualquier página post-login

## Suggested Review Order

**Sync forzado en el login (el corazón del cambio)**

- Punto de entrada: tras `findOrCreate`, dispara el sync síncrono antes del redirect final (AD-7)
  [`OAuthLoginSuccessHandler.java:109`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L109)

- `syncOnLoginBestEffort`: resuelve el access token vía `OAuth2AuthorizedClientService`, nunca bloquea el login
  [`OAuthLoginSuccessHandler.java:121`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L121)

- Sin authorized client (token ausente): se loggea y el login sigue igual, sin sync
  [`OAuthLoginSuccessHandler.java:125`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L125)

- Cualquier falla de sync (red/timeout) se traga acá -- nunca se convierte en 500 ni en `/login?error`
  [`OAuthLoginSuccessHandler.java:135`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L135)

**Reconciliación y Snapshots (`SyncService`, único codepath para 2.1 y 2.2)**

- `syncUser`: fetch a AniList + reconciliación + Snapshot, todo en una transacción
  [`SyncService.java:46`](../../src/main/java/com/animetracker/sync/SyncService.java#L46)

- Dedup por `mediaId` antes de upsertear (code-review patch #3) -- evita romper la corrida si AniList repite un media entre listas
  [`SyncService.java:56`](../../src/main/java/com/animetracker/sync/SyncService.java#L56)

- Upsert in-place vs. alta nueva + Snapshot por entrada presente, con el mismo `takenAt` de la corrida
  [`SyncService.java:71`](../../src/main/java/com/animetracker/sync/SyncService.java#L71)

- Baja lógica de lo que quedó sin tocar en el map (removido o con status no mapeado)
  [`SyncService.java:88`](../../src/main/java/com/animetracker/sync/SyncService.java#L88)

**Anti-corrupción AniList (`AniListMediaListClient`)**

- `fetchMediaList`: query `MediaListCollection(userId, type: ANIME)`, mismo patrón que `AniListViewerClient`
  [`AniListMediaListClient.java:77`](../../src/main/java/com/animetracker/integration/anilist/AniListMediaListClient.java#L77)

- `appendMapped`/`mapStatus`: filtra `media`/`progress` nulos y estados sin equivalente en `TrackingStatus` (p.ej. `PAUSED`), ahora con logging (code-review patch #4)
  [`AniListMediaListClient.java:108`](../../src/main/java/com/animetracker/integration/anilist/AniListMediaListClient.java#L108)

**Modelo de datos (entidades + esquema)**

- `TrackingEntry`: upsert in-place vs. baja lógica -- única escritora es `SyncService`
  [`TrackingEntry.java:72`](../../src/main/java/com/animetracker/domain/TrackingEntry.java#L72)

- `Snapshot`: registro histórico inmutable por (`TrackingEntry`, corrida)
  [`Snapshot.java:49`](../../src/main/java/com/animetracker/domain/Snapshot.java#L49)

- `tracking_entry` + `snapshot`: FKs, `UNIQUE(app_user_id, anilist_media_id)` como base de la reconciliación
  [`V2__create_tracking_entry_and_snapshot.sql:21`](../../src/main/resources/db/migration/V2__create_tracking_entry_and_snapshot.sql#L21)

**Lectura publicada (`SyncedDataQueryService`, AD-9)**

- Interfaz pública, única puerta de lectura para features futuras (Hoy, Por Estado, Tendencias)
  [`SyncedDataQueryService.java:14`](../../src/main/java/com/animetracker/sync/SyncedDataQueryService.java#L14)

- Implementación package-private sobre `TrackingEntryRepository`
  [`SyncedDataQueryServiceImpl.java:10`](../../src/main/java/com/animetracker/sync/SyncedDataQueryServiceImpl.java#L10)

**Tests**

- `SyncServiceTest`: cubre las 5 filas de la I/O Matrix contra Postgres real + el dedup de media repetido (patch #3)
  [`SyncServiceTest.java:81`](../../src/test/java/com/animetracker/sync/SyncServiceTest.java#L81)

- `AniListMediaListClientTest` (nuevo, patch #2): ejercita el parsing GraphQL real contra un `MockRestServiceServer`, incluyendo `PAUSED` y campos nulos
  [`AniListMediaListClientTest.java:73`](../../src/test/java/com/animetracker/integration/anilist/AniListMediaListClientTest.java#L73)

- `WhitelistGateTest`: nuevo test que prueba que el login dispara `SyncService.syncUser` de verdad cuando hay access token (patch #1)
  [`WhitelistGateTest.java:194`](../../src/test/java/com/animetracker/auth/WhitelistGateTest.java#L194)
