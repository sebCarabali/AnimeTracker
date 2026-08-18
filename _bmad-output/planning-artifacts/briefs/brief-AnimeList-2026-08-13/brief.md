---
title: AnimeTracker
status: draft
created: 2026-08-13
updated: 2026-08-13
---

# Product Brief: AnimeTracker

## Resumen Ejecutivo

AnimeTracker es un dashboard web personal que responde rápido una sola pregunta: *"¿qué estoy mirando ahora y qué episodio sigue?"*. No es un tracker de anime desde cero — se apoya en dos piezas que ya existen y funcionan: **MAL-Sync** (extensión de navegador open-source que detecta reproducción en JKAnime y AnimeFLV y actualiza el progreso automáticamente) y **AniList** (plataforma de tracking con API GraphQL abierta, que actúa como fuente de verdad del progreso y el catálogo). AnimeTracker es la capa de visualización sobre esos datos: un dashboard "hoy / seguí viendo" con la información resumida de forma más directa que la lista genérica de AniList, más vistas por estado y tendencias históricas (episodios vistos por semana) que AniList no ofrece de forma nativa.

Es un proyecto de uso invitado (no público) pensado para escalar de forma modesta: unos 100 usuarios el primer año, hasta 1000 como techo. Backend en Java Spring Boot con base de datos propia para persistir snapshots históricos, ya que AniList solo expone el estado actual, no una serie de tiempo.

## El Problema

Quien mira anime en sitios de streaming como JKAnime o AnimeFLV y lleva su progreso en AniList tiene el dato, pero no una vista pensada para la pregunta del día a día: "¿qué tengo pendiente de ver ahora mismo?". La lista de AniList está organizada como catálogo (todo lo que viste, todo lo que planeás ver), no como un feed de "seguí acá". Tampoco guarda historial de actividad en el tiempo — no hay forma nativa de ver "cuántos episodios miré esta semana" o detectar una racha o una caída de actividad.

## La Solución

AnimeTracker no reconstruye la detección de reproducción ni el catálogo de animes — ambos problemas ya están resueltos por herramientas existentes, y reconstruirlos sería esfuerzo desperdiciado:

- **Detección de reproducción:** la resuelve **MAL-Sync**, instalada tal cual (sin modificar) por cada usuario en su navegador. Ya soporta JKAnime y AnimeFLV de forma nativa y escribe el progreso directamente en la cuenta de AniList del usuario.
- **Catálogo y estado de progreso:** vive en **AniList**, vía su API GraphQL pública, con OAuth por usuario.
- **Lo que construye AnimeTracker:** una app web (backend Java Spring Boot + base de datos propia) que autentica al usuario contra AniList, lee su lista de tracking, y la presenta como:
  - Un dashboard "hoy / seguí viendo" — foco en lo que está en curso y el próximo episodio pendiente.
  - Vistas organizadas por estado (viendo, pendiente, completado, abandonado — heredados de AniList).
  - Tendencias históricas (ej. episodios vistos por semana), construidas a partir de snapshots periódicos que AnimeTracker guarda en su propia base de datos, porque AniList no expone esa serie de tiempo.

AnimeTracker es de **solo lectura** frente a AniList: no escribe ni modifica datos allí. Toda edición (cambiar estado, calificar, notas) se sigue haciendo en AniList o vía MAL-Sync mientras se mira streaming.

## Qué Lo Hace Diferente

Honestamente, no hay una ventaja técnica defendible ("moat"): MAL-Sync y AniList son mejores y más maduros que cualquier cosa que se construya para este proyecto en esas dos áreas, y ese es justamente el motivo de apoyarse en ellos en vez de competir. La diferenciación de AnimeTracker es de **experiencia**, no de datos ni de detección: un dashboard enfocado en "qué sigue" en vez de un catálogo completo, y una vista de tendencias en el tiempo que AniList no tiene. Es una app deliberadamente chica y enfocada, no un tracker alternativo.

## A Quién Sirve

Usuarios de AniList que miran anime en JKAnime o AnimeFLV y quieren una vista diaria más directa que la lista completa de AniList — inicialmente un grupo cerrado por invitación (no registro público), pensado para crecer a un ritmo modesto: ~100 usuarios en el primer año, con un techo de escala de referencia de 1000.

## Requisitos Funcionales Clave (V1)

