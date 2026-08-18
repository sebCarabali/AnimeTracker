---
title: AnimeTracker — Addendum
status: draft
created: 2026-08-13
updated: 2026-08-13
---

# Addendum: AnimeTracker

Contenido de contexto que no entra en el brief ejecutivo pero es relevante para las fases siguientes (PRD / arquitectura).

## Alternativas consideradas y descartadas

**Opción B — extensión propia + base de datos propia como fuente de verdad.**
Considerada al inicio de la conversación: construir una extensión de navegador propia (en vez de usar MAL-Sync) que hablara directo con una API propia, dueña de punta a punta del dato de catálogo y progreso. Se descartó porque MAL-Sync ya resuelve la detección de reproducción en JKAnime y AnimeFLV de forma madura y mantenida (soporte confirmado desde su v0.4.8), y reconstruir esa lógica de scraping específico por sitio —que además requiere mantenimiento continuo porque los sitios cambian su HTML— no aporta valor diferencial al proyecto. Se optó por apoyarse en MAL-Sync + AniList y enfocar el esfuerzo propio en la capa de dashboard/tendencias.

**Catálogo 100% propio (carga manual o base propia independiente de APIs externas).**
Mencionado como preferencia inicial, antes de decidir apoyarse en AniList como fuente de verdad. Se abandonó al elegir la Opción A (usar MAL-Sync + AniList): mantener un catálogo de animes propio (miles de títulos, metadata, imágenes) es esfuerzo significativo sin beneficio claro frente a usar el catálogo ya curado de AniList. La base de datos propia de V1 cambia de rol: pasa de "fuente de verdad de catálogo" a "cache + histórico de snapshots".

**Lectura + escritura hacia AniList (cliente completo).**
Se consideró que AnimeTracker permitiera editar estado, rating y notas desde la propia app, escribiendo esos cambios de vuelta a AniList vía mutations de GraphQL. Se descartó para V1 en favor de un modelo estrictamente de solo lectura, para mantener el alcance chico — queda anotado como posible V2 en la sección Visión del brief.

**Multi-plataforma (MyAnimeList, Kitsu, Simkl además de AniList).**
MAL-Sync soporta sincronizar contra varias plataformas simultáneamente (MyAnimeList, AniList, Kitsu, Simkl, Shikimori). Se decidió limitar V1 a AniList únicamente por su API GraphQL moderna y gratuita, dejando el soporte multi-plataforma fuera de alcance explícito.

## Contexto técnico de investigación (research digest)

Investigación de comparables realizada durante la conversación para fundamentar las decisiones de arquitectura:

- **MAL-Sync** es la extensión dominante en este espacio: open-source, soporta Chrome/Firefox/Tampermonkey, y sincroniza contra MyAnimeList, AniList, Kitsu, Simkl y Shikimori simultáneamente sobre 100+ sitios de streaming. Confirmado soporte nativo para JKAnime y AnimeFLV (agregado en su v0.4.8), con módulos de detección dedicados por sitio en su repositorio (`src/pages/Jkanime/main.ts`, equivalente para AnimeFLV).
- **Mecanismo de detección:** cada sitio requiere un módulo hecho a mano con selectores CSS y/o parseo de URL — no existe una forma genérica de detectar título/episodio en un sitio de streaming arbitrario. Esto implica mantenimiento continuo por parte de quien mantiene esos módulos (en este caso, el proyecto MAL-Sync, no AnimeTracker).
- **Restricciones de extensiones (Manifest V3):** las llamadas cross-origin desde un content script deben pasar por el service worker de background, con permisos de host declarados explícitamente — relevante solo si en el futuro se reconsiderara construir una extensión propia.
- **Modelo de datos estándar de la industria** (MAL, AniList, Simkl, Kitsu): enum de estado (viendo, completado, en pausa, abandonado, planeado), progreso de episodios, contador de rewatch, score (1-10 u otras escalas), fechas de inicio/fin, timestamps por episodio visto.
- Fuentes: repositorio de MAL-Sync en GitHub, documentación de Simkl sobre tracking de progreso, Universal Trakt Scrobbler (comparable de arquitectura, no de dominio), documentación de MDN sobre content scripts y la guía de migración a Manifest V3.

## Preguntas abiertas para PRD / arquitectura

- Mecanismo concreto de invitación (código de invitación, alta manual por admin, lista blanca por email) — no definido en esta conversación.
- Frecuencia de sincronización de snapshots (cada N minutos, al login, job nocturno) — pendiente de decisión técnica.
- Manejo explícito de degradación cuando AniList no responde o el usuario revoca el OAuth (qué ve el usuario, por cuánto tiempo se sirve el último snapshot).
- Métricas de éxito: las propuestas en el brief son un borrador inicial del asistente, no fueron provistas por el usuario — deberían revisarse y ajustarse en una siguiente conversación o durante el PRD.
