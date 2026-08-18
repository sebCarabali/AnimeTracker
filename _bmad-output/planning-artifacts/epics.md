---
stepsCompleted: [1, 2, 3]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-AnimeList-2026-08-13/prd.md
  - _bmad-output/planning-artifacts/architecture/architecture-AnimeList-2026-08-18/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-AnimeList-2026-08-13/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-AnimeList-2026-08-13/EXPERIENCE.md
  - _bmad-output/planning-artifacts/briefs/brief-AnimeList-2026-08-13/brief.md
  - _bmad-output/planning-artifacts/briefs/brief-AnimeList-2026-08-13/addendum.md
---

# AnimeTracker - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for AnimeTracker, decomposing the requirements from the PRD, UX Design Contract (DESIGN.md + EXPERIENCE.md), Architecture Spine, and Product Brief into implementable stories.

## Requirements Inventory

### Functional Requirements

FR-1: Un usuario puede autenticarse en AnimeTracker usando el flujo OAuth de AniList. El login redirige al flujo OAuth oficial de AniList (no pide ni almacena contraseña propia); un login exitoso abre una sesión válida en AnimeTracker.

FR-2: Solo un usuario de AniList presente en la Whitelist de Invitación puede completar el login y acceder a las vistas de AnimeTracker. Un usuario que completa el OAuth pero no está en la Whitelist recibe un mensaje explícito de acceso denegado (no un error genérico); agregar un usuario a la Whitelist habilita su acceso sin re-registro.

FR-3: El usuario autenticado ve, al entrar, la vista "Hoy / Seguí Viendo": sus animes en estado *viendo* ordenados por actividad reciente, cada uno con título, episodio visto más reciente y próximo episodio pendiente. Un anime sin progreso en la sincronización más reciente no aparece. Si no hay ningún anime en *viendo*, se comunica ese estado vacío explícitamente.

FR-4: El usuario puede ver su lista de anime filtrada por cualquiera de los Estados de seguimiento estándar de AniList (viendo, planeado, completado, abandonado, repitiendo). Cada estado tiene una vista filtrada correspondiente; el conteo por estado coincide con lo reportado por AniList al momento del último Sync.

FR-5: El usuario puede ver un gráfico/listado de episodios vistos agrupados por semana (y/o mes) a partir de los Snapshots históricos. El valor de un período se calcula por diferencia entre Snapshots consecutivos dentro del período (no conteo acumulado). Un período sin Snapshots registrados se muestra como dato faltante, no como cero falso.

FR-6: Un proceso de background (job periódico) consulta la API de AniList por cada usuario habilitado a intervalos regulares y actualiza su estado en AnimeTracker, sin requerir sesión abierta del usuario. El job respeta los rate limits de AniList procesando usuarios con concurrencia limitada.

FR-7: Al iniciar sesión, AnimeTracker fuerza una sincronización síncrona del usuario que está entrando, además del job periódico, de modo que los datos mostrados inmediatamente post-login reflejan una consulta hecha en ese login.

FR-8: Cada corrida de sincronización (periódica o por login) persiste un Snapshot del estado del usuario en la base de datos propia de AnimeTracker, asociado a usuario/anime y timestamp. Los Snapshots son la única fuente de datos para Tendencias Históricas (FR-5) — no se re-consulta AniList para tendencias pasadas.

FR-9: Si la API de AniList no responde o el usuario revocó el acceso OAuth, AnimeTracker muestra el último Snapshot conocido con su fecha explícita ("datos de [fecha]"), en vez de romper o presentar el dato viejo como actual. Una falla de sincronización de un usuario no bloquea el job para el resto.

FR-10: AnimeTracker publica una página con instrucciones para instalar y configurar MAL-Sync contra AniList, accesible para un usuario recién invitado incluso sin Snapshots todavía.

### NonFunctional Requirements

NFR-1 (Escala): Diseño para ~100 usuarios activos el primer año, con margen hasta 1000. El Dashboard nunca debe disparar una consulta en vivo a AniList por carga de página.

