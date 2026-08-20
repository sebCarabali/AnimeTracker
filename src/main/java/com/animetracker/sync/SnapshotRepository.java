package com.animetracker.sync;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.animetracker.domain.Snapshot;

/**
 * Único punto de escritura sobre {@code snapshot} (AD-2) -- visibilidad
 * package-private a propósito, nunca expuesta fuera de {@code sync}.
 */
interface SnapshotRepository extends JpaRepository<Snapshot, Long> {

    List<Snapshot> findByTrackingEntry_AppUser_Id(Long appUserId);
}
