package com.animetracker.auth;

import java.io.IOException;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

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
 */
@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String DEFAULT_TARGET_URL = "/";
    private static final String ACCESS_DENIED_TARGET_URL = "/acceso-denegado";
    private static final String LOGIN_ERROR_TARGET_URL = "/login?error";

    private final WhitelistedUserRepository whitelistedUserRepository;
    private final AppUserProvisioningService appUserProvisioningService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    OAuthLoginSuccessHandler(WhitelistedUserRepository whitelistedUserRepository,
            AppUserProvisioningService appUserProvisioningService) {
        this.whitelistedUserRepository = whitelistedUserRepository;
        this.appUserProvisioningService = appUserProvisioningService;
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

            appUserProvisioningService.findOrCreate(anilistUserId);
        } catch (DataAccessException databaseUnavailableOrFailed) {
            logoutHandler.logout(request, response, authentication);
            response.sendRedirect(LOGIN_ERROR_TARGET_URL);
            return;
        }

        response.sendRedirect(DEFAULT_TARGET_URL);
    }
}
