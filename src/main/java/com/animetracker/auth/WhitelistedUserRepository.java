package com.animetracker.auth;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Package-private: solo {@code auth} consulta la Whitelist de Invitación
 * (AD-5). Ningún otro package debe depender de este repositorio.
 */
interface WhitelistedUserRepository extends JpaRepository<WhitelistedUser, Long> {

    boolean existsByAnilistUserId(Long anilistUserId);
}
