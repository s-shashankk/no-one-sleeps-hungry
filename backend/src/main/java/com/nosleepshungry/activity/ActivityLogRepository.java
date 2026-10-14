package com.nosleepshungry.activity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop200ByOrderByCreatedAtDesc();
}
