package com.animetracker.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fila de la Whitelist de Invitación: solo {@code auth} consulta esta tabla
 * (AD-5), por eso la entidad y su repositorio son package-private. Alta es
 * manual por DB directa en V1 -- no existe UI de administración (OQ-1,
 * deferred). Mantiene su propia PK, separada del id de AniList.
 */
@Entity
@Table(name = "whitelisted_user")
class WhitelistedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anilist_user_id", nullable = false, unique = true)
    private Long anilistUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WhitelistedUser() {
        // Requerido por JPA.
    }

    WhitelistedUser(Long anilistUserId) {
        this.anilistUserId = anilistUserId;
        this.createdAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    Long getAnilistUserId() {
        return anilistUserId;
    }
}
