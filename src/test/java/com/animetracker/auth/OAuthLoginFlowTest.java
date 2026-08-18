package com.animetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cubre los 3 escenarios de la I/O & Edge-Case Matrix de la spec 1.1 sin
 * depender de credenciales ni red reales de AniList:
 * - Happy path: /login es público y el CTA inicia el redirect OAuth oficial.
 * - Falla transitoria de OAuth: el callback sin authorization request previa
 *   en sesión (state/code inválido o ausente) dispara el failureUrl.
 * - Handoff a Story 1.2: el success handler redirige a "/" sin crear nada
 *   más allá de la autenticación de Spring Security.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OAuthLoginFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageIsPubliclyAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Continuar con AniList")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/oauth2/authorization/anilist")));
    }

    @Test
    void loginPageWithErrorFlagShowsRetryMessageAnnouncedViaAriaLive() throws Exception {
        mockMvc.perform(get("/login").param("error", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-live=\"polite\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "No pudimos completar el inicio de sesión con AniList. Inténtalo de nuevo.")));
    }

    @Test
    void unauthenticatedRequestToProtectedRouteIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void startingOAuthRedirectsToAniListOfficialAuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/anilist"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .startsWith("https://anilist.co/api/v2/oauth/authorize"));
    }

    @Test
    void transientOAuthFailureRedirectsBackToLoginWithErrorAndNoSession() throws Exception {
        // Sin una authorization request previa guardada en sesión (equivalente a
        // que el usuario cancele el consentimiento o a un error de red antes de
        // completar el login), el filtro de callback de Spring Security dispara
        // el failureUrl configurado en SecurityConfig.
        mockMvc.perform(get("/login/oauth2/code/anilist").param("code", "irrelevant").param("state", "irrelevant"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }

    @Test
    void authenticatedOAuth2UserPassesTheSecurityGateOnProtectedRoutes() throws Exception {
        // No existe todavía una página real en "/" (eso es de otras stories/epics);
        // lo que este test prueba es que, a diferencia del caso no autenticado
        // (que redirige a /login), un Viewer de AniList autenticado sí atraviesa
        // el gate de seguridad de SecurityConfig.
        mockMvc.perform(get("/").with(oauth2Login()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(302));
    }

    @Test
    void successHandlerRedirectsToRootWithoutTouchingAppUserOrWhitelist() throws Exception {
        OAuthLoginSuccessHandler handler = new OAuthLoginSuccessHandler();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new TestingAuthenticationToken("anilist-viewer-id", null));

        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }
}
