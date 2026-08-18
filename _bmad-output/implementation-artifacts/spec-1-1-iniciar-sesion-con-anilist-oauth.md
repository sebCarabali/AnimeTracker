---
title: 'Story 1.1 — Iniciar sesión con AniList (OAuth)'
type: 'feature'
created: '2026-08-18'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'NO_VCS'
context: [_bmad-output/implementation-artifacts/epic-1-context.md, _bmad-output/planning-artifacts/ux-designs/ux-AnimeList-2026-08-13/mockups/login.html]
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** El repo está vacío — no existe todavía proyecto Java. Un usuario invitado necesita poder iniciar sesión en AnimeTracker usando su cuenta de AniList vía OAuth, sin que la app pida ni almacene contraseña propia.

**Approach:** Bootstrapear el monolito Spring Boot (Gradle Kotlin DSL, Java 25) con Spring Security OAuth2 client configurado contra AniList (proveedor custom — AniList no es un proveedor preconfigurado de Spring Security ni expone un user-info REST estándar; la identidad del Viewer se obtiene vía GraphQL a través de `integration.anilist`). El callback deja el token exclusivamente server-side y termina en un punto de extensión explícito y marcado (sin implementarlo) donde la Story 1.2 conectará el gate de whitelist y `findOrCreate(AppUser)` — 1.1 no crea sesión de aplicación real ni fila `AppUser`. El toolchain de Tailwind CSS y la documentación de setup quedan diferidos (ver Spec Change Log / deferred-work.md); esta story replica los tokens de diseño del mockup con CSS embebido mínimo.

## Boundaries & Constraints

**Always:**
- El token OAuth se maneja exclusivamente server-side desde el callback en adelante; nunca se expone en cookie, HTML o JS de cliente (AD-4/NFR-3).
- La pantalla de Login replica visualmente los tokens de diseño oscuro-primero del mockup `login.html` (colores, tipografía Sora/Inter, spacing 4px, radios) — UX-DR1. Alcanza con CSS embebido copiando esos valores; no se requiere el pipeline de Tailwind en esta story.
- Client id/secret de AniList y cualquier config sensible van vía `application.yml` + variables de entorno, nunca hardcodeados ni commiteados.
- El punto donde Story 1.2 debe insertar el gate de whitelist queda marcado explícitamente en el código (comentario/TODO en la clase correspondiente), sin lógica de whitelist ni acceso a `AppUser`/DB en esta story.
- Cualquier llamada a AniList (incluida la resolución de identidad del Viewer) pasa por `integration.anilist`, nunca directo desde `auth` (AD-1, AD-9).
- El estado de error/reintento de Login se anuncia a lectores de pantalla vía `aria-live="polite"` (mismo patrón ya usado por el Stale Banner, UX-DR6), aunque ningún UX-DR lo exija explícitamente para esta pantalla.
- El comportamiento de seguridad relevante (rutas públicas, redirect a `/` tras login exitoso, redirect a `/login?error` tras fallo) se verifica con una prueba automatizada que simula el login con el soporte de test de Spring Security (`oauth2Login()`), sin depender de credenciales reales de AniList.

**Ask First:**
- Si no hay Gradle ni acceso a red para generar el wrapper (`gradle wrapper`) en este entorno, preguntar antes de commitear un wrapper roto o elegir un enfoque alternativo.
- Si el intercambio de código por token contra AniList falla porque el endpoint exige `application/json` en vez del `application/x-www-form-urlencoded` que envía el cliente por defecto de Spring, preguntar antes de introducir un `RestClient` custom de intercambio de token (no es un cambio trivial de config).

