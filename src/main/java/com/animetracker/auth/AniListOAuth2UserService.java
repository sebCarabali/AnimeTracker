package com.animetracker.auth;

import java.util.Map;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.animetracker.integration.anilist.AniListViewer;
import com.animetracker.integration.anilist.AniListViewerClient;

/**
 * Puente entre Spring Security y integration.anilist.
 * Reemplaza al DefaultOAuth2UserService (que asume un user-info-uri REST
 * estándar) porque AniList solo expone la identidad del Viewer vía GraphQL.
 * Toda resolución de identidad pasa por AniListViewerClient (AD-1, AD-9).
 */
@Service
public class AniListOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String ID_ATTRIBUTE_NAME = "id";
    private static final String VIEWER_RESOLUTION_FAILED_ERROR_CODE = "anilist_viewer_resolution_failed";

    private final AniListViewerClient aniListViewerClient;

    public AniListOAuth2UserService(AniListViewerClient aniListViewerClient) {
        this.aniListViewerClient = aniListViewerClient;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String accessToken = userRequest.getAccessToken().getTokenValue();

        AniListViewer viewer;
        try {
            viewer = aniListViewerClient.fetchViewer(accessToken);
        } catch (RuntimeException ex) {
            // Cualquier falla al resolver el Viewer (respuesta GraphQL inválida, un
            // 4xx/5xx de AniList, timeout de red, etc.) debe tratarse como una falla
            // de autenticación -- así Spring Security la enruta al failureUrl
            // (/login?error) en vez de dejarla propagar como un 500 sin manejar
            // (AD-4, fila "Falla transitoria de OAuth" de la I/O Matrix).
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(VIEWER_RESOLUTION_FAILED_ERROR_CODE,
                            "No se pudo resolver la identidad del Viewer en AniList", null),
                    ex);
        }

        if (viewer.id() == null) {
            // Nunca convertir silenciosamente un id ausente en el literal "null" como
            // atributo de identidad -- es el mismo estado de falla que arriba.
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(VIEWER_RESOLUTION_FAILED_ERROR_CODE,
                            "AniList no devolvió un id de Viewer válido", null));
        }

        Map<String, Object> attributes = Map.of(
                ID_ATTRIBUTE_NAME, String.valueOf(viewer.id()),
                "name", viewer.name() == null ? "" : viewer.name());

        // DefaultOAuth2User exige al menos una authority; no representa un rol de
        // dominio real todavía (eso no existe hasta que Story 1.2 cree AppUser).
        return new DefaultOAuth2User(Set.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, ID_ATTRIBUTE_NAME);
    }
}
