package com.animetracker.domain;

/**
 * Los 5 estados estándar de tracking del producto, 1:1 con el subconjunto
 * soportado de {@code MediaListStatus} de AniList (Story 2.1). Los nombres
 * coinciden literalmente con los valores crudos que devuelve AniList para que
 * {@code TrackingStatus.valueOf(rawStatus)} sea la única lógica de mapeo
 * necesaria en {@code integration.anilist.AniListMediaListClient} -- un
 * estado de AniList sin equivalente acá (p.ej. {@code PAUSED}) lanza
 * {@link IllegalArgumentException} y se descarta silenciosamente para esa
 * corrida de sync, en vez de agregarse como un sexto valor.
 */
public enum TrackingStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    DROPPED,
    REPEATING
}