**Never:**
- No implementar el gate de whitelist real, `findOrCreate(AppUser)`, ni crear la entidad `AppUser`/JPA/Flyway/PostgreSQL — eso es Story 1.2 y Epic 2.
- No implementar Theme Toggle ni el sistema completo light/dark (Epic 3) — solo los tokens oscuro-primero que necesita esta pantalla.
- No implementar la pantalla de Acceso Denegado (es Story 1.2) ni Nav/chrome autenticado (Epic 3).
- No configurar el pipeline de build de Tailwind ni escribir README/docs de setup — diferido (ver deferred-work.md).
- No commitear client-id/secret reales de AniList.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Happy path | Usuario click "Continuar con AniList" en `/login` | Redirect a `https://anilist.co/api/v2/oauth/authorize`; tras consentir, callback intercambia el code por token server-side y llega al punto de extensión de 1.2 | N/A |
| Falla transitoria de OAuth | Usuario cancela el consentimiento o hay error de red antes de completar el login | Redirect a `/login?error`, se re-renderiza Login con mensaje "No pudimos completar el inicio de sesión con AniList. Inténtalo de nuevo." (anunciado vía `aria-live="polite"`) y botón de reintento (Button Primary) | No se crea sesión ni se persiste nada |
| Credenciales AniList placeholder | `ANILIST_CLIENT_ID`/`ANILIST_CLIENT_SECRET` sin configurar con valores reales | AniList devuelve error de client inválido en su propia pantalla | La app no debe crashear; es un estado esperado hasta que el humano cargue credenciales reales |

</frozen-after-approval>

## Code Map

- `settings.gradle.kts`, `build.gradle.kts` -- nuevos, bootstrap del proyecto (Gradle Kotlin DSL, Java 25 toolchain, plugin Spring Boot 4.1.x, `testImplementation("org.springframework.security:spring-security-test")`)
- `src/main/java/com/animetracker/AnimetrackerApplication.java` -- nuevo, clase `@SpringBootApplication`
- `src/main/java/com/animetracker/config/SecurityConfig.java` -- nuevo, `SecurityFilterChain`: permitAll en `/login*` y estáticos, `oauth2Login` con `loginPage("/login")`, `failureUrl("/login?error")`, `userInfoEndpoint` custom, `successHandler` custom
- `src/main/java/com/animetracker/integration/anilist/AniListViewerClient.java` -- nuevo, POST GraphQL a `https://graphql.anilist.co` (`{ Viewer { id name } }`) con el access token, vía Spring `RestClient`
- `src/main/java/com/animetracker/integration/anilist/AniListViewer.java` -- nuevo, record `(Long id, String name)`
- `src/main/java/com/animetracker/auth/AniListOAuth2UserService.java` -- nuevo, implementa `OAuth2UserService<OAuth2UserRequest, OAuth2User>`, delega en `AniListViewerClient`, construye un `DefaultOAuth2User` (atributo de nombre: id de AniList como string)
- `src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java` -- nuevo, `AuthenticationSuccessHandler`; comentario explícito marcando el punto de extensión de Story 1.2 (whitelist gate + `findOrCreate(AppUser)`); por ahora solo redirige a `/`
- `src/main/java/com/animetracker/auth/LoginController.java` -- nuevo, `GET /login` renderiza `auth/login.html`, pasa flag `error` desde query param
- `src/main/resources/templates/auth/login.html` -- nuevo, implementa la sección "Login" de `mockups/login.html` (estado default + estado de error/reintento, este último con `aria-live="polite"`) con `<style>` embebido copiando los tokens de color/tipografía/spacing del mockup 1:1; CTA "Continuar con AniList" -> `/oauth2/authorization/anilist`
- `src/main/resources/application.yml` -- nuevo, `spring.security.oauth2.client.registration.anilist` (authorization-uri, token-uri, redirect-uri, client-authentication-method: post, client-id/secret vía `${ANILIST_CLIENT_ID}`/`${ANILIST_CLIENT_SECRET}`)
- `.gitignore` -- nuevo, estándar Java/Gradle/IDE; se inicializa el repo git local (no existe `.git` todavía)
- `src/test/java/com/animetracker/auth/OAuthLoginFlowTest.java` -- nuevo, test con `spring-security-test` (`oauth2Login()`) que simula un Viewer autenticado y verifica el redirect del success handler a `/`, el redirect de fallo a `/login?error`, y el permitAll de rutas públicas -- hace verificable el AC 2 sin depender de credenciales reales de AniList

## Tasks & Acceptance

