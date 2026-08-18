package com.animetracker.auth;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler de éxito del login OAuth con AniList.
 *
 * PUNTO DE EXTENSIÓN PARA STORY 1.2: acá es donde debe insertarse la consulta
 * a WhitelistedUser y, si el usuario está habilitado, el findOrCreate(AppUser)
 * (AD-5). El orden correcto (ver epic-1-context.md) es: (1) resolver
 * WhitelistedUser por el id de AniList del usuario autenticado, sin crear
 * AppUser todavía; (2) si no está en la whitelist, redirigir a la pantalla de
 * Acceso Denegado sin crear sesión ni fila; (3) si está, recién ahí ejecutar
 * findOrCreate(AppUser) y considerar la sesión válida. Story 1.1 no implementa
 * nada de esto: solo redirige a "/" tras un login OAuth técnicamente exitoso.
 */
@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String DEFAULT_TARGET_URL = "/";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // TODO(Story 1.2): whitelist gate + findOrCreate(AppUser) va acá, antes de
        // considerar la sesión válida. Por ahora se redirige siempre a "/".
        response.sendRedirect(DEFAULT_TARGET_URL);
    }
}
