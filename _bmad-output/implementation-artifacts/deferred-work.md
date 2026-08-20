# Deferred Work

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-iniciar-sesion-con-anilist-oauth.md`
  summary: Configurar el pipeline de build de Tailwind CSS 4.x (CLI standalone) y migrar los estilos de Login de CSS inline/embebido a clases Tailwind con tokens compartidos.
  evidence: Se carveó de Story 1.1 para bajar el tamaño de la spec por debajo del rango objetivo; el login puede satisfacer los tokens de diseño oscuro-primero con CSS embebido mínimo (copiado 1:1 del mockup) sin bloquear las ACs de OAuth ni requerir el toolchain de Tailwind todavía.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-iniciar-sesion-con-anilist-oauth.md`
  summary: Documentación de desarrollo (README) con instrucciones para registrar una app OAuth real en AniList y comandos de build/run del proyecto.
  evidence: No es necesaria para que las ACs de OAuth de Story 1.1 sean verificables; se carveó para reducir el tamaño de la spec.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-iniciar-sesion-con-anilist-oauth.md`
  summary: Agregar un endpoint de logout (`/logout`) para que un usuario autenticado pueda cerrar sesión.
  evidence: Surgió en la revisión de Step 4 (review layers). Story 1.1 solo cubre login; no hay AC ni FR que pida logout todavía, y agregarlo ahora sería scope creep sobre la spec congelada. Real pero no bloqueante — revisar cuándo se implemente la Story con el chrome autenticado (Epic 3) o antes si se detecta necesidad de operar la whitelist manualmente y hay que forzar logout de un usuario.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-gate-de-whitelist-de-invitacion.md`
  summary: Entorno de desarrollo local para PostgreSQL (`docker-compose.yml`) y documentación de cómo levantarlo.
  evidence: Se carveó de Story 1.2 para bajar el tamaño de la spec por debajo del rango objetivo. Ninguna AC lo requiere: los tests de la capa JPA usan Testcontainers (levanta y destruye su propio Postgres), no una instancia de desarrollo persistente. Mismo patrón que el tooling diferido de Story 1.1 (Tailwind, README).

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-gate-de-whitelist-de-invitacion.md`
  summary: Re-validar la membresía en la Whitelist durante la sesión activa (no solo en el momento del login), para que quitar a un usuario de la Whitelist le corte el acceso de inmediato en vez de esperar a que expire su sesión.
  evidence: Surgió en Step 4 (review layers, blind-hunter). El gate actual solo corre una vez en `OAuthLoginSuccessHandler`; ninguna AC ni fila de la I/O Matrix de Story 1.2 cubre la revocación durante una sesión activa. Real pero no bloqueante para V1 (gestión de whitelist es manual y de bajo volumen); requeriría diseñar un filtro/interceptor nuevo, fuera del alcance de esta story.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-gate-de-whitelist-de-invitacion.md`
  summary: `epic-1-context.md` sigue afirmando que la pantalla de Login "usa los design tokens ... configurados en Tailwind", pero tanto Login como Acceso Denegado están implementadas con CSS embebido inline (el pipeline de Tailwind está deferred desde Story 1.1). Corregir la redacción del contexto del epic para que no contradiga la implementación real.
  evidence: Surgió en Step 4 (review layers, blind-hunter). Contradicción preexistente desde Story 1.1 que resurgió al reescribirse `epic-1-context.md` en esta story; no bloquea ninguna AC de Story 1.2.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-gate-de-whitelist-de-invitacion.md`
  summary: Agregar un constraint `CHECK` en `app_user.theme_preference` (restringir al set cerrado de temas soportados) y una columna `updated_at` para auditar cuándo cambió la preferencia de tema de un usuario.
  evidence: Surgió en Step 4 (review layers, blind-hunter). El toggle de tema es Epic 3 (fuera de alcance de Story 1.2 por el Boundary "Never" de la spec); la columna existe ya pero nada la restringe ni audita su mutación todavía. Revisar cuando Epic 3 implemente la escritura de `theme_preference`.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-gate-de-whitelist-de-invitacion.md`
  summary: Agregar un test que fuerce la carrera real de doble inserción concurrente en `AppUserProvisioningService.insertOrFetchExisting` (el catch de `DataIntegrityViolationException`), en vez de solo probar el camino donde el pre-check ya encuentra la fila.
  evidence: Surgió en Step 4 (review layers, verification-gap). `WhitelistGateTest.whitelistedReturningLoginReusesTheExistingAppUserInsteadOfDuplicating` invoca el handler dos veces secuencialmente, así que el segundo `findOrCreate` nunca llega al catch -- el mecanismo de seguridad ante la carrera documentado en el Javadoc de la clase queda sin cobertura automatizada. Ventana de carrera angosta para una app de whitelist manual de bajo volumen, no bloqueante para V1, pero requiere infraestructura de test más compleja (hilos concurrentes o mocking del repositorio) que amerita atención dedicada.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-gate-de-whitelist-de-invitacion.md`
  summary: Un usuario ya autenticado y en la Whitelist que navega manualmente a `GET /acceso-denegado` (público por `permitAll`) ve la pantalla de rechazo aunque su acceso siga siendo válido, porque `AccessDeniedController` no inspecciona el estado de autenticación.
  evidence: Surgió en Step 4 (review layers, edge-case-hunter). Inconsistencia de UX menor, no un problema de seguridad (la sesión del usuario no se toca); ninguna AC ni fila de la I/O Matrix cubre esta ruta de navegación directa.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-sincronizacion-forzada-en-el-login.md`
  summary: `AniListMediaListClient` (y `AniListViewerClient`, preexistente) no parsean el array `errors` de una respuesta GraphQL 200; cualquier falla de AniList que no sea un 4xx/5xx cae a un `IllegalStateException` genérico sin detalle accionable para logging/debug.
  evidence: Surgió en Step 4 (review layers, blind-hunter). Patrón preexistente desde Story 1.1 (`AniListViewerClient` tiene la misma limitación); no bloqueante para Story 2.1, pero amerita una pasada de consistencia sobre ambos clientes de AniList.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-sincronizacion-forzada-en-el-login.md`
  summary: El endpoint GraphQL y los timeouts connect/read de `AniListMediaListClient` (y `AniListViewerClient`) están hardcodeados como constantes en vez de externalizados vía `application.yml`.
  evidence: Surgió en Step 4 (review layers, blind-hunter). Mismo patrón preexistente desde Story 1.1; no bloqueante, revisar si se necesita tunear por ambiente sin redeploy.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-sincronizacion-forzada-en-el-login.md`
  summary: `SyncService.syncUser` no tiene protección ante dos corridas concurrentes para el mismo usuario (p.ej. doble login casi simultáneo): ambas transacciones pueden leer el mismo estado antes de que cualquiera escriba y colisionar en el `UNIQUE(app_user_id, anilist_media_id)`.
  evidence: Surgió en Step 4 (review layers, blind-hunter + edge-case-hunter). Se degrada con gracia hoy (el catch en `OAuthLoginSuccessHandler.syncOnLoginBestEffort` loggea y el login sigue igual, sin romper nada), a diferencia de la carrera de primer-login de `AppUser` que sí tiene un patrón `REQUIRES_NEW` dedicado. Ventana angosta, no bloqueante para V1, pero amerita el mismo tipo de solución si se vuelve un problema real.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-sincronizacion-forzada-en-el-login.md`
  summary: La query `MediaListCollection` no pagina (`perChunk`/`chunk`); AniList recomienda queries paginadas para listas muy grandes.
  evidence: Surgió en Step 4 (review layers, blind-hunter). No es un problema a la escala objetivo (~100-1000 usuarios, listas personales de anime), pero revisar si el tamaño de lista de algún usuario crece lo suficiente como para acercarse a límites de tamaño de respuesta de AniList.

- source_spec: `_bmad-output/implementation-artifacts/spec-2-1-sincronizacion-forzada-en-el-login.md`
  summary: Los FKs de `V2__create_tracking_entry_and_snapshot.sql` (`tracking_entry.app_user_id`, `snapshot.tracking_entry_id`) no especifican comportamiento `ON DELETE`; borrar un `AppUser` fallaría hoy por violación de FK.
  evidence: Surgió en Step 4 (review layers, blind-hunter). Irrelevante hoy porque no existe ninguna feature de borrado de cuenta en el producto; revisar cuando esa feature se planee.