**Execution:**
- [x] `settings.gradle.kts` / `build.gradle.kts` -- crear proyecto Gradle Kotlin DSL, Java 25 toolchain, dependencias `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-oauth2-client` -- fundamento del monolito, sin esto no hay nada que correr
- [x] Generar Gradle Wrapper -- reproducibilidad del build sin depender de una instalación global de Gradle
- [x] `AnimetrackerApplication.java` -- clase main -- punto de entrada Spring Boot
- [x] `application.yml` -- registrar el client OAuth2 `anilist` (authorization-uri, token-uri, redirect-uri, client-authentication-method post) con client-id/secret placeholder vía env vars -- AniList no es un proveedor preconfigurado de Spring Security
- [x] `integration/anilist/AniListViewer.java` + `AniListViewerClient.java` -- resolver identidad del Viewer autenticado vía GraphQL -- AniList no expone un user-info REST estándar; debe pasar por la capa anti-corrupción (AD-1)
- [x] `auth/AniListOAuth2UserService.java` -- puente entre Spring Security y `integration.anilist` -- reemplaza el `DefaultOAuth2UserService` que asume REST user-info
- [x] `auth/OAuthLoginSuccessHandler.java` -- handler de éxito con el punto de extensión de Story 1.2 explícitamente comentado -- evita implementar prematuramente el whitelist gate
- [x] `config/SecurityConfig.java` -- wiring de `oauth2Login`, `loginPage`, `failureUrl`, permitAll de rutas públicas -- única fuente de verdad de la config de seguridad
- [x] `auth/LoginController.java` + `templates/auth/login.html` -- pantalla de Login (default + error/reintento con `aria-live="polite"`) fiel al mockup, con CSS embebido -- UX-DR1, AC de falla transitoria, accesibilidad del anuncio de error
- [x] `.gitignore` + inicializar git local -- higiene básica de repo antes de empezar a versionar código
- [x] `test/auth/OAuthLoginFlowTest.java` -- test con `oauth2Login()` de `spring-security-test` cubriendo los 3 escenarios de la I/O Matrix -- sin esto, el AC 2 (handoff a Story 1.2 sin sesión real) no es verificable con credenciales placeholder

**Acceptance Criteria:**
- Given un usuario no autenticado en `/login`, when hace click en "Continuar con AniList", then es redirigido al flujo OAuth oficial de AniList sin que se le pida contraseña propia
- Given el usuario completa el OAuth exitosamente, when AniList redirige al callback, then el access token se maneja exclusivamente server-side y el flujo llega al punto de extensión marcado para Story 1.2, sin crear sesión de aplicación ni fila `AppUser`
- Given una falla transitoria de OAuth, when vuelve al callback o la redirección falla, then ve `/login` con mensaje de error y botón de reintento, y no se crea sesión
- Given la pantalla de Login, when se renderiza, then visualmente coincide con los tokens de diseño oscuro-primero del mockup (colores, Sora/Inter, spacing 4px, radios)
- Given el proyecto recién bootstrapeado, when se ejecuta `./gradlew build`, then compila sin errores

## Spec Change Log

