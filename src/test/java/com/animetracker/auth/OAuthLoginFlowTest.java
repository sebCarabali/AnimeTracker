package com.animetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.animetracker.domain.AppUserRepository;

/**
 * Cubre los 3 escenarios de la I/O & Edge-Case Matrix de la spec 1.1 sin
 * depender de credenciales ni red reales de AniList:
 * - Happy path: /login es público y el CTA inicia el redirect OAuth oficial.
 * - Falla transitoria de OAuth: el callback sin authorization request previa
 *   en sesión (state/code inválido o ausente) dispara el failureUrl.
 * - Handoff a Story 1.2: el success handler ahora consulta la Whitelist antes
 *   de redirigir (ver WhitelistGateTest para las 4 filas completas de esa
 *   I/O Matrix); acá solo se verifica que el handler está bien conectado.
 *
 * Desde Story 1.2 el contexto completo de Spring necesita un datasource real
 * (JPA + Flyway), así que esta clase también levanta Testcontainers Postgres,
 * igual que WhitelistGateTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OAuthLoginFlowTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

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
    void accessDeniedPageIsPubliclyReachableThroughTheRealSecurityFilterChain() throws Exception {
        // A diferencia de WhitelistGateTest (que invoca el handler directo), esto
        // ejercita de verdad la entrada permitAll de /acceso-denegado en
        // SecurityConfig: si esa entrada se borrara o se tipeara mal, Spring
        // Security redirigiría esta request no autenticada a /login en vez de
        // servir la página dedicada (UX-DR12), y este test fallaría.
        mockMvc.perform(get("/acceso-denegado"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers
                        .containsString("Tu cuenta de AniList no está habilitada todavía.")));
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
    void successHandlerConsultsTheWhitelistBeforeGrantingAccess(
            @Autowired OAuthLoginSuccessHandler handler, @Autowired AppUserRepository appUserRepository)
            throws Exception {
        // Id de AniList sin fila en whitelisted_user: el handler real (con sus
        // dependencias JPA reales, wireadas contra el Postgres del container)
        // debe rechazarlo sin crear un AppUser. La cobertura exhaustiva de las 4
        // filas de la I/O Matrix de Story 1.2 (AD-5) vive en WhitelistGateTest.
        long anilistUserId = 424_242L;
        OAuth2User oAuth2User = new DefaultOAuth2User(Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", String.valueOf(anilistUserId), "name", "tester"), "id");
        Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(),
                "anilist");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/acceso-denegado");
        assertThat(appUserRepository.findByAnilistUserId(anilistUserId)).isEmpty();
    }
}
