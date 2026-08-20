package com.animetracker.sync;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.animetracker.domain.TrackingEntry;

/**
 * Único punto de escritura sobre {@code tracking_entry} (AD-2) -- visibilidad
 * package-private a propósito, nunca expuesta fuera de {@code sync}. Lectura
 * publicada para el resto de las features vía {@link SyncedDataQueryService}.
 */
interface TrackingEntryRepository extends JpaRepository<TrackingEntry, Long> {

    List<TrackingEntry> findByAppUserId(Long appUserId);

    List<TrackingEntry> findByAppUserIdAndActiveTrue(Long appUserId);
}
