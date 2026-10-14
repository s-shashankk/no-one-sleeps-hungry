package com.nosleepshungry.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nosleepshungry.model.VolunteerApplication;

public interface VolunteerApplicationRepository extends JpaRepository<VolunteerApplication, Long> {
    List<VolunteerApplication> findAllByOrderByCreatedAtDesc();

    List<VolunteerApplication> findByStatusOrderByCreatedAtDesc(VolunteerApplication.Status status);
}
