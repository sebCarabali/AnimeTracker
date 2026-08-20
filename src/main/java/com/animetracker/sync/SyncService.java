package com.animetracker.sync;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.animetracker.domain.AppUser;
import com.animetracker.domain.Snapshot;
import com.animetracker.domain.TrackingEntry;
import com.animetracker.integration.anilist.AniListMediaListClient;
import com.animetracker.integration.anilist.AniListMediaListEntry;

/**
 * Único codepath de sincronización (Epic 2 Technical Decisions): tanto el
 * login-sync (Story 2.1, síncrono) como el job periódico (Story 2.2, todavía
 * no implementado) invocan este mismo método single-user -- no existe una
 * segunda implementación.
 *
 * {@code syncUser} corre en una única transacción: fetch a AniList, upsert +
 * baja lógica de {@link TrackingEntry}, y un {@link Snapshot} por entrada
 * presente en la respuesta, todos con el mismo {@code takenAt} de la corrida.
 * Si {@link AniListMediaListClient} lanza una excepción (red/timeout), la
 * transacción no compromete ningún cambio -- el caller (login-sync) decide
 * cómo tolerar la falla sin bloquear al usuario (AD-7).
 */
@Service
public class SyncService {

    private final AniListMediaListClient aniListMediaListClient;
    private final TrackingEntryRepository trackingEntryRepository;
    private final SnapshotRepository snapshotRepository;

    SyncService(AniListMediaListClient aniListMediaListClient, TrackingEntryRepository trackingEntryRepository,
            SnapshotRepository snapshotRepository) {
        this.aniListMediaListClient = aniListMediaListClient;
        this.trackingEntryRepository = trackingEntryRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public void syncUser(AppUser appUser, String accessToken) {
        List<AniListMediaListEntry> remoteEntries = aniListMediaListClient.fetchMediaList(appUser.getAnilistUserId(),
                accessToken);

        // MediaListCollection puede repetir un mismo media entre las listas por
        // status y listas personalizadas del usuario en AniList; deduplicar acá
        // -- quedándonos con la última ocurrencia -- evita un segundo save() sobre
        // la misma TrackingEntry (o un segundo insert con el mismo
        // app_user_id+anilist_media_id) dentro de la misma corrida, que violaría el
        // UNIQUE de tracking_entry y tumbaría toda la transacción.
        Map<Long, AniListMediaListEntry> dedupedRemoteEntriesByMediaId = new LinkedHashMap<>();
        for (AniListMediaListEntry remoteEntry : remoteEntries) {
            dedupedRemoteEntriesByMediaId.put(remoteEntry.mediaId(), remoteEntry);
        }

        // Todas las TrackingEntry existentes del usuario (activas o no), para poder
        // reactivar in-place una fila que había sido dada de baja y reapareció, sin
        // violar el unique (app_user_id, anilist_media_id) insertando una segunda.
        Map<Long, TrackingEntry> entriesByMediaId = new HashMap<>();
        for (TrackingEntry entry : trackingEntryRepository.findByAppUserId(appUser.getId())) {
            entriesByMediaId.put(entry.getAnilistMediaId(), entry);
        }

        Instant takenAt = Instant.now();

        for (AniListMediaListEntry remoteEntry : dedupedRemoteEntriesByMediaId.values()) {
            TrackingEntry trackingEntry = entriesByMediaId.remove(remoteEntry.mediaId());
            if (trackingEntry == null) {
                trackingEntry = new TrackingEntry(appUser, remoteEntry.mediaId(), remoteEntry.status(),
                        remoteEntry.progress());
                trackingEntryRepository.save(trackingEntry);
            } else {
                // Fila ya gestionada por JPA dentro de esta transacción: el dirty
                // checking de Hibernate persiste el cambio al commit, sin save() explícito.
                trackingEntry.applyRemoteState(remoteEntry.status(), remoteEntry.progress());
            }
            snapshotRepository.save(new Snapshot(trackingEntry, remoteEntry.progress(), takenAt));
        }

        // Lo que quedó en el map no apareció en la respuesta de esta corrida
        // (removido de la lista de AniList, o con un status no mapeado como
        // PAUSED): baja lógica, sin Snapshot para esta corrida.
        for (TrackingEntry staleEntry : entriesByMediaId.values()) {
            staleEntry.deactivate();
        }
    }
}
