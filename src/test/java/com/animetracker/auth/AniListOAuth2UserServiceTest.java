package com.animetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClientException;

import com.animetracker.integration.anilist.AniListViewer;
import com.animetracker.integration.anilist.AniListViewerClient;

/**
 * Cubre el patch de Step-4 review: cualquier falla al resolver el Viewer de
 * AniList (respuesta GraphQL inválida, 4xx/5xx, timeout) debe convertirse en
 * OAuth2AuthenticationException -- nunca propagar como RuntimeException sin
 * manejar -- para que Spring Security la enrute a /login?error (AD-4) en vez
 * de un 500. Ver también OAuthLoginFlowTest para el escenario de "no hay
 * authorization request guardada", que es un caso de falla distinto (antes
 * de llegar a loadUser).
 */
class AniListOAuth2UserServiceTest {

    private static final OAuth2UserRequest USER_REQUEST = new OAuth2UserRequest(
            ClientRegistration.withRegistrationId("anilist")
                    .clientId("client-id")
                    .clientSecret("client-secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/anilist")
                    .authorizationUri("https://anilist.co/api/v2/oauth/authorize")
                    .tokenUri("https://anilist.co/api/v2/oauth/token")
                    .build(),
            new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token-value", Instant.now(),
                    Instant.now().plusSeconds(3600)));

    @Test
    void wrapsAnyRuntimeExceptionFromViewerResolutionAsOAuth2AuthenticationException() {
        AniListViewerClient aniListViewerClient = mock(AniListViewerClient.class);
        when(aniListViewerClient.fetchViewer(anyString()))
                .thenThrow(new RestClientException("AniList respondió 503"));

        AniListOAuth2UserService service = new AniListOAuth2UserService(aniListViewerClient);

        assertThatThrownBy(() -> service.loadUser(USER_REQUEST))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void treatsAMissingViewerIdAsAnAuthenticationFailureInsteadOfTheLiteralStringNull() {
        AniListViewerClient aniListViewerClient = mock(AniListViewerClient.class);
        when(aniListViewerClient.fetchViewer(anyString())).thenReturn(new AniListViewer(null, "sin-id"));

        AniListOAuth2UserService service = new AniListOAuth2UserService(aniListViewerClient);

        assertThatThrownBy(() -> service.loadUser(USER_REQUEST))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void resolvesAValidViewerIntoAnOAuth2UserKeyedByTheAniListId() {
        AniListViewerClient aniListViewerClient = mock(AniListViewerClient.class);
        when(aniListViewerClient.fetchViewer(anyString())).thenReturn(new AniListViewer(42L, "vale"));

        AniListOAuth2UserService service = new AniListOAuth2UserService(aniListViewerClient);

        OAuth2User user = service.loadUser(USER_REQUEST);

        assertThat(user.getName()).isEqualTo("42");
        assertThat(user.getAttributes()).containsEntry("name", "vale");
    }
}
