package com.mealup.vendors;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface VendorRepository extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {

    long countByActiveTrue();

    boolean existsByNameIgnoreCase(String name);

    java.util.Optional<Vendor> findFirstByNameIgnoreCase(String name);

    /** Loads the hotel and locks its row, so two volunteers booking at the same moment cannot both take the last place of the day. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vendor v where v.id = :id")
    java.util.Optional<Vendor> findByIdForUpdate(@Param("id") Long id);

    /** Every active vendor, for the public "our partners" slider. */
    List<Vendor> findByActiveTrueOrderByNameAsc();

    /** Active vendors in one area, for the public "where can I collect food" list. */
    List<Vendor> findByArea_IdAndActiveTrueOrderByNameAsc(Long areaId);

    /** [areaId, number of active vendors] for every area that has at least one. */
    @Query("select v.area.id, count(v) from Vendor v where v.active = true and v.area is not null group by v.area.id")
    List<Object[]> activeCountsByArea();
}
