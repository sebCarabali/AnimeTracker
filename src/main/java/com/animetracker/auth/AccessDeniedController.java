package com.animetracker.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página dedicada de Acceso Denegado (Story 1.2, UX-DR12): distinta de
 * Login, sin redirección automática de reintento. Se llega acá desde
 * OAuthLoginSuccessHandler cuando el id de AniList del usuario no está en la
 * Whitelist de Invitación.
 */
@Controller
public class AccessDeniedController {

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "auth/acceso-denegado";
    }
}
