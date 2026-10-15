package com.mealup.requests;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FoodRequestRepository extends JpaRepository<FoodRequest, Long>, JpaSpecificationExecutor<FoodRequest> {

    long countByStatus(RequestStatus status);

    /** Meals delivered across all completed "need food" requests. */
    @Query("select coalesce(sum(r.mealsDelivered), 0L) from FoodRequest r "
            + "where r.type = com.mealup.requests.RequestType.NEED_FOOD "
            + "and r.status = com.mealup.requests.RequestStatus.COMPLETED")
    Long totalMealsDelivered();
}
