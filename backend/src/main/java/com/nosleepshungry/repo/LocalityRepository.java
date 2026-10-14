package com.nosleepshungry.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nosleepshungry.model.Locality;

public interface LocalityRepository extends JpaRepository<Locality, Long> {
    List<Locality> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);
}
