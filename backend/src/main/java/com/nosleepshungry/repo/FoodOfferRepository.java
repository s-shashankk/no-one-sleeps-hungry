package com.nosleepshungry.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nosleepshungry.model.FoodOffer;

public interface FoodOfferRepository extends JpaRepository<FoodOffer, Long> {
    long countByStatus(FoodOffer.Status status);

    List<FoodOffer> findAllByOrderByCreatedAtDesc();

    List<FoodOffer> findByStatusOrderByCreatedAtDesc(FoodOffer.Status status);

    @Modifying
    @Query("update FoodOffer f set f.status = :to where f.status = :from and f.expiresAt < :now")
    int expire(@Param("from") FoodOffer.Status from, @Param("to") FoodOffer.Status to,
               @Param("now") LocalDateTime now);
}