NFR-2 (Staleness aceptable): El dashboard no es tiempo real estricto; staleness máxima aceptable fuera de un login activo ~30-60 min (intervalo de job configurable, default arquitectónico 45 min).

NFR-3 (Seguridad de sesión/token): El token OAuth de AniList se maneja exclusivamente server-side, atado a la sesión — nunca expuesto al cliente (cookie/HTML/JS). Sin requisitos de seguridad adicionales más allá de manejo estándar de sesión server-side.

NFR-4 (Retención de datos): Los Snapshots se retienen indefinidamente en V1, sin política de purga definida.

NFR-5 (Contra-métrica SM-C1): No maximizar la frecuencia del job de sincronización para acercarse a los rate limits de AniList — frescura suficiente para uso diario, no tiempo real.

### Additional Requirements

- Paradigma en capas por feature-package (Controller Thymeleaf → Service → Repository) sobre un `domain` compartido, con `integration.anilist` como única capa anti-corrupción hacia AniList — ningún Controller/Thymeleaf template la invoca directo (AD-1, AD-9, enforced por test ArchUnit).
- Los repositorios JPA de `TrackingEntry` y `Snapshot` viven exclusivamente dentro del package `sync` con visibilidad package-private; el resto de features lee solo vía `SyncedDataQueryService` (interfaz de solo lectura publicada por `sync`) (AD-2).
- Cada corrida de sync (job o login) hace reconciliación completa por usuario: upsert de entradas presentes en AniList + baja lógica de `TrackingEntry` que ya no aparece en la respuesta (AD-2).
- AniList es la única fuente de verdad; la DB propia es cache de lectura + bitácora de Snapshots — ninguna mutation GraphQL existe en el codebase (AD-3).
- Autenticación vía Spring Security + OAuth2 client; token inválido/expirado dispara el camino de degradación de FR-9, no una excepción sin manejar (AD-4).
- Whitelist gate inmediatamente tras el callback OAuth, antes de crear sesión o fila `AppUser`: se consulta `WhitelistedUser` por id de AniList; si no está, redirige a Acceso Denegado sin crear sesión ni fila; si está, `auth` es el único owner de `findOrCreate(AppUser)` (AD-5). Gestión de whitelist manual (DB directa) en V1, sin UI admin.
- Job `@Scheduled` único, in-process (mismo JVM que la web app, sin worker service separado), intervalo default 45 min configurable. Límite de concurrencia (semáforo/pool acotado, default 3, configurable) vive dentro de `integration.anilist`, compartido entre job y login-sync. Cada usuario se procesa en su propio try/catch (AD-6).
- Login-sync reutiliza el mismo método single-user de `SyncService` que usa el job — no existe un segundo codepath de sincronización; la llamada es síncrona/bloqueante antes del primer render post-login (AD-7).
- Tendencias calculadas on-read desde Snapshots por (usuario, anime) — un `Snapshot` por (usuario, anime, corrida de sync), nunca un agregado único por usuario; el valor de un período se calcula sumando diferencias de episodio entre Snapshots consecutivos por anime en tiempo de consulta; período sin ningún Snapshot se devuelve como dato faltante explícito (AD-8).
- Dirección de dependencias: solo `auth` y `sync` dependen de `integration.anilist`; `hoy`, `porestado`, `tendencias`, `onboarding` dependen únicamente de `domain`, leyendo vía `SyncedDataQueryService`; `integration.anilist` depende de `domain`, nunca al revés (AD-9).
- Despliegue single-instance: un contenedor Docker con el monolito completo (web + scheduler in-process) + una instancia de PostgreSQL separada. Proveedor de hosting concreto: deferred (AD-10).
- Naming: `TrackingStatus` mapea 1:1 a `MediaListStatus` de AniList (CURRENT/PLANNING/COMPLETED/DROPPED/REPEATING) — nunca un enum en español inventado en el dominio; localización solo en templates Thymeleaf.
- Timestamps siempre `Instant` UTC en DB; cada entidad local mantiene su propia PK más una columna indexada separada para el id remoto de AniList — nunca conflar ambos ids.
- Preferencia de tema persistida server-side como columna del usuario (no `localStorage`), para que sobreviva entre dispositivos.
- Config/secrets vía `application.yml` + variables de entorno (client id/secret AniList, credenciales DB) — nunca committeados.
- Stack fijado: Java 25 (LTS), Spring Boot 4.1.x (Spring Framework 7, Jakarta EE 11), Thymeleaf 3.1.x server-rendered, htmx 2.x para fragment swaps puntuales (ej. toggle semana/mes de Tendencias), Tailwind CSS 4.3.x, Spring Data JPA + Flyway (migraciones versionadas), PostgreSQL 18.x, cliente AniList vía Spring `RestClient` + Jackson DTOs hand-mapped (sin librería GraphQL client/codegen).
- Estructura de paquetes por feature: `domain/`, `integration/anilist/`, `auth/`, `sync/`, `hoy/`, `porestado/`, `tendencias/`, `onboarding/`, `config/`.

