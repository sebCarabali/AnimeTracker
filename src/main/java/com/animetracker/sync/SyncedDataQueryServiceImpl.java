package com.animetracker.sync;

import java.util.List;

import org.springframework.stereotype.Service;

import com.animetracker.domain.TrackingEntry;

@Service
class SyncedDataQueryServiceImpl implements SyncedDataQueryService {

    private final TrackingEntryRepository trackingEntryRepository;

    SyncedDataQueryServiceImpl(TrackingEntryRepository trackingEntryRepository) {
        this.trackingEntryRepository = trackingEntryRepository;
    }

    @Override
    public List<TrackingEntry> findActiveEntries(Long appUserId) {
        return trackingEntryRepository.findByAppUserIdAndActiveTrue(appUserId);
    }
}
