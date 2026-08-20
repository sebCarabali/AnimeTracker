package com.animetracker.integration.anilist;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.animetracker.domain.TrackingStatus;

/**
 * Cliente de la capa anti-corrupción hacia AniList (AD-1, AD-9) para la lista
 * de anime del usuario. Igual patrón que {@link AniListViewerClient}
 * (RestClient + timeouts, sin librería de GraphQL client/codegen): query
 * GraphQL {@code MediaListCollection(userId, type: ANIME)} contra
 * https://graphql.anilist.co, solo lectura -- ninguna mutation GraphQL existe
 * en el codebase. Ninguna llamada a AniList debe hacerse fuera de este
 * paquete.
 *
 * El mapeo de status crudo de AniList a {@link TrackingStatus} pasa por
 * {@code TrackingStatus.valueOf(rawStatus)}; un status sin equivalente (p.ej.
 * {@code PAUSED}) se descarta silenciosamente de la lista devuelta -- la
 * entrada completa queda ausente para esta corrida, ver Story 2.1 Ask First.
 */
@Component
public class AniListMediaListClient {

    private static final Logger log = LoggerFactory.getLogger(AniListMediaListClient.class);

    private static final String GRAPHQL_ENDPOINT = "https://graphql.anilist.co";
    private static final String ANIME_MEDIA_TYPE = "ANIME";

    private static final String MEDIA_LIST_QUERY = """
            query ($userId: Int, $type: MediaType) {
              MediaListCollection(userId: $userId, type: $type) {
                lists {
                  entries {
                    status
                    progress
                    media {
                      id
                    }
                  }
                }
              }
            }
            """;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;

    public AniListMediaListClient() {
        this(RestClient.builder().baseUrl(GRAPHQL_ENDPOINT).requestFactory(timeoutLimitedRequestFactory()).build());
    }

    private static ClientHttpRequestFactory timeoutLimitedRequestFactory() {
        HttpClientSettings settings = HttpClientSettings.defaults().withTimeouts(CONNECT_TIMEOUT, READ_TIMEOUT);
        return ClientHttpRequestFactoryBuilder.detect().build(settings);
    }

    AniListMediaListClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<AniListMediaListEntry> fetchMediaList(Long anilistUserId, String accessToken) {
        GraphQlResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .body(Map.of("query", MEDIA_LIST_QUERY,
                        "variables", Map.of("userId", anilistUserId, "type", ANIME_MEDIA_TYPE)))
                .retrieve()
                .body(GraphQlResponse.class);

        if (response == null || response.data() == null || response.data().mediaListCollection() == null) {
            throw new IllegalStateException(
                    "AniList no devolvió una MediaListCollection válida para el usuario provisto");
        }

        List<MediaListGroupDto> lists = response.data().mediaListCollection().lists();
        if (lists == null) {
            return List.of();
        }

        List<AniListMediaListEntry> entries = new ArrayList<>();
        for (MediaListGroupDto group : lists) {
            if (group == null || group.entries() == null) {
                continue;
            }
            for (MediaListEntryDto entry : group.entries()) {
                appendMapped(entries, entry);
            }
        }
        return entries;
    }

    private static void appendMapped(List<AniListMediaListEntry> entries, MediaListEntryDto entry) {
        if (entry == null || entry.media() == null || entry.media().id() == null || entry.progress() == null) {
            return;
        }
        mapStatus(entry.status())
                .ifPresent(status -> entries.add(new AniListMediaListEntry(entry.media().id(), status, entry.progress())));
    }

    private static Optional<TrackingStatus> mapStatus(String rawStatus) {
        if (rawStatus == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(TrackingStatus.valueOf(rawStatus));
        } catch (IllegalArgumentException unmappedStatus) {
            // Status de AniList sin equivalente en TrackingStatus (p.ej. PAUSED):
            // se omite del upsert de esta corrida, según lo confirmado en Ask First.
            log.debug("Status de AniList sin equivalente en TrackingStatus, se descarta la entrada de esta "
                    + "corrida: rawStatus={}", rawStatus);
            return Optional.empty();
        }
    }

    private record GraphQlResponse(Data data) {
    }

    private record Data(@JsonProperty("MediaListCollection") MediaListCollectionDto mediaListCollection) {
    }

    private record MediaListCollectionDto(List<MediaListGroupDto> lists) {
    }

    private record MediaListGroupDto(List<MediaListEntryDto> entries) {
    }

    private record MediaListEntryDto(String status, Integer progress, MediaDto media) {
    }

    private record MediaDto(Long id) {
    }
}
