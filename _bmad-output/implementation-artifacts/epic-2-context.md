# Epic 2 Context: Motor de Sincronización y Snapshots

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Mantener los datos de la lista de AniList del usuario actualizados automáticamente en la base propia de AnimeTracker, mediante un job periódico en background y una sincronización forzada en cada login, persistiendo cada corrida como un Snapshot histórico. Cuando AniList no responde o el token OAuth fue revocado, el usuario debe ver con claridad la fecha del último dato conocido en vez de un dato roto o silenciosamente desactualizado. Este motor es la única fuente de datos para el resto de las vistas (Hoy, Por Estado, Tendencias): ninguna de ellas consulta AniList en vivo.

## Stories

- Story 2.1: Sincronización forzada en el login
- Story 2.2: Job periódico de sincronización en background
- Story 2.3: Degradación ante fallo de sincronización

## Requirements & Constraints

- El dashboard nunca debe disparar una consulta en vivo a AniList por carga de página; toda lectura de las demás features pasa por datos ya sincronizados (escala objetivo: ~100 usuarios activos el primer año, margen hasta 1000).
- Staleness aceptable fuera de un login activo: ~30-60 min. El intervalo del job periódico es configurable, con default arquitectónico de 45 min — no se debe maximizar la frecuencia del job para acercarse a los rate limits de AniList (frescura suficiente para uso diario, no tiempo real).
- Los Snapshots se retienen indefinidamente en V1; no existe política de purga definida todavía.
- Rate limit de AniList: nominal 90 req/min, con burst limiter y 429 al superarlo (históricamente degradado a 30 req/min en ventanas de incidente) — el límite de concurrencia debe ser conservador frente a esto.
- Una corrida de sincronización exitosa (login-sync o job) es la única señal que hace desaparecer el Banner de Onboarding global de MAL-Sync (Epic 6) y el Stale Banner activo.

## Technical Decisions

- **Único codepath de sync:** login-sync y job periódico reutilizan el mismo método single-user de `SyncService`; no existe una segunda implementación. La llamada de login-sync es síncrona/bloqueante, antes del primer render post-login (una sola query, no batch).
- **Job:** `@Scheduled` único, in-process (misma JVM que la web app, sin worker service separado). Intervalo configurable, default 45 min.
- **Concurrencia compartida:** el límite de concurrencia (semáforo/pool acotado, default 3, configurable) vive dentro de `integration.anilist`, envolviendo toda llamada a AniList sin importar el origen (job o login), de modo que ambos caminos comparten el mismo cupo automáticamente.
- **Aislamiento por usuario:** cada usuario se procesa en su propio try/catch durante la corrida del job; la falla de uno no bloquea la sincronización del resto.
- **Reconciliación completa por corrida:** cada sync hace upsert de las entradas de `TrackingEntry` presentes en la respuesta de AniList y da de baja lógica las que ya no aparecen. Esta es la única vía de escritura sobre datos derivados de AniList (single-writer).
- **Ownership de datos:** los repositorios JPA de `TrackingEntry` y `Snapshot` viven exclusivamente dentro del package `sync`, con visibilidad package-private. El resto de las features (`hoy`, `porestado`, `tendencias`, `onboarding`) lee estos datos solo a través de `SyncedDataQueryService`, la interfaz de solo lectura publicada por `sync`.
- **Snapshot por (usuario, anime, corrida):** nunca un agregado único por usuario — misma cardinalidad que `TrackingEntry`. Esto es lo que permite calcular Tendencias (Epic 5) on-read, sumando diferencias de episodio entre Snapshots consecutivos por anime.
- **Acceso a AniList exclusivamente vía `integration.anilist`:** capa anti-corrupción; ninguna mutation GraphQL existe en el codebase, solo lectura. Solo `auth` y `sync` dependen de `integration.anilist`.
- **Degradación, no excepción:** un token OAuth inválido/expirado detectado durante sync dispara el camino de degradación de FR-9 (mostrar último Snapshot conocido con fecha explícita), nunca una excepción sin manejar. Las fallas se loggean y degradan; nunca rompen el render de página.
- **Timestamps:** siempre `Instant` UTC en DB. Cada entidad local mantiene su propia PK más una columna indexada separada para el id remoto de AniList (nunca conflar ambos ids).
- **Cliente AniList:** Spring `RestClient` + Jackson DTOs hand-mapped (sin librería de GraphQL client/codegen), contra AniList API GraphQL v2 vía OAuth2 authorization-code.
- **Despliegue:** single-instance — un contenedor Docker con el monolito completo (web + scheduler in-process); sin locking distribuido ni escalado multi-instancia del job en V1.

## UX & Interaction Patterns

- **Stale Banner:** franja superior de página (no modal, no bloqueante, no dismissible manualmente), visible en cualquier superficie autenticada cuando una sincronización degrada, con la fecha explícita del último Snapshot conocido ("datos de [fecha]"). Desaparece solo cuando una sincronización exitosa refresca el dato — nunca por acción manual del usuario.
- Usa el color `warning` (fondo al 14% de opacidad sobre la superficie base, texto sólido) de forma exclusiva — este color no se reutiliza para ningún otro componente o estado.
- Se anuncia vía `aria-live="polite"` al aparecer, para que un lector de pantalla lo capture sin interrumpir la navegación en curso.
- El botón de reintento de sync (cuando exista en la UI) es la única excepción a "primary sin estado disabled": se deshabilita mientras la petición está en curso y vuelve a habilitarse al resolver.

## Cross-Story Dependencies

- Story 2.1 (login-sync) depende del gate de whitelist de Epic 1 (Story 1.2): la sincronización síncrona corre recién después de abierta la sesión.
- Story 2.2 (job periódico) comparte el limitador de concurrencia y el método single-user de sync con Story 2.1 — deben implementarse sobre la misma base de `SyncService`/`integration.anilist`, no como caminos separados.
- Story 2.3 (degradación) aplica tanto a fallas originadas en el job (2.2) como en el login-sync (2.1); el Stale Banner que produce es consumido globalmente por Epic 3 (Hoy), Epic 4 (Por Estado) y Epic 5 (Tendencias).
- `SyncedDataQueryService`, publicado por esta épica, es la única puerta de lectura que usarán Epic 3 (Hoy), Epic 4 (Por Estado) y Epic 5 (Tendencias) para acceder a `TrackingEntry`/`Snapshot`.
- La detección de "usuario sin ningún Snapshot" que produce esta épica alimenta el Banner de Onboarding global de Epic 6.
