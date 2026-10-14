package com.nosleepshungry.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nosleepshungry.model.HungerReport;

public interface HungerReportRepository extends JpaRepository<HungerReport, Long> {
    long countByStatus(HungerReport.Status status);

    List<HungerReport> findAllByOrderByCreatedAtDesc();

    List<HungerReport> findByStatusOrderByCreatedAtDesc(HungerReport.Status status);
}
