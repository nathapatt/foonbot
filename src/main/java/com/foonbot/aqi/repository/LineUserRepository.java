package com.foonbot.aqi.repository;

import com.foonbot.aqi.model.LineUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LineUserRepository extends JpaRepository<LineUser, String> {

    Page<LineUser> findByNotifyEnabledTrueAndLastLatIsNotNullAndLastLonIsNotNull(Pageable pageable);
}