### UX Design Requirements

UX-DR1: Implementar el sistema de design tokens (colores light/dark, tipografía Sora/Inter, escala de spacing 4px, radios) como configuración Tailwind, oscuro-primero con modo claro vía toggle sobre la misma lógica de rol de color.

UX-DR2: Construir el componente Poster Card (usado en Hoy y Por Estado): póster, título en `heading`, próximo episodio pendiente en `numeric` con acento cuando aplica a Hoy, badge de estado en la esquina; no navega a ninguna página de detalle (no hay vista de detalle de anime en V1).

UX-DR3: Construir el componente Status Badge (píldora `label-caps`) para los 5 estados de AniList (viendo/completado/planeado/repitiendo/abandonado), cada uno con su par fill/foreground fijo y texto accesible del nombre del estado (nunca solo color); verificar contraste WCAG AA para cada uno de los 4 pares fill/foreground, no solo el de *viendo*.

UX-DR4: Construir la Nav principal persistente (Hoy / Por Estado / Tendencias / Configurar MAL-Sync), sin jerarquía anidada, con indicador de línea inferior en el ítem activo; sin auto-hide en scroll. Responsive: barra horizontal en `md`/`lg`+, colapso a barra inferior o menú hamburguesa en `sm`.

UX-DR5: Construir el componente Skeleton para carga inicial post-login en cualquier superficie: reemplaza el layout final pieza por pieza (misma grilla de Poster cards o Trend bars), sin spinner de página completa.

UX-DR6: Construir el Stale Banner: franja superior no bloqueante, no dismissible manualmente, visible en cualquier superficie cuando FR-9 degrada, con fecha del último Snapshot conocido; anunciado vía `aria-live="polite"`. Color de warning exclusivo de este componente.

UX-DR7: Construir el componente Trend Bar para Tendencias: barra por período (semana con toggle a mes) con tres estados visuales — período vigente (acento), período pasado con dato, período sin Snapshot (contorno punteado, nunca barra en cero); cada barra expone su valor o "sin dato" como texto accesible.

UX-DR8: Construir el Banner de Onboarding de MAL-Sync: visible globalmente (Hoy, Por Estado, Tendencias) mientras el usuario no tenga ningún Snapshot registrado, reemplazando el estado vacío genérico de cada superficie; enlaza a "Configurar MAL-Sync"; desaparece tras la primera sincronización exitosa.

UX-DR9: Construir el Theme Toggle (header, todas las superficies autenticadas): un solo control oscuro⇄claro, sin tercer estado "sistema", cambio inmediato sin confirmación, persistido server-side (no localStorage).

UX-DR10: Construir los componentes Button Primary/Secondary: primary sin estado disabled salvo durante el reintento de sync (se deshabilita mientras la petición está en curso); secondary siempre navega, nunca dispara acción destructiva.

UX-DR11: Construir el componente Empty State (Hoy, Por Estado) sin botón de acción (app de solo lectura), con copy específico por contexto (ver microcopy UX-DR14) — distinto del banner de onboarding de MAL-Sync (que tiene prioridad cuando nunca hubo Snapshot).

