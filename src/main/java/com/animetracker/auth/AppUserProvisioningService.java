package com.animetracker.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.animetracker.domain.AppUser;
import com.animetracker.domain.AppUserRepository;

/**
 * Único punto de creación de {@link AppUser} (AD-5): la whitelist gate en
 * {@link OAuthLoginSuccessHandler} es la única llamadora de
 * {@link #findOrCreate(Long)}. Ningún otro package -- en particular
 * {@code sync} -- crea un AppUser; solo lee/actualiza uno ya existente.
 *
 * findOrCreate busca primero por {@code anilist_user_id} y, si no existe,
 * intenta insertar en una transacción propia (REQUIRES_NEW, vía
 * {@link TransactionTemplate}). Eso es intencional y no cosmético: si la
 * inserción fallara dentro de la misma transacción ambiente y esa falla se
 * atrapara ahí mismo, Postgres igual dejaría la transacción en estado
 * abortado para cualquier sentencia posterior ("current transaction is
 * aborted"), rompiendo la relectura de abajo. Al aislar el intento de
 * inserción en su propia transacción, una violación del constraint único
 * -- dos requests concurrentes en el primer login del mismo usuario -- solo
 * hace rollback de esa transacción aislada; la relectura que sigue corre
 * limpia y devuelve la fila que sí logró insertar el otro request.
 */
@Service
class AppUserProvisioningService {

    private final AppUserRepository appUserRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;

    AppUserProvisioningService(AppUserRepository appUserRepository, PlatformTransactionManager transactionManager) {
        this.appUserRepository = appUserRepository;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    AppUser findOrCreate(Long anilistUserId) {
        return appUserRepository.findByAnilistUserId(anilistUserId)
                .orElseGet(() -> insertOrFetchExisting(anilistUserId));
    }

    private AppUser insertOrFetchExisting(Long anilistUserId) {
        try {
            return requiresNewTransactionTemplate.execute(status -> appUserRepository
                    .save(new AppUser(anilistUserId, AppUser.DEFAULT_THEME_PREFERENCE)));
        } catch (DataIntegrityViolationException raceLostToConcurrentFirstLogin) {
            return appUserRepository.findByAnilistUserId(anilistUserId)
                    .orElseThrow(() -> raceLostToConcurrentFirstLogin);
        }
    }
}
