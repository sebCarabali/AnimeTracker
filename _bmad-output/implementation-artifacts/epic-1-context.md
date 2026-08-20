# Epic 1 Context: Autenticación y Control de Acceso

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Un usuario invitado puede iniciar sesión con su cuenta de AniList vía OAuth, sin crear ni recordar una contraseña propia. El acceso está cerrado por invitación: solo un usuario de AniList presente en la Whitelist puede completar el login; alguien no invitado ve un mensaje explícito de acceso denegado en vez de un error genérico. Esta épica es el punto de entrada de toda la app — ninguna otra superficie es alcanzable sin pasar por este flujo.

## Stories

- Story 1.1: Iniciar sesión con AniList (OAuth)
- Story 1.2: Gate de Whitelist de Invitación

## Requirements & Constraints

- El login redirige al flujo OAuth oficial de AniList; no se pide ni se almacena contraseña propia. Un login exitoso abre una sesión válida.
- Solo un usuario de AniList presente en la Whitelist de Invitación puede completar el login y acceder a las vistas de la app. Un usuario no invitado que completa el OAuth recibe un mensaje explícito de acceso denegado, nunca un error genérico.
- Agregar un usuario a la Whitelist habilita su acceso de inmediato, sin que necesite re-registrarse ni repetir ningún paso adicional.
- Gestión de la Whitelist es manual (edición directa en base de datos) en V1 — no existe UI de administración; deferred hasta que el volumen lo justifique.
- El token OAuth de AniList se maneja exclusivamente server-side, atado a la sesión — nunca expuesto en cookie, HTML o JS del cliente (requisito de seguridad transversal a toda la épica).
- Una falla transitoria de OAuth (usuario cancela, error de red) antes de completar el login muestra la pantalla de Login con mensaje de error y botón de reintento; no crea ninguna sesión.
- La pantalla de Acceso Denegado es una página dedicada, distinta de Login, sin botón de reintento automático (no es una falla transitoria, es un rechazo de Whitelist).
- El botón primario (login) no tiene estado disabled salvo en el reintento de sincronización (fuera de alcance de esta épica); los botones secundarios siempre navegan, nunca disparan una acción destructiva.
- La pantalla de Login usa los design tokens (colores light/dark, tipografía Sora/Inter, spacing de 4px, radios) configurados en Tailwind, oscuro-primero.

## Technical Decisions

- Autenticación vía Spring Security + OAuth2 client. Toda comunicación con AniList pasa exclusivamente por `integration.anilist` (capa anti-corrupción); ningún Controller/Thymeleaf la invoca directo.
- El gate de Whitelist ocurre inmediatamente después del callback OAuth, antes de crear sesión o fila `AppUser`: se consulta `WhitelistedUser` por el id de AniList del usuario (no requiere `AppUser` previo). Si no está, redirige a Acceso Denegado sin crear sesión ni fila. Si está, se procede a abrir sesión.
- `auth` es el único owner de `findOrCreate(AppUser)` — se crea o recupera ahí, antes de abrir la sesión. Ningún otro package (en particular `sync`) crea un `AppUser`; solo lee/actualiza uno ya existente.
- Un token inválido/expirado dispara el camino de degradación definido para fallas de sincronización (no aplica dentro del login mismo, pero el manejo de token server-side es la misma pieza que reutiliza esa ruta después), nunca una excepción sin manejar.
- Estructura relevante: package `auth/` contiene el callback OAuth, el gate de whitelist, `findOrCreate(AppUser)` y el manejo de sesión; `integration/anilist/` es la única puerta de entrada al API de AniList.
- Entidades: `AppUser` (con `anilist_user_id`, `theme_preference`) y `WhitelistedUser`. Cada entidad local mantiene su propia PK más una columna indexada separada para el id remoto de AniList — nunca conflar ambos ids. Timestamps siempre `Instant` UTC.
- Config/secrets (client id/secret de AniList) vía `application.yml` + variables de entorno, nunca committeados.

## UX & Interaction Patterns

- Login y Acceso Denegado son pantallas distintas: falla transitoria de OAuth (con reintento) vs. rechazo de Whitelist (página dedicada, sin reintento automático).
- Microcopy de referencia: acceso denegado se comunica como "Tu cuenta de AniList no está habilitada todavía." — nunca "Acceso denegado" a secas. Frases cortas, sin exclamaciones ni emojis, sin tono motivacional.
- La pantalla de Login es la raíz de la app sin sesión activa; tras Acceso Denegado, el usuario puede volver a intentar el login apenas sea agregado a la Whitelist, sin pasos adicionales.
- Layout de Login/Acceso Denegado sigue los tokens de `DESIGN.md`: fondo oscuro por defecto, botón primario con fill de acento, sin sombras como jerarquía visual.

## Cross-Story Dependencies

- Story 1.2 (Whitelist gate) depende directamente del callback OAuth completado en Story 1.1: el gate se evalúa entre el callback y la creación de sesión/`AppUser`, no después.
- Epic 2 (Story 2.1, sync forzado en login) depende de que la sesión y la fila `AppUser` ya existan — se dispara inmediatamente después de que Story 1.2 abre la sesión, antes del primer render post-login.
- El resto de las épicas (Hoy, Por Estado, Tendencias, Onboarding) asumen una sesión ya autenticada; ninguna de sus superficies es alcanzable sin pasar por el flujo de esta épica.
