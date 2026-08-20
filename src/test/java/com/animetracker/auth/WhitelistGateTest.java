package com.animetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.animetracker.domain.AppUserRepository;

/**
 * Cubre las 4 filas de la I/O & Edge-Case Matrix de la spec 1.2 (AD-5) contra
 * un Postgres real vía Testcontainers -- Flyway aplica V1__... al arrancar el
 * contexto, igual que en producción.
 *
 * Nota sobre "oauth2Login()": SecurityMockMvcRequestPostProcessors.oauth2Login()
 * inyecta un SecurityContext ya autenticado para una única request MockMvc;
 * no atraviesa el filtro real de login OAuth2, así que nunca invoca
 * AuthenticationSuccessHandler (el gate vive ahí). Por eso estos tests
 * invocan el bean real de OAuthLoginSuccessHandler directamente -- con sus
 * dependencias reales (repos JPA contra el Postgres del container) -- pasándole
 * una Authentication construida igual que AniListOAuth2UserService construye
 * la real: DefaultOAuth2User con "id" como name-attribute-key y el id de
 * AniList como String.
 */
@SpringBootTest
@Testcontainers
class WhitelistGateTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OAuthLoginSuccessHandler handler;

    @Autowired
    private WhitelistedUserRepository whitelistedUserRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Authentication authenticationFor(long anilistUserId) {
        OAuth2User oAuth2User = new DefaultOAuth2User(Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", String.valueOf(anilistUserId), "name", "tester"), "id");
        return new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), "anilist");
    }

    @Test
    void whitelistedFirstLoginCreatesAppUserAndOpensAValidSession() throws Exception {
        long anilistUserId = 9_001L;
        whitelistedUserRepository.save(new WhitelistedUser(anilistUserId));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = authenticationFor(anilistUserId);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/");
        assertThat(appUserRepository.findByAnilistUserId(anilistUserId)).isPresent();
    }

    @Test
    void whitelistedReturningLoginReusesTheExistingAppUserInsteadOfDuplicating() throws Exception {
        long anilistUserId = 9_002L;
        whitelistedUserRepository.save(new WhitelistedUser(anilistUserId));
        Authentication authentication = authenticationFor(anilistUserId);

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), authentication);
        Long firstAppUserId = appUserRepository.findByAnilistUserId(anilistUserId).orElseThrow().getId();

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), secondResponse, authentication);

        assertThat(secondResponse.getRedirectedUrl()).isEqualTo("/");
        Long secondAppUserId = appUserRepository.findByAnilistUserId(anilistUserId).orElseThrow().getId();
        assertThat(secondAppUserId).isEqualTo(firstAppUserId);
    }

    @Test
    void nonWhitelistedUserIsRedirectedToAccessDeniedWithoutSessionOrAppUserRow() throws Exception {
        long anilistUserId = 9_003L;
        // A propósito, ninguna fila en whitelisted_user para este id.

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = authenticationFor(anilistUserId);
        // Simula el estado real al llegar acá: Spring Security ya autenticó y
        // persistió el SecurityContext antes de invocar el success handler.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/acceso-denegado");
        assertThat(appUserRepository.findByAnilistUserId(anilistUserId)).isEmpty();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonWhitelistedUserWithARealHttpSessionHasThatSessionInvalidated() throws Exception {
        long anilistUserId = 9_005L;
        // A propósito, ninguna fila en whitelisted_user para este id.

        MockHttpServletRequest request = new MockHttpServletRequest();
        // A diferencia de los demás tests (que pasan un MockHttpServletRequest sin
        // sesión asociada, así que SecurityContextLogoutHandler.logout() nunca
        // llega a invalidar nada), acá se crea una sesión real primero para
        // ejercitar de verdad la rama session.invalidate() del logout handler.
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = authenticationFor(anilistUserId);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(session.isInvalid()).isTrue();
        assertThat(request.getSession(false)).isNull();
    }

    @Test
    void gettingAddedToTheWhitelistAfterAPriorDenialAllowsTheNextLoginToCompleteWithoutReRegistering() throws Exception {
        long anilistUserId = 9_004L;
        Authentication authentication = authenticationFor(anilistUserId);

        MockHttpServletResponse deniedResponse = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), deniedResponse, authentication);
        assertThat(deniedResponse.getRedirectedUrl()).isEqualTo("/acceso-denegado");
        assertThat(appUserRepository.findByAnilistUserId(anilistUserId)).isEmpty();

        whitelistedUserRepository.save(new WhitelistedUser(anilistUserId));

        MockHttpServletResponse grantedResponse = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), grantedResponse, authentication);

        assertThat(grantedResponse.getRedirectedUrl()).isEqualTo("/");
        assertThat(appUserRepository.findByAnilistUserId(anilistUserId)).isPresent();
    }
}