- 2026-08-18 — Token count excedía 1600 (~2200-2900 estimado). Split solicitado por el usuario ([S]). Se carvearon dos goals secundarios a `deferred-work.md`: (1) pipeline de build de Tailwind CSS 4.x, reemplazado en esta spec por CSS embebido copiando los tokens del mockup 1:1; (2) documentación de desarrollo (README). Ningún AC de OAuth se vio afectado por el recorte.
- 2026-08-18 — Revisión en bmad-party-mode: (1) se agregó una prueba automatizada (`OAuthLoginFlowTest`, `spring-security-test` + `oauth2Login()`) porque el AC 2 era imposible de verificar manualmente con credenciales placeholder; (2) se agregó `aria-live="polite"` al estado de error de Login por consistencia con el patrón ya usado en el Stale Banner (UX-DR6), aunque ningún UX-DR lo exige explícitamente para esta pantalla.
- 2026-08-18 — Implementación: Spring Boot 4.1.0 (GA real disponible en Maven Central) modularizó los test slices — `@AutoConfigureMockMvc` ya no vive en `spring-boot-test-autoconfigure` (ese artefacto quedó reducido a soporte genérico + `JsonTest`) sino en un artefacto nuevo `org.springframework.boot:spring-boot-webmvc-test`, bajo el paquete `org.springframework.boot.webmvc.test.autoconfigure`. Se agregó esa dependencia `testImplementation` explícita en `build.gradle.kts` además de `spring-boot-starter-test`. Story 1.2 (y cualquier otro test que use MockMvc) debe tener esto en cuenta.
- 2026-08-18 — Fix post-review (Step 4, 3 capas de revisión coincidieron en el hallazgo 1): (1) **Excepción sin manejar bypasseaba `/login?error`** — `AniListOAuth2UserService.loadUser` llamaba a `AniListViewerClient.fetchViewer` sin capturar nada; un `IllegalStateException` (Viewer inválido en la respuesta GraphQL) o un `RestClientException` (4xx/5xx de AniList) no son `AuthenticationException`, así que Spring Security no los enrutaba al `failureUrl` y el request terminaba en un 500 sin manejar — violando AD-4 y la fila "Falla transitoria de OAuth" de la I/O Matrix para el caso específico de que AniList falle *después* del intercambio de token, durante la resolución del Viewer. Fix: `loadUser` ahora envuelve la llamada en try/catch(RuntimeException) y relanza como `OAuth2AuthenticationException`, y trata `viewer.id() == null` como el mismo tipo de falla en vez de convertirlo silenciosamente en el literal `"null"`. Cubierto por el nuevo `AniListOAuth2UserServiceTest` (3 tests: viewer client lanza excepción, id nulo, y el happy path de regresión). No se agregó un test end-to-end vía `MockMvc`/`oauth2Login()` para este caso puntual porque `oauth2Login()` construye la autenticación directamente y nunca ejecuta `loadUser`, y simular el flujo real de principio a fin exigiría stubbear el intercambio de token interno de Spring Security (un `OAuth2AccessTokenResponseClient` custom o un WireMock) — se juzgó no práctico para este parche; el unit test sobre `AniListOAuth2UserService` es la cobertura elegida. Estado-malo-que-evita: un usuario cuyo Viewer falla en resolverse (AniList caído, 5xx, respuesta GraphQL malformada) ya no ve un error 500 genérico sin manejar, ve la pantalla de Login con el mensaje de reintento. (2) **`RestClient` de `AniListViewerClient` sin timeout** — si `graphql.anilist.co` no respondía o respondía muy lento, el thread del login quedaba bloqueado indefinidamente sin límite. Fix: se configuró un `ClientHttpRequestFactory` con `HttpClientSettings` (connect timeout 5s / read timeout 10s) vía `ClientHttpRequestFactoryBuilder.detect()` (paquete `org.springframework.boot.http.client`, artefacto nuevo `spring-boot-http-client` agregado a `build.gradle.kts`); un timeout ahora es un `RestClientException` más que cae en el mismo catch del fix (1) y termina en `/login?error`. Estado-malo-que-evita: un request de login colgado indefinidamente por una AniList lenta o caída, agotando threads del servidor.

## Design Notes

- **AniList no es un proveedor OAuth2 estándar de Spring Security**: no hay `spring.security.oauth2.client.provider.anilist` preconfigurado, y no expone un `user-info-uri` REST — la identidad del Viewer se obtiene con una query GraphQL (`{ Viewer { id name } }`) contra `https://graphql.anilist.co` usando el access token como Bearer. Por eso se necesita un `OAuth2UserService` custom en vez del `DefaultOAuth2UserService`.
- **Posible mismatch de content-type en el token exchange**: el endpoint de token de AniList (`https://anilist.co/api/v2/oauth/token`) ha requerido históricamente `Content-Type: application/json` en el body, mientras que el cliente de intercambio de token por defecto de Spring Security envía `application/x-www-form-urlencoded`. Verificar en implementación; si falla con 400, ver "Ask First".
- **Punto de extensión para Story 1.2**: `OAuthLoginSuccessHandler` debe dejar un comentario explícito indicando que ahí es donde se debe insertar la consulta a `WhitelistedUser` y `findOrCreate(AppUser)` antes de considerar la sesión válida (AD-5). No crear una interfaz/abstracción para esto — es una sola clase que 1.2 va a editar directamente.
- **CSS embebido temporal**: el `<style>` de `login.html` (template) debe copiar literalmente las variables CSS del mockup (`--surface-*`, `--ink-*`, `--accent*`, `--border-hairline`, `--danger`, fuentes Sora/Inter). Cuando se resuelva el deferred-work de Tailwind, ese bloque se reemplaza por clases utilitarias — no se diseña una abstracción propia mientras tanto.

