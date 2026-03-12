package com.aifm.repository;

import com.aifm.model.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {

    List<FocusSession> findByTaskId(Long taskId);

    @Query("SELECT COALESCE(SUM(s.duration), 0) FROM FocusSession s")
    int sumTotalMinutes();

    @Query("SELECT COALESCE(SUM(s.distractions), 0) FROM FocusSession s")
    int sumTotalDistractions();
}