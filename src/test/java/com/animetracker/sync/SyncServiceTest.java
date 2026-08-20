package com.animetracker.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.animetracker.domain.AppUser;
import com.animetracker.domain.AppUserRepository;
import com.animetracker.domain.Snapshot;
import com.animetracker.domain.TrackingEntry;
import com.animetracker.domain.TrackingStatus;
import com.animetracker.integration.anilist.AniListMediaListClient;
import com.animetracker.integration.anilist.AniListMediaListEntry;

/**
 * Cubre las 5 filas de la I/O & Edge-Case Matrix de la spec 2.1 contra un
 * Postgres real vía Testcontainers (Flyway aplica V1 + V2 al arrancar el
 * contexto) con {@link AniListMediaListClient} mockeado -- el punto exacto
 * donde la anti-corrupción de AD-1 aísla a SyncService de la red real.
 *
 * Los tests comparten el mismo Postgres del container sin rollback entre
 * métodos (ninguno es {@code @Transactional}), así que toda aserción sobre
 * TrackingEntry/Snapshot está deliberadamente scopeada por appUserId -- cada
 * test usa su propio anilistUserId -- en vez de leer las tablas completas.
 */
@SpringBootTest
@Testcontainers
class SyncServiceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String ACCESS_TOKEN = "test-access-token";

    @Autowired
    private SyncService syncService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private TrackingEntryRepository trackingEntryRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @MockitoBean
    private AniListMediaListClient aniListMediaListClient;

    private AppUser newAppUser(long anilistUserId) {
        return appUserRepository.save(new AppUser(anilistUserId, AppUser.DEFAULT_THEME_PREFERENCE));
    }

    private void mockRemoteList(Long anilistUserId, List<AniListMediaListEntry> entries) {
        when(aniListMediaListClient.fetchMediaList(eq(anilistUserId), any())).thenReturn(entries);
    }

    private List<TrackingEntry> entriesFor(AppUser appUser) {
        return trackingEntryRepository.findByAppUserId(appUser.getId());
    }

    private List<Snapshot> snapshotsFor(AppUser appUser) {
        return snapshotRepository.findByTrackingEntry_AppUser_Id(appUser.getId());
    }

    @Test
    void firstSyncOfANewUserCreatesAllActiveEntriesWithOneSnapshotEach() {
        AppUser appUser = newAppUser(9_101L);
        mockRemoteList(appUser.getAnilistUserId(), List.of(
                new AniListMediaListEntry(1L, TrackingStatus.CURRENT, 5),
                new AniListMediaListEntry(2L, TrackingStatus.PLANNING, 0),
                new AniListMediaListEntry(3L, TrackingStatus.COMPLETED, 12)));

        syncService.syncUser(appUser, ACCESS_TOKEN);

        List<TrackingEntry> entries = entriesFor(appUser);
        assertThat(entries).hasSize(3);
        assertThat(entries).allMatch(TrackingEntry::isActive);
        assertThat(snapshotsFor(appUser)).hasSize(3);
    }

    @Test
    void recurringSyncDeactivatesAnEntryWhoseMediaNoLongerAppearsWithoutCreatingANewSnapshotForIt() {
        AppUser appUser = newAppUser(9_102L);
        mockRemoteList(appUser.getAnilistUserId(), List.of(
                new AniListMediaListEntry(10L, TrackingStatus.CURRENT, 3),
                new AniListMediaListEntry(20L, TrackingStatus.CURRENT, 1)));
        syncService.syncUser(appUser, ACCESS_TOKEN);
        assertThat(snapshotsFor(appUser)).hasSize(2);

        // Segunda corrida: el media 20 ya no aparece en la respuesta de AniList.
        mockRemoteList(appUser.getAnilistUserId(), List.of(new AniListMediaListEntry(10L, TrackingStatus.CURRENT, 4)));
        syncService.syncUser(appUser, ACCESS_TOKEN);

        List<TrackingEntry> entries = entriesFor(appUser);
        assertThat(entries).hasSize(2);

        TrackingEntry removedEntry = entries.stream().filter(e -> e.getAnilistMediaId().equals(20L)).findFirst()
                .orElseThrow();
        assertThat(removedEntry.isActive()).isFalse();

        TrackingEntry stillPresentEntry = entries.stream().filter(e -> e.getAnilistMediaId().equals(10L)).findFirst()
                .orElseThrow();
        assertThat(stillPresentEntry.isActive()).isTrue();

        // Solo el media 10 recibió un segundo Snapshot en la segunda corrida; el
        // media 20 (dado de baja) no generó uno nuevo: 2 de la primera corrida + 1.
        assertThat(snapshotsFor(appUser)).hasSize(3);
    }

    @Test
    void recurringSyncWithHigherProgressUpdatesTheSameRowInPlaceAndAddsANewSnapshot() {
        AppUser appUser = newAppUser(9_103L);
        mockRemoteList(appUser.getAnilistUserId(), List.of(new AniListMediaListEntry(30L, TrackingStatus.CURRENT, 5)));
        syncService.syncUser(appUser, ACCESS_TOKEN);
        Long trackingEntryId = entriesFor(appUser).get(0).getId();

        mockRemoteList(appUser.getAnilistUserId(), List.of(new AniListMediaListEntry(30L, TrackingStatus.CURRENT, 8)));
        syncService.syncUser(appUser, ACCESS_TOKEN);

        List<TrackingEntry> entries = entriesFor(appUser);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getId()).isEqualTo(trackingEntryId);
        assertThat(entries.get(0).getLastEpisode()).isEqualTo(8);
        assertThat(entries.get(0).isActive()).isTrue();

        List<Snapshot> snapshots = snapshotsFor(appUser);
        assertThat(snapshots).hasSize(2);
        assertThat(snapshots).extracting(Snapshot::getEpisodeProgress).containsExactlyInAnyOrder(5, 8);
    }

    @Test
    void entryWithAnUnmappedStatusIsOmittedFromTheUpsertOfThisRun() {
        AppUser appUser = newAppUser(9_104L);
        // El cliente de AniList ya filtra los status no mapeados (p.ej. PAUSED) antes
        // de construir AniListMediaListEntry -- desde la perspectiva de SyncService,
        // esa entrada simplemente está ausente de la respuesta de esta corrida.
        mockRemoteList(appUser.getAnilistUserId(), List.of(new AniListMediaListEntry(40L, TrackingStatus.CURRENT, 2)));

        syncService.syncUser(appUser, ACCESS_TOKEN);

        List<TrackingEntry> entries = entriesFor(appUser);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getAnilistMediaId()).isEqualTo(40L);
        assertThat(entries.stream().anyMatch(e -> e.getAnilistMediaId().equals(50L))).isFalse();
    }

    @Test
    void networkFailureTowardsAniListPersistsNothingAndPropagatesTheException() {
        AppUser appUser = newAppUser(9_105L);
        when(aniListMediaListClient.fetchMediaList(eq(appUser.getAnilistUserId()), any()))
                .thenThrow(new RuntimeException("AniList timeout"));

        assertThatThrownBy(() -> syncService.syncUser(appUser, ACCESS_TOKEN)).isInstanceOf(RuntimeException.class);

        assertThat(entriesFor(appUser)).isEmpty();
        assertThat(snapshotsFor(appUser)).isEmpty();
    }

    @Test
    void duplicateRemoteEntriesForTheSameMediaAreDeduplicatedIntoASingleTrackingEntry() {
        // MediaListCollection puede repetir un media entre las listas por status y
        // listas personalizadas de AniList; sin deduplicar, el segundo upsert de esta
        // corrida violaría el UNIQUE (app_user_id, anilist_media_id) y tumbaría la
        // transacción completa.
        AppUser appUser = newAppUser(9_106L);
        mockRemoteList(appUser.getAnilistUserId(), List.of(
                new AniListMediaListEntry(60L, TrackingStatus.CURRENT, 3),
                new AniListMediaListEntry(60L, TrackingStatus.CURRENT, 7)));

        syncService.syncUser(appUser, ACCESS_TOKEN);

        List<TrackingEntry> entries = entriesFor(appUser);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getLastEpisode()).isEqualTo(7);
        assertThat(snapshotsFor(appUser)).hasSize(1);
    }
}
