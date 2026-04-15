package com.foonbot.aqi.repository;

import com.foonbot.aqi.model.AirQualityRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AirQualityRepository extends JpaRepository<AirQualityRecord, Long> {

    // Get the 10 most recent records (for history endpoint)
    List<AirQualityRecord> findTop10ByOrderByFetchedAtDesc();

    // Get the single most recent record (for latest endpoint)
    AirQualityRecord findTopByOrderByFetchedAtDesc();

    Page<AirQualityRecord> findByLineUserLineUserIdOrderByFetchedAtDesc(String lineUserId, Pageable pageable);
}
