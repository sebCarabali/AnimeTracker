package com.animetracker.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Snapshot histórico e inmutable del progreso de un {@link TrackingEntry} en
 * una corrida de sync puntual (Story 2.1). Cardinalidad por
 * (usuario, anime, corrida) -- nunca un agregado único por usuario -- para
 * que Tendencias (Epic 5) pueda sumar diferencias de episodio entre
 * Snapshots consecutivos on-read. {@code takenAt} es el mismo {@link Instant}
 * UTC para todas las entradas de la misma corrida.
 *
 * Entidad compartida en {@code domain}; su repositorio JPA es
 * package-private dentro de {@code sync} (AD-2). Se retiene indefinidamente
 * en V1 -- no existe política de purga todavía.
 */
@Entity
@Table(name = "snapshot")
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracking_entry_id", nullable = false)
    private TrackingEntry trackingEntry;

    @Column(name = "episode_progress", nullable = false)
    private int episodeProgress;

    @Column(name = "taken_at", nullable = false)
    private Instant takenAt;

    protected Snapshot() {
        // Requerido por JPA.
    }

    public Snapshot(TrackingEntry trackingEntry, int episodeProgress, Instant takenAt) {
        this.trackingEntry = trackingEntry;
        this.episodeProgress = episodeProgress;
        this.takenAt = takenAt;
    }

    public Long getId() {
        return id;
    }

    public TrackingEntry getTrackingEntry() {
        return trackingEntry;
    }

    public int getEpisodeProgress() {
        return episodeProgress;
    }

    public Instant getTakenAt() {
        return takenAt;
    }
}