## Verification

**Commands:**
- `./gradlew build` -- expected: `BUILD SUCCESSFUL`, incluyendo `OAuthLoginFlowTest` en verde
- `./gradlew test --tests "*OAuthLoginFlowTest"` -- expected: los 3 escenarios de la I/O Matrix pasan sin red real a AniList

**Manual checks (if no CLI):**
- Levantar la app (`./gradlew bootRun`) y visitar `http://localhost:8080/login`: debe verse igual a la sección "Login" del mockup, con el CTA visible
- Click en el CTA: debe redirigir hacia `anilist.co` (fallará ahí mismo con credenciales placeholder — comportamiento esperado hasta cargar credenciales reales; el AC 2 se verifica por el test automatizado, no manualmente)
- Visitar `http://localhost:8080/login?error=1` manualmente: debe verse el estado de error/reintento con el copy correcto y anunciarse al activar un lector de pantalla

## Suggested Review Order

**Punto de entrada — configuración de seguridad**

- Única fuente de verdad de qué rutas son públicas y cómo se conecta el login OAuth2 con AniList.
  [`SecurityConfig.java:30`](../../src/main/java/com/animetracker/config/SecurityConfig.java#L30)

**Resolución de identidad de AniList (capa anti-corrupción + patch de Step 4)**

- Envuelve cualquier falla al resolver el Viewer (GraphQL inválido, 4xx/5xx, timeout) como `OAuth2AuthenticationException` en vez de dejarla propagar como 500 sin manejar.
  [`AniListOAuth2UserService.java:41`](../../src/main/java/com/animetracker/auth/AniListOAuth2UserService.java#L41)

- `RestClient` con timeout explícito de conexión/lectura hacia `graphql.anilist.co` — el otro fix del patch de review.
  [`AniListViewerClient.java:36`](../../src/main/java/com/animetracker/integration/anilist/AniListViewerClient.java#L36)

- Record inmutable que aísla al resto del código del shape crudo de la respuesta GraphQL de AniList.
  [`AniListViewer.java:8`](../../src/main/java/com/animetracker/integration/anilist/AniListViewer.java#L8)

**Handoff a Story 1.2**

- Punto de extensión explícito y comentado donde 1.2 debe insertar el gate de whitelist; por ahora solo redirige a `/`.
  [`OAuthLoginSuccessHandler.java:33`](../../src/main/java/com/animetracker/auth/OAuthLoginSuccessHandler.java#L33)

**Pantalla de Login**

- Controller mínimo: renderiza el estado default y el de error/reintento según el query param.
  [`LoginController.java:11`](../../src/main/java/com/animetracker/auth/LoginController.java#L11)

- Estado de error con `aria-live="polite"`, CSS embebido temporal copiando los tokens del mockup 1:1.
  [`login.html:96`](../../src/main/resources/templates/auth/login.html#L96)

**Config y build**

- Registro OAuth2 custom `anilist` — AniList no es un proveedor preconfigurado de Spring Security.
  [`application.yml:8`](../../src/main/resources/application.yml#L8)

- Dependencia agregada por el patch de timeout (`spring-boot-http-client`, no viene transitiva con `starter-web`).
  [`build.gradle.kts:25`](../../build.gradle.kts#L25)

**Tests**

- Cubre las 3 filas de la I/O Matrix vía `MockMvc` + `oauth2Login()`, sin red real a AniList.
  [`OAuthLoginFlowTest.java:34`](../../src/test/java/com/animetracker/auth/OAuthLoginFlowTest.java#L34)

- Cubre el patch de Step 4: falla de red/GraphQL → `OAuth2AuthenticationException`, id ausente nunca se vuelve el literal `"null"`.
  [`AniListOAuth2UserServiceTest.java:47`](../../src/test/java/com/animetracker/auth/AniListOAuth2UserServiceTest.java#L47)
