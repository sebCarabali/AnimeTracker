package com.animetracker.integration.anilist;

/**
 * Identidad del Viewer autenticado en AniList, resuelta vía GraphQL.
 * Anti-corrupción: ningún otro paquete debe modelar directamente la
 * respuesta cruda de la API de AniList (AD-1).
 */
public record AniListViewer(Long id, String name) {
}