UX-DR12: Implementar las pantallas de Login y Acceso Denegado como estados distintos: falla transitoria de OAuth previa al login (con botón de reintento) vs. Acceso Denegado por no estar en Whitelist (página dedicada, sin reintento automático).

UX-DR13: Implementar los patrones de microcopy del PRD/EXPERIENCE.md (tabla Do/Don't): frases cortas, sin exclamaciones ni emojis, sin tono motivacional/gamificación; textos específicos para Hoy vacío, Tendencias sin dato, Stale banner y Acceso Denegado.

UX-DR14: Implementar la interacción htmx de toggle semana/mes en Tendencias: recalcula las Trend bars in-place sin navegar ni recargar la página.

UX-DR15: Cumplir el piso de accesibilidad (WCAG 2.2 AA) en ambos modos de color para botones y los 4 pares fill/foreground de Status badge; foco de teclado visible en nav, toggle de tema y links, con orden de tabulación siguiendo el orden de lectura de cada superficie.

UX-DR16: Implementar la grilla responsive de tarjetas (Hoy, Por Estado): 1 columna en mobile (`sm`), 2-3 en tablet (`md`), 4+ en desktop (`lg`+), sin tabla ancha en ningún breakpoint.

### FR Coverage Map

```
FR-1:  Epic 1 - Login OAuth AniList
FR-2:  Epic 1 - Whitelist gate
FR-3:  Epic 3 - Vista Hoy/Seguí Viendo
FR-4:  Epic 4 - Vista Por Estado
FR-5:  Epic 5 - Tendencias históricas
FR-6:  Epic 2 - Job periódico de sync
FR-7:  Epic 2 - Sync síncrono en login
FR-8:  Epic 2 - Persistencia de Snapshots
FR-9:  Epic 2 - Degradación ante fallo AniList
FR-10: Epic 6 - Página de instrucciones MAL-Sync
```

NFR-1, NFR-2, NFR-4, NFR-5: Epic 2 (motor de sync/lectura local). NFR-3: Epic 1 (sesión/token server-side).
UX-DR15 (accesibilidad WCAG 2.2 AA): verificado dentro de cada historia de componente (no es épica aparte).

## Epic List

### Epic 1: Autenticación y Control de Acceso
Un usuario invitado puede iniciar sesión con su cuenta de AniList vía OAuth; alguien no invitado ve un mensaje explícito de acceso denegado en vez de un error genérico.
**FRs covered:** FR-1, FR-2 (NFR-3; UX-DR1, UX-DR10, UX-DR12)

### Epic 2: Motor de Sincronización y Snapshots
Los datos de la lista de AniList del usuario se mantienen actualizados automáticamente (job periódico + sync forzado en login), persistidos como Snapshots; si AniList falla o el token se revocó, el usuario ve claramente la fecha del último dato conocido en vez de un dato roto o silenciosamente viejo.
**FRs covered:** FR-6, FR-7, FR-8, FR-9 (NFR-1, NFR-2, NFR-4, NFR-5; UX-DR6)

### Epic 3: Vista Hoy / Seguí Viendo
Al entrar, el usuario ve sus animes en curso ordenados por actividad reciente. Introduce el chrome global (Nav, Theme Toggle) como primera superficie autenticada real.
**FRs covered:** FR-3 (UX-DR2, UX-DR4, UX-DR5, UX-DR9, UX-DR11, UX-DR13)

### Epic 4: Vista Por Estado
El usuario puede filtrar y navegar su lista completa por cualquiera de los 5 estados de AniList, reutilizando Poster Card/Nav de Epic 3.
**FRs covered:** FR-4 (UX-DR3, UX-DR16, UX-DR11)

### Epic 5: Tendencias Históricas
El usuario puede ver episodios vistos agrupados por semana/mes, calculados a partir de Snapshots, con toggle in-place vía htmx.
**FRs covered:** FR-5 (UX-DR7, UX-DR14)

### Epic 6: Onboarding de MAL-Sync
Un usuario recién invitado sin Snapshots todavía ve una guía clara para instalar MAL-Sync (en vez de un empty state genérico confuso), visible globalmente en Hoy/Por Estado/Tendencias hasta el primer sync exitoso.
**FRs covered:** FR-10 (UX-DR8, UX-DR13)

## Epic 1: Autenticación y Control de Acceso

Un usuario invitado puede iniciar sesión con su cuenta de AniList vía OAuth; alguien no invitado ve un mensaje explícito de acceso denegado en vez de un error genérico.

### Story 1.1: Iniciar sesión con AniList (OAuth)

As a usuario invitado,
I want iniciar sesión usando mi cuenta de AniList,
So that puedo acceder a AnimeTracker sin crear ni recordar una contraseña propia.

**Acceptance Criteria:**

**Given** un usuario no autenticado en la pantalla de Login
**When** hace click en "Iniciar sesión con AniList"
**Then** es redirigido al flujo OAuth oficial de AniList
**And** no se le pide ni se almacena una contraseña propia

**Given** el usuario completa el OAuth exitosamente
**When** AniList redirige al callback
**Then** el token OAuth recibido se maneja exclusivamente server-side desde ese momento, nunca expuesto en cookie/HTML/JS (NFR-3)
**And** el control pasa al gate de whitelist (Story 1.2) antes de crear cualquier sesión o fila `AppUser`

**Given** una falla transitoria de OAuth (usuario cancela, error de red) antes de completar el login
**When** vuelve al callback o la redirección falla
**Then** ve la pantalla de Login con mensaje de error y botón de reintento (UX-DR12)
**And** no se crea ninguna sesión

**Given** los design tokens (UX-DR1: colores light/dark, tipografía Sora/Inter, spacing 4px, radios) configurados en Tailwind
**When** se renderiza la pantalla de Login
**Then** usa esos tokens, oscuro-primero

### Story 1.2: Gate de Whitelist de Invitación

As a operador de AnimeTracker,
I want que solo los usuarios de AniList presentes en la Whitelist completen el login,
So that el acceso queda restringido a usuarios invitados sin exponer la app públicamente.

**Acceptance Criteria:**

**Given** un usuario completa el OAuth exitosamente
**When** el callback se procesa
**Then** se consulta `WhitelistedUser` por el id de AniList del usuario antes de crear sesión o fila `AppUser` (AD-5)

**Given** el id de AniList no está en la Whitelist
**When** se evalúa el gate
**Then** el usuario es redirigido a la pantalla de Acceso Denegado con un mensaje explícito
**And** no se crea sesión ni fila `AppUser`

**Given** el id de AniList está en la Whitelist
**When** se evalúa el gate
**Then** `auth` invoca `findOrCreate(AppUser)` (único owner de esa operación)
**And** se abre una sesión válida

**Given** un usuario fue agregado a la Whitelist después de un intento fallido
**When** vuelve a intentar el login
**Then** completa el acceso sin necesidad de re-registro

**Given** la pantalla de Acceso Denegado
**When** se muestra
**Then** es una página dedicada, distinta de la de Login, sin botón de reintento automático (UX-DR12)

## Epic 2: Motor de Sincronización y Snapshots

Los datos de la lista de AniList del usuario se mantienen actualizados automáticamente (job periódico + sync forzado en login), persistidos como Snapshots; si AniList falla o el token se revocó, el usuario ve claramente la fecha del último dato conocido en vez de un dato roto o silenciosamente viejo.

### Story 2.1: Sincronización forzada en el login

As a usuario que acaba de iniciar sesión,
I want que mis datos de AniList se sincronicen automáticamente en ese momento,
So that lo que veo inmediatamente post-login refleja una consulta hecha en ese login.

**Acceptance Criteria:**

**Given** una sesión recién creada (después del gate de whitelist)
**When** se abre la sesión
**Then** AnimeTracker ejecuta de forma síncrona/bloqueante una consulta a la API de AniList para ese usuario antes del primer render post-login (AD-7)

**Given** la consulta a AniList devuelve la lista actual del usuario
**When** se procesa la respuesta
**Then** se hace upsert de las entradas presentes (`TrackingEntry`)
**And** baja lógica de las que ya no aparecen (reconciliación completa) (AD-2)

**Given** la corrida de sync se completó
**When** termina
**Then** se persiste un `Snapshot` del estado por (usuario, anime, corrida de sync), asociado a timestamp `Instant` UTC (AD-8, FR-8)

**Given** el flujo de sync
**When** se implementa el acceso a AniList
**Then** pasa exclusivamente por `integration.anilist` (capa anti-corrupción), sin ninguna mutation GraphQL — solo lectura (AD-1, AD-3)

**Given** los repositorios JPA de `TrackingEntry`/`Snapshot`
**When** se implementan
**Then** viven en el package `sync` con visibilidad package-private (AD-2)

**Given** cualquier feature de lectura (Hoy, Por Estado, Tendencias)
**When** necesita datos sincronizados
**Then** los obtiene exclusivamente vía la interfaz `SyncedDataQueryService` publicada por `sync` — nunca consultando AniList en vivo por carga de página (NFR-1, AD-9)

### Story 2.2: Job periódico de sincronización en background

As a usuario de AnimeTracker,
I want que mis datos se actualicen periódicamente aunque no tenga sesión abierta,
So that al volver a entrar mi información ya está razonablemente fresca.

**Acceptance Criteria:**

**Given** AnimeTracker corriendo
**When** pasa el intervalo configurado (default 45 min, configurable)
**Then** un job `@Scheduled` único, in-process, sincroniza a todos los usuarios habilitados reutilizando el mismo método single-user de `SyncService` que usa el login-sync (AD-7)

**Given** múltiples usuarios a sincronizar
**When** el job corre
**Then** respeta un límite de concurrencia (semáforo/pool acotado, default 3, configurable) que vive en `integration.anilist`, compartido con login-sync (AD-6)

**Given** un usuario individual falla durante la corrida del job
**When** ocurre el error
**Then** se captura en su propio try/catch y no bloquea la sincronización del resto de usuarios (AD-6)

**Given** el intervalo del job
**When** se configura
**Then** no maximiza la frecuencia cerca de los rate limits de AniList — frescura suficiente para uso diario, no tiempo real (NFR-5)

### Story 2.3: Degradación ante fallo de sincronización

As a usuario de AnimeTracker,
I want ver claramente cuándo mis datos no están al día por una falla de sincronización,
So that no confundo un dato viejo con uno actual ni veo la app romperse.

**Acceptance Criteria:**

**Given** la API de AniList no responde o el token OAuth fue revocado durante una sincronización (job o login)
**When** falla la corrida
**Then** AnimeTracker muestra el último Snapshot conocido con su fecha explícita ("datos de [fecha]") en vez de romper o mostrar el dato viejo como actual

**Given** una degradación activa
**When** el usuario navega cualquier superficie (Hoy, Por Estado, Tendencias)
**Then** ve el Stale Banner: franja superior no bloqueante, no dismissible manualmente, con la fecha del último Snapshot conocido, anunciada vía `aria-live="polite"` (UX-DR6)

**Given** el Stale Banner
**When** se muestra
**Then** usa un color de warning exclusivo de este componente

**Given** un token OAuth inválido/expirado detectado durante sync
**When** ocurre
**Then** dispara este camino de degradación, nunca una excepción sin manejar (AD-4)

## Epic 3: Vista Hoy / Seguí Viendo

Al entrar, el usuario ve sus animes en curso ordenados por actividad reciente. Introduce el chrome global (Nav, Theme Toggle) porque es la primera superficie autenticada real.

### Story 3.1: Layout autenticado con Nav y Theme Toggle

As a usuario autenticado,
I want una navegación persistente y un control de tema consistentes en toda la app,
So that puedo moverme entre secciones y ajustar el tema visual sin fricción.

**Acceptance Criteria:**

**Given** un usuario autenticado
**When** navega cualquier superficie
**Then** ve la Nav principal persistente (Hoy / Por Estado / Tendencias / Configurar MAL-Sync), sin jerarquía anidada, con indicador de línea inferior en el ítem activo, sin auto-hide en scroll (UX-DR4)

**Given** un viewport `sm`
**When** se renderiza la Nav
**Then** colapsa a barra inferior o menú hamburguesa
**And** en `md`/`lg`+ es una barra horizontal (UX-DR4)

**Given** el header de cualquier superficie autenticada
**When** el usuario hace click en el Theme Toggle
**Then** cambia entre modo oscuro y claro inmediatamente sin confirmación, sin tercer estado "sistema" (UX-DR9)

**Given** un cambio de tema
**When** se aplica
**Then** se persiste server-side como columna del usuario (no localStorage), sobreviviendo entre dispositivos (UX-DR9)

### Story 3.2: Vista Hoy con datos reales

As a usuario autenticado,
I want ver al entrar mis animes en curso con su progreso más reciente,
So that sé inmediatamente qué sigue viendo y qué episodio corresponde.

**Acceptance Criteria:**

**Given** un usuario autenticado con animes en estado *viendo*
**When** entra a Hoy
**Then** ve sus animes en *viendo* ordenados por actividad reciente, cada uno con título, episodio visto más reciente y próximo episodio pendiente, leídos vía `SyncedDataQueryService` (FR-3, NFR-1)

**Given** un anime sin progreso en la sincronización más reciente
**When** se renderiza Hoy
**Then** ese anime no aparece en la lista

**Given** el usuario no tiene ningún anime en *viendo*
**When** entra a Hoy
**Then** ve el Empty State específico de Hoy (sin botón de acción, copy de UX-DR13), distinto del banner de onboarding

**Given** la carga inicial post-login de Hoy
**When** los datos aún no llegaron
**Then** se muestra el componente Skeleton (misma grilla de Poster cards, sin spinner de página completa) (UX-DR5)

**Given** cada anime en Hoy
**When** se renderiza como tarjeta
**Then** usa el componente Poster Card (póster, título en `heading`, próximo episodio en `numeric` con acento, badge de estado, sin navegación a detalle) (UX-DR2)

## Epic 4: Vista Por Estado

El usuario puede filtrar y navegar su lista completa por cualquiera de los 5 estados de AniList, reutilizando Poster Card/Nav de Epic 3.

### Story 4.1: Filtro de lista por estado de seguimiento

As a usuario autenticado,
I want filtrar mi lista completa por cualquiera de los estados estándar de AniList,
So that puedo revisar, por ejemplo, todo lo que planeo ver o todo lo que abandoné.

**Acceptance Criteria:**

**Given** un usuario autenticado
**When** entra a Por Estado
**Then** puede filtrar su lista por cualquiera de los 5 Estados estándar de AniList (viendo, planeado, completado, abandonado, repitiendo), leyendo vía `SyncedDataQueryService`

**Given** un estado filtrado
**When** se muestra el conteo
**Then** coincide con lo reportado por AniList al momento del último Sync (FR-4)

**Given** la grilla de tarjetas
**When** se renderiza en distintos breakpoints
**Then** usa 1 columna en `sm`, 2-3 en `md`, 4+ en `lg`+, sin tabla ancha en ningún breakpoint (UX-DR16)

**Given** un estado sin ningún anime
**When** se filtra
**Then** se muestra el Empty State de Por Estado (sin botón de acción, copy específico) (UX-DR11)

### Story 4.2: Status Badge con contraste accesible

As a usuario autenticado,
I want distinguir claramente el estado de cada anime sin depender solo del color,
So that la información es accesible incluso con dificultades de percepción de color.

**Acceptance Criteria:**

**Given** los 5 estados de AniList (viendo/completado/planeado/repitiendo/abandonado)
**When** se renderiza el Status Badge
**Then** cada uno usa su par fill/foreground fijo y texto accesible del nombre del estado (nunca solo color) (UX-DR3)

**Given** cada uno de los 4 pares fill/foreground restantes (fuera de *viendo*)
**When** se audita
**Then** cumple contraste WCAG AA (UX-DR3, UX-DR15)

**Given** el Status Badge
**When** aparece en Poster Card (Hoy) o en Por Estado
**Then** es el mismo componente reutilizado sin duplicación de estilos

## Epic 5: Tendencias Históricas

El usuario puede ver episodios vistos agrupados por semana/mes, calculados a partir de Snapshots, con toggle in-place vía htmx.

### Story 5.1: Cálculo y visualización de tendencias por semana

As a usuario autenticado,
I want ver cuántos episodios vi por semana a lo largo del tiempo,
So that entiendo mi propio ritmo de consumo histórico.

**Acceptance Criteria:**

**Given** los Snapshots históricos del usuario
**When** entra a Tendencias
**Then** ve episodios vistos agrupados por semana, calculados por diferencia entre Snapshots consecutivos dentro del período (no conteo acumulado), leídos vía `SyncedDataQueryService` (FR-5, AD-8)

**Given** un período sin ningún Snapshot registrado
**When** se renderiza
**Then** se muestra como dato faltante explícito (contorno punteado en su Trend Bar), nunca como cero falso

**Given** el período vigente
**When** se renderiza su Trend Bar
**Then** usa el acento visual distintivo de "período vigente" (UX-DR7)

**Given** cada Trend Bar
**When** se renderiza
**Then** expone su valor o "sin dato" como texto accesible, no solo visual (UX-DR7, UX-DR15)

### Story 5.2: Toggle semana/mes in-place vía htmx

As a usuario autenticado,
I want alternar entre vista semanal y mensual de mis tendencias sin recargar la página,
So that puedo comparar rápidamente ambos niveles de detalle.

**Acceptance Criteria:**

**Given** la vista de Tendencias
**When** el usuario activa el toggle de semana/mes
**Then** las Trend bars se recalculan in-place sin navegar ni recargar la página completa (htmx fragment swap) (UX-DR14)

**Given** el toggle mensual
**When** se selecciona
**Then** agrupa los mismos Snapshots por mes en vez de semana, aplicando la misma lógica de diferencia entre Snapshots consecutivos (FR-5)

## Epic 6: Onboarding de MAL-Sync

Un usuario recién invitado sin Snapshots todavía ve una guía clara para instalar MAL-Sync (en vez de un empty state genérico confuso), visible globalmente en Hoy/Por Estado/Tendencias hasta el primer sync exitoso.

### Story 6.1: Página de instrucciones MAL-Sync

As a usuario recién invitado,
I want instrucciones claras para instalar y configurar MAL-Sync contra AniList,
So that puedo empezar a generar datos que AnimeTracker pueda mostrar.

**Acceptance Criteria:**

**Given** un usuario recién invitado (en la Whitelist) sin Snapshots todavía
**When** navega a "Configurar MAL-Sync" desde la Nav
**Then** ve una página con instrucciones para instalar y configurar MAL-Sync contra AniList (FR-10)

**Given** esa página
**When** el usuario aún no tiene ningún Snapshot
**Then** es accesible igual, sin requerir datos previos (FR-10)

### Story 6.2: Banner global de onboarding

As a usuario recién invitado sin datos todavía,
I want ver una guía en vez de una pantalla vacía confusa en cualquier vista,
So that entiendo qué me falta hacer para empezar a usar AnimeTracker.

**Acceptance Criteria:**

**Given** un usuario sin ningún Snapshot registrado
**When** navega Hoy, Por Estado o Tendencias
**Then** ve el Banner de Onboarding de MAL-Sync (visible globalmente en las 3 superficies), reemplazando el Empty State genérico de cada una (UX-DR8)

**Given** el banner
**When** se muestra
**Then** enlaza a la página "Configurar MAL-Sync" (Story 6.1)

**Given** el usuario completa su primera sincronización exitosa (login-sync o job)
**When** vuelve a cualquiera de las 3 superficies
**Then** el banner desaparece definitivamente, reemplazado por el contenido real o el Empty State normal de cada superficie (UX-DR8)

**Given** el copy del banner y de los empty states
**When** se redacta
**Then** sigue los patrones de microcopy del PRD/EXPERIENCE.md (frases cortas, sin exclamaciones ni emojis, sin tono motivacional) (UX-DR13)

