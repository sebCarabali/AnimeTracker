package com.animetracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Estado local de tracking de un anime para un {@link AppUser} (Story 2.1).
 * Entidad compartida en {@code domain} -- a diferencia de sus repositorios
 * JPA, confinados package-private a {@code sync} (AD-2) -- porque el resto de
 * las features (Hoy, Por Estado, Tendencias) leen instancias de esta clase
 * vía {@code sync.SyncedDataQueryService}, nunca la fila cruda por SQL propio.
 *
 * Mantiene su propia PK más una columna indexada separada para el id remoto
 * de AniList ({@code anilistMediaId}) -- nunca conflar ambos ids. Única
 * escritora: {@code sync.SyncService}, que hace upsert in-place (misma fila)
 * en sync recurrente y baja lógica ({@code active=false}) cuando el anime ya
 * no aparece en la respuesta de AniList (reconciliación completa por
 * corrida). No persiste título/póster ni ningún otro campo de Media -- eso es
 * scope de las vistas de lectura (Epic 3+).
 */
@Entity
@Table(name = "tracking_entry")
public class TrackingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    @Column(name = "anilist_media_id", nullable = false)
    private Long anilistMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrackingStatus status;

    @Column(name = "last_episode", nullable = false)
    private int lastEpisode;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected TrackingEntry() {
        // Requerido por JPA.
    }

    public TrackingEntry(AppUser appUser, Long anilistMediaId, TrackingStatus status, int lastEpisode) {
        this.appUser = appUser;
        this.anilistMediaId = anilistMediaId;
        this.status = status;
        this.lastEpisode = lastEpisode;
        this.active = true;
    }

    /**
     * Upsert in-place para una corrida de sync donde este anime sigue
     * apareciendo en la respuesta de AniList: actualiza estado/progreso y
     * reactiva la fila si venía de una baja lógica previa.
     */
    public void applyRemoteState(TrackingStatus status, int lastEpisode) {
        this.status = status;
        this.lastEpisode = lastEpisode;
        this.active = true;
    }

    /** Baja lógica: este anime ya no aparece en la respuesta de esta corrida. */
    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public AppUser getAppUser() {
        return appUser;
    }

    public Long getAnilistMediaId() {
        return anilistMediaId;
    }

    public TrackingStatus getStatus() {
        return status;
    }

    public int getLastEpisode() {
        return lastEpisode;
    }

    public boolean isActive() {
        return active;
    }
}
