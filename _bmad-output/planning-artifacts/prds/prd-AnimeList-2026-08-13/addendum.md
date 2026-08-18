---
title: AnimeTracker — Addendum
created: 2026-08-13
updated: 2026-08-13
---

# Addendum: AnimeTracker

Contenido que enriquece el PRD pero no encaja como requisito — preservado acá para no perderlo. El PRD (`prd.md`) es el documento canónico para arquitectura y épicas; este addendum es contexto de apoyo.

## Razón de diferenciación (de "Qué Lo Hace Diferente" en el Brief)

AnimeTracker no tiene una ventaja técnica defendible ("moat") frente a AniList o MAL-Sync — ambas herramientas son más maduras que cualquier cosa que se construya para este proyecto en las áreas relevantes (detección de reproducción y catálogo/tracking), y esa es justamente la razón de apoyarse en ellas en vez de competir. Construir una alternativa a cualquiera de las dos sería esfuerzo desperdiciado.

La diferenciación de AnimeTracker es de **experiencia**, no de datos ni de detección: un dashboard enfocado en "qué sigue" en vez de un catálogo completo, y una vista de tendencias en el tiempo que AniList no tiene de forma nativa. Es una app deliberadamente chica y enfocada — no un tracker alternativo, no un competidor de AniList.

Esta razón importa para cualquier decisión futura de alcance: cualquier feature que empuje a AnimeTracker hacia "reconstruir lo que AniList o MAL-Sync ya resuelven" contradice la premisa fundacional del proyecto, incluso si técnicamente es viable.
