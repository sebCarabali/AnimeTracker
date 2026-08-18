package com.animetracker.integration.anilist;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de la capa anti-corrupción hacia AniList (AD-1, AD-9).
 * AniList no expone un endpoint REST de user-info estándar, así que la
 * identidad del Viewer autenticado se resuelve con una query GraphQL contra
 * https://graphql.anilist.co usando el access token OAuth como Bearer.
 * Ninguna llamada a AniList debe hacerse fuera de este paquete.
 */
@Component
public class AniListViewerClient {

    private static final String GRAPHQL_ENDPOINT = "https://graphql.anilist.co";
    private static final String VIEWER_QUERY = "{ Viewer { id name } }";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;

    public AniListViewerClient() {
        this(RestClient.builder().baseUrl(GRAPHQL_ENDPOINT).requestFactory(timeoutLimitedRequestFactory()).build());
    }

    private static ClientHttpRequestFactory timeoutLimitedRequestFactory() {
        HttpClientSettings settings = HttpClientSettings.defaults().withTimeouts(CONNECT_TIMEOUT, READ_TIMEOUT);
        return ClientHttpRequestFactoryBuilder.detect().build(settings);
    }

    AniListViewerClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public AniListViewer fetchViewer(String accessToken) {
        GraphQlResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(Map.of("query", VIEWER_QUERY))
                .retrieve()
                .body(GraphQlResponse.class);

        if (response == null || response.data() == null || response.data().viewer() == null) {
            throw new IllegalStateException("AniList no devolvió un Viewer válido para el token provisto");
        }

        ViewerDto viewer = response.data().viewer();
        return new AniListViewer(viewer.id(), viewer.name());
    }

    private record GraphQlResponse(Data data) {
    }

    private record Data(@JsonProperty("Viewer") ViewerDto viewer) {
    }

    private record ViewerDto(Long id, String name) {
    }
}
