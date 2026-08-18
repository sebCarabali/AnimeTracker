# Epic 1 Context: Autenticación y Control de Acceso

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Permitir que un usuario invitado inicie sesión en AnimeTracker usando exclusivamente su cuenta de AniList vía OAuth, sin que la app maneje contraseñas propias, y restringir el acceso solo a usuarios presentes en una Whitelist de Invitación gestionada manualmente. Un usuario no invitado que completa el OAuth debe ver un mensaje explícito de acceso denegado, nunca un error genérico. Este epic es el punto de entrada de toda la aplicación: ninguna sesión ni fila de usuario puede crearse sin pasar primero por el gate de whitelist.

## Stories

- Story 1.1: Iniciar sesión con AniList (OAuth)
- Story 1.2: Gate de Whitelist de Invitación

## Requirements & Constraints

- El login es 100% delegado a OAuth de AniList: AnimeTracker nunca pide ni almacena una contraseña propia.
- Un login exitoso solo abre sesión si el usuario está en la Whitelist; si no, se redirige a una pantalla de Acceso Denegado dedicada, con mensaje explícito (no un error genérico), sin botón de reintento automático.
- Agregar un usuario a la Whitelist después de un intento fallido debe permitirle completar el acceso en su siguiente intento, sin re-registro.
- Una falla transitoria de OAuth (usuario cancela, error de red) antes de completar el login es un caso distinto de "no está en whitelist": se vuelve a la pantalla de Login con mensaje de error y botón de reintento; no se crea sesión.
- El token OAuth recibido se maneja exclusivamente server-side desde el momento del callback; nunca se expone en cookie, HTML o JS del cliente (NFR-3).
- Único rol de usuario autenticado; no existe rol admin ni UI de administración de whitelist en V1 — la gestión es manual, directa en DB.
- AnimeTracker es de solo lectura hacia AniList: la única escritura propia de la app relacionada con este epic es la sesión (login/logout).

## Technical Decisions

- Package `auth`: contiene el callback OAuth, el gate de whitelist y `findOrCreate(AppUser)`, y la gestión de sesión. Es el único owner de la creación de `AppUser` — ninguna otra feature (incluido `sync`) crea esa fila, solo la lee o actualiza.
- Orden estricto tras el callback OAuth: (1) se consulta `WhitelistedUser` por el id de AniList del usuario, sin necesitar aún un `AppUser`; (2) si no está en la whitelist, redirect a Acceso Denegado sin crear sesión ni fila; (3) si está, se ejecuta `findOrCreate(AppUser)` y recién ahí se abre la sesión. Este orden previene condiciones de carrera en el primer login y evita crear estado para usuarios no habilitados.
- Autenticación implementada vía Spring Security + OAuth2 client. Un token inválido o expirado debe disparar el camino de degradación (ver Epic 2 / FR-9), nunca una excepción sin manejar.
- Stack relevante: Java 25, Spring Boot 4.1.x (Spring Framework 7, Jakarta EE 11), Thymeleaf 3.1.x server-rendered, Tailwind CSS 4.3.x. Config y secrets (client id/secret de AniList) vía `application.yml` + variables de entorno, nunca committeados.
- Design tokens (colores light/dark, tipografía Sora/Inter, spacing 4px, radios) se configuran como tokens de Tailwind, oscuro-primero, y deben usarse ya en la pantalla de Login por ser la primera superficie renderizada.

## UX & Interaction Patterns

- Pantalla de Login: raíz de la app sin sesión; único CTA es "Iniciar sesión con AniList" (Button Primary), que inicia el redirect OAuth.
- Pantalla de Acceso Denegado: página dedicada y visualmente distinta de Login, con mensaje explícito de que el usuario no está invitado; sin reintento automático ni botón de reintento (a diferencia de la falla transitoria de OAuth).
- Falla transitoria de OAuth: se resuelve en la misma pantalla de Login, con copy tipo "No pudimos completar el login con AniList — probá de nuevo." y botón de reintento (Button Primary).
- Button Primary no tiene estado disabled en estas pantallas en V1 (no hay validación de formulario que lo justifique aquí; el disabled-durante-request de Button Primary aplica al reintento de sync de otro epic, no a este).
- Ambas pantallas (Login y Acceso Denegado) deben usar los design tokens oscuro-primero (fondo `surface-base-dark`, texto `ink-primary-dark`, acento `accent-dark`, tipografía Sora para headings / Inter para texto).

## Cross-Story Dependencies

- Story 1.2 depende del resultado del callback OAuth de Story 1.1: el gate de whitelist se evalúa inmediatamente después de que el OAuth se completa, antes de que exista cualquier sesión o fila `AppUser`.
- La sesión abierta al final de Story 1.2 es el disparador de Epic 2 (Story 2.1: sincronización forzada en login), que se ejecuta de forma síncrona antes del primer render post-login. Epic 1 no implementa esa sincronización, solo produce la sesión que la dispara.
- El chrome global autenticado (Nav, Theme Toggle) que consumen todas las superficies posteriores se construye en Epic 3, no en este epic; Login y Acceso Denegado son pantallas no autenticadas y no lo usan.