- **Autenticación:** login vía OAuth de AniList (no hay sistema de usuarios/contraseñas propio). Acceso solo por invitación — se necesita algún mecanismo de invitación/whitelist para admitir nuevos usuarios.
- **Dashboard "hoy / seguí viendo":** vista principal, foco en animes con progreso activo y el próximo episodio pendiente por título.
- **Vistas por estado:** listas filtradas según el estado que AniList reporta (viendo, pendiente, completado, abandonado, y el resto de los estados estándar de AniList).
- **Tendencias históricas:** vista de actividad en el tiempo (ej. episodios vistos por semana/mes), calculada a partir de snapshots que AnimeTracker guarda periódicamente en su propia base de datos.
- **Sincronización de datos:** proceso (job periódico o al login) que consulta la API de AniList por usuario, actualiza el estado mostrado y registra un snapshot para el histórico.

## Integraciones y Restricciones Técnicas

- **MAL-Sync** es una dependencia externa no controlada por este proyecto. Si un usuario no la instala o no la configura contra AniList, AnimeTracker no tiene forma de detectar reproducción — es un requisito de setup del usuario, no algo que la app resuelva.
- **AniList API (GraphQL, OAuth):** fuente de verdad para catálogo y progreso. AnimeTracker consume, nunca escribe. Sujeta a rate limits de AniList — con la escala proyectada (hasta 1000 usuarios) es necesario cachear/limitar la frecuencia de consultas en vez de pedir en vivo en cada carga de página.
- **Backend:** Java Spring Boot, desplegado en un hosting/servidor que lo soporte (restricción de stack ya definida, no una preferencia abierta).
- **Base de datos propia:** no es la fuente de verdad de catálogo o progreso (eso es AniList) — su rol es (a) cache para no depender de AniList en cada request y (b) almacenamiento de snapshots históricos para las tendencias, algo que AniList no ofrece.
- **Dependencia de terceros:** el producto entero depende de que AniList y MAL-Sync sigan existiendo y manteniéndose. Esto es una limitación aceptada conscientemente (ver "Qué lo hace diferente"), no un descuido.

## Requisitos No Funcionales y Casos de Borde

- **Escala:** diseño para ~100 usuarios el primer año, con margen hasta 1000 — no hace falta arquitectura para escala masiva, pero sí evitar que cada carga de dashboard dispare una consulta en vivo a AniList por usuario (riesgo de rate-limiting con cientos de usuarios concurrentes).
- **Staleness de datos:** al ser solo-lectura con snapshots periódicos, el dashboard no es en tiempo real estricto — hay que definir la frecuencia de sincronización aceptable (ej. cada N minutos, o al login) como parte del diseño técnico.
- **Disponibilidad de AniList:** si la API de AniList está caída o el usuario revoca el acceso OAuth, el dashboard debe degradar de forma clara (mostrar el último snapshot conocido con su fecha, no romper).
- **Registro por invitación:** al no ser autoregistro público, se necesita algún mecanismo de invitación (código, alta manual, whitelist) — sin definir todavía el detalle de cómo se gestiona.

## Alcance V1

**Dentro de alcance:** lo descrito en "Requisitos Funcionales Clave" arriba — login por invitación, dashboard "hoy / seguí viendo", vistas por estado, tendencias históricas, y la documentación de setup de MAL-Sync.

**Explícitamente fuera de alcance (V1):**
- Construir una extensión de navegador propia — se usa MAL-Sync tal cual.
- Escritura/edición de datos desde AnimeTracker hacia AniList (cambiar estado, calificar, notas) — se sigue haciendo en AniList o MAL-Sync.
- Soporte para otras plataformas de tracking externas (MyAnimeList, Kitsu, Simkl) — solo AniList.
- Soporte para sitios de streaming más allá de JKAnime/AnimeFLV — esto lo resuelve MAL-Sync, está fuera del control de este proyecto.
- Comparar o cruzar listas entre distintos usuarios de AnimeTracker.
- Aplicación móvil nativa.
- Registro público / autoregistro abierto.

## Criterios de Éxito (borrador — a validar)

No se definieron métricas de éxito explícitas en esta conversación; se proponen como punto de partida a confirmar o ajustar:

- **Adopción:** alcanzar el rango de ~100 usuarios invitados activos durante el primer año.
- **Uso real del feature ancla:** los usuarios activos vuelven al dashboard "hoy / seguí viendo" con cierta regularidad (frecuencia a definir — ej. varias veces por semana), en vez de usar AniList directamente para esa consulta.
- **Confiabilidad de la sincronización:** los snapshots históricos se generan de forma consistente, sin huecos, para que la vista de tendencias sea confiable.

## Visión (más allá de V1)

Si el enfoque "solo lectura sobre AniList + MAL-Sync" prueba tener valor, los caminos de crecimiento natural — ninguno comprometido para V1 — serían: habilitar edición/escritura hacia AniList desde la misma app, sumar soporte a otras plataformas (MyAnimeList, Kitsu), y funciones sociales como comparar listas entre usuarios invitados. Todo esto se dejó fuera deliberadamente para mantener V1 chico y enfocado en el dashboard "seguí viendo".
