package com.animetracker.integration.anilist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.animetracker.domain.TrackingStatus;

/**
 * Ejercita el parsing real de la respuesta GraphQL de {@code MediaListCollection}
 * (Story 2.1, code-review patch #2): {@code SyncServiceTest} mockea
 * {@link AniListMediaListClient} por completo, así que la lógica de
 * mapeo/filtrado -- {@code fetchMediaList}/{@code appendMapped}/{@code mapStatus}
 * -- nunca se ejercitaba de verdad en el repo. Usa el constructor
 * package-private que recibe un {@link RestClient} ya construido, atado acá a
 * un {@link MockRestServiceServer} en vez de hacer una llamada de red real.
 */
class AniListMediaListClientTest {

    private static final String GRAPHQL_ENDPOINT = "http://anilist-test.invalid/graphql";

    /**
     * Respuesta realista con: una entrada válida (CURRENT), una con
     * {@code status: "PAUSED"} (sin equivalente en {@link TrackingStatus} --
     * debe descartarse), una con {@code progress: null} (debe descartarse) y
     * una con {@code media: null} (debe descartarse) -- todas repartidas en más
     * de una lista, como puede devolver AniList cuando el usuario tiene listas
     * personalizadas además de las de status.
     */
    private static final String REALISTIC_RESPONSE_BODY = """
            {
              "data": {
                "MediaListCollection": {
                  "lists": [
                    {
                      "entries": [
                        {"status": "CURRENT", "progress": 5, "media": {"id": 10}},
                        {"status": "PAUSED", "progress": 2, "media": {"id": 99}}
                      ]
                    },
                    {
                      "entries": [
                        {"status": "COMPLETED", "progress": null, "media": {"id": 30}},
                        {"status": "DROPPED", "progress": 1, "media": null},
                        {"status": "COMPLETED", "progress": 12, "media": {"id": 20}}
                      ]
                    }
                  ]
                }
              }
            }
            """;

    private AniListMediaListClient clientBackedBy(String responseBody) {
        RestClient.Builder builder = RestClient.builder().baseUrl(GRAPHQL_ENDPOINT);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GRAPHQL_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
        return new AniListMediaListClient(builder.build());
    }

    @Test
    void mapsOnlyEntriesWithARecognizedStatusAndNonNullProgressAndMedia() {
        AniListMediaListClient client = clientBackedBy(REALISTIC_RESPONSE_BODY);

        List<AniListMediaListEntry> entries = client.fetchMediaList(123L, "test-access-token");

        assertThat(entries).containsExactlyInAnyOrder(
                new AniListMediaListEntry(10L, TrackingStatus.CURRENT, 5),
                new AniListMediaListEntry(20L, TrackingStatus.COMPLETED, 12));
    }

    @Test
    void emptyListsCollectionYieldsAnEmptyResult() {
        String responseWithNoLists = """
                {
                  "data": {
                    "MediaListCollection": {
                      "lists": []
                    }
                  }
                }
                """;
        AniListMediaListClient client = clientBackedBy(responseWithNoLists);

        List<AniListMediaListEntry> entries = client.fetchMediaList(123L, "test-access-token");

        assertThat(entries).isEmpty();
    }

    @Test
    void malformedResponseWithoutAMediaListCollectionThrows() {
        String malformedResponse = """
                {
                  "data": {}
                }
                """;
        AniListMediaListClient client = clientBackedBy(malformedResponse);

        assertThatThrownBy(() -> client.fetchMediaList(123L, "test-access-token"))
                .isInstanceOf(IllegalStateException.class);
    }
}
