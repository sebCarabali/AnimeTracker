package com.animetracker.auth;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import com.animetracker.domain.AppUser;
import com.animetracker.sync.SyncService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler de éxito del login OAuth con AniList.
 *
 * Implementa el gate de Whitelist de Invitación (Story 1.2, AD-5). Orden
 * exacto, preservado literalmente de la Story 1.1: (1) resolver
 * WhitelistedUser por el id de AniList del usuario autenticado, sin crear
 * AppUser todavía; (2) si no está en la whitelist, redirigir a la pantalla de
 * Acceso Denegado sin crear sesión ni fila -- para eso se invalida
 * explícitamente la sesión/SecurityContext que Spring Security ya estableció
 * antes de invocar este handler, porque un usuario rechazado no debe quedar
 * con una sesión autenticada; (3) si está, recién ahí ejecutar
 * findOrCreate(AppUser) y considerar la sesión válida.
 *
 * Cualquier falla al resolver el id de AniList del principal (nombre no
 * numérico/ausente) o al hablar con la base de datos (whitelist o
 * findOrCreate) se trata como una falla de login, no como un 500 sin manejar
 * -- mismo criterio que AniListOAuth2UserService (AD-4): se redirige a
 * /login?error en vez de dejar propagar la excepción.
 *
 * Story 2.1 (AD-7): una vez que la sesión es válida, este handler dispara de
 * forma síncrona/bloqueante {@link SyncService#syncUser} antes del redirect
 * final a "/" -- ninguna vista post-login debe renderizar sin datos ya
 * sincronizados. El access token se obtiene vía
 * {@link OAuth2AuthorizedClientService#loadAuthorizedClient} (bean
 * autoconfigurado por Spring Boot, sin necesitar el
 * OAuth2AuthorizedClientRepository basado en sesión) usando el mismo
 * principal-name (id de AniList) que ya resuelve el gate de whitelist. Una
 * falla de sync durante el login (red/timeout, token ausente) se loggea y el
 * login continúa igual -- nunca bloquea al usuario ni rompe el redirect
 * (Story 2.3, degradación explícita, queda fuera de este alcance).
 */
@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginSuccessHandler.class);

    private static final String DEFAULT_TARGET_URL = "/";
    private static final String ACCESS_DENIED_TARGET_URL = "/acceso-denegado";
    private static final String LOGIN_ERROR_TARGET_URL = "/login?error";
    private static final String ANILIST_CLIENT_REGISTRATION_ID = "anilist";

    private final WhitelistedUserRepository whitelistedUserRepository;
    private final AppUserProvisioningService appUserProvisioningService;
    private final SyncService syncService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    OAuthLoginSuccessHandler(WhitelistedUserRepository whitelistedUserRepository,
            AppUserProvisioningService appUserProvisioningService, SyncService syncService,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.whitelistedUserRepository = whitelistedUserRepository;
        this.appUserProvisioningService = appUserProvisioningService;
        this.syncService = syncService;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Long anilistUserId;
        try {
            anilistUserId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException nonNumericPrincipalName) {
            logoutHandler.logout(request, response, authentication);
            response.sendRedirect(LOGIN_ERROR_TARGET_URL);
            return;
        }

        AppUser appUser;
        try {
            if (!whitelistedUserRepository.existsByAnilistUserId(anilistUserId)) {
                // Sin fila en la whitelist: ningún AppUser, ninguna sesión. Spring
                // Security ya persistió el SecurityContext autenticado en la sesión
                // antes de llegar acá (así es como este handler recibe una
                // Authentication válida) -- hay que deshacer eso explícitamente.
                logoutHandler.logout(request, response, authentication);
                response.sendRedirect(ACCESS_DENIED_TARGET_URL);
                return;
            }

            appUser = appUserProvisioningService.findOrCreate(anilistUserId);
        } catch (DataAccessException databaseUnavailableOrFailed) {
            logoutHandler.logout(request, response, authentication);
            response.sendRedirect(LOGIN_ERROR_TARGET_URL);
            return;
        }

        syncOnLoginBestEffort(appUser, authentication);

        response.sendRedirect(DEFAULT_TARGET_URL);
    }

    /**
     * Sync forzado síncrono (AD-7), pero best-effort: cualquier falla -- token
     * ausente, red/timeout hacia AniList, lo que sea -- se loggea y se traga acá
     * mismo. El login ya es válido en este punto (sesión + AppUser ya
     * confirmados); una corrida de sync fallida nunca debe convertirse en un
     * redirect a /login?error ni en un 500.
     */
    private void syncOnLoginBestEffort(AppUser appUser, Authentication authentication) {
        try {
            OAuth2AuthorizedClient authorizedClient = authorizedClientService
                    .loadAuthorizedClient(ANILIST_CLIENT_REGISTRATION_ID, authentication.getName());
            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                log.warn(
                        "No se encontró un access token de AniList autorizado para el usuario anilist_user_id={} "
                                + "tras el login; se omite el sync forzado y el login continúa igual",
                        appUser.getAnilistUserId());
                return;
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            syncService.syncUser(appUser, accessToken);
        } catch (RuntimeException syncFailure) {
            log.warn(
                    "Sync forzado de login falló para el usuario anilist_user_id={}; el login continúa igual",
                    appUser.getAnilistUserId(), syncFailure);
        }
    }
}
