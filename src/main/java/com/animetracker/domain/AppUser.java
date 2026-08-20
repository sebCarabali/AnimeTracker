package com.animetracker.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuario de la app, compartido entre packages (a diferencia de
 * {@code TrackingEntry}/{@code Snapshot}, confinados a {@code sync/} por
 * AD-2). Mantiene su propia PK más una columna indexada separada para el id
 * remoto de AniList -- nunca conflar ambos ids, incluso si apuntan a la
 * misma cuenta.
 *
 * Solo {@code auth} crea filas nuevas (vía {@code findOrCreate}, AD-5);
 * cualquier otro package que necesite un {@code AppUser} debe leer/actualizar
 * uno ya existente, nunca instanciar uno propio.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    public static final String DEFAULT_THEME_PREFERENCE = "dark";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anilist_user_id", nullable = false, unique = true)
    private Long anilistUserId;

    @Column(name = "theme_preference", nullable = false)
    private String themePreference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUser() {
        // Requerido por JPA.
    }

    public AppUser(Long anilistUserId, String themePreference) {
        this.anilistUserId = anilistUserId;
        this.themePreference = themePreference;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAnilistUserId() {
        return anilistUserId;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
