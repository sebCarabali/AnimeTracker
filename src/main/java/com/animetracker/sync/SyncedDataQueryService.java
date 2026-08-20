package com.animetracker.sync;

import java.util.List;

import com.animetracker.domain.TrackingEntry;

/**
 * Única puerta de lectura pública sobre datos sincronizados (AD-9): cualquier
 * feature futura (Hoy, Por Estado, Tendencias, Onboarding) que necesite
 * {@link TrackingEntry}/{@code Snapshot} debe pasar por acá -- nunca
 * consultando AniList en vivo ni instanciando los repositorios JPA
 * package-private de {@code sync} directamente.
 */
public interface SyncedDataQueryService {

    List<TrackingEntry> findActiveEntries(Long appUserId);
}
