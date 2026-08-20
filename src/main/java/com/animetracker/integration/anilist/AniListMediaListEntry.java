package com.animetracker.integration.anilist;

import com.animetracker.domain.TrackingStatus;

/**
 * Entrada ya mapeada de la lista de anime del usuario en AniList, resuelta
 * vía GraphQL. Anti-corrupción (AD-1): ningún otro paquete debe modelar
 * directamente el shape crudo de {@code MediaListCollection} -- en
 * particular, un status de AniList sin equivalente en {@link TrackingStatus}
 * (p.ej. {@code PAUSED}) nunca llega a existir como instancia de este record;
 * se descarta en {@link AniListMediaListClient} antes de construirlo.
 */
public record AniListMediaListEntry(Long mediaId, TrackingStatus status, int progress) {
}
