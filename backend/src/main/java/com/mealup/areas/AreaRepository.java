package com.mealup.areas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByCityIgnoreCaseAndNameIgnoreCase(String city, String name);

    long countByOpenTrue();

    @Query("select count(distinct a.city) from Area a where a.open = true")
    long countOpenCities();
}
