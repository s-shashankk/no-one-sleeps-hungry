package com.mealup.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.coworkers.CoworkerRepository;
import com.mealup.dashboard.DashboardDtos.Summary;
import com.mealup.requests.FoodRequestRepository;
import com.mealup.requests.RequestStatus;
import com.mealup.vendors.VendorRepository;

/** The four numbers on the admin home page. */
@Service
public class DashboardService {

    private final FoodRequestRepository requests;
    private final VendorRepository vendors;
    private final CoworkerRepository coworkers;
    private final AccessPolicy access;

    public DashboardService(FoodRequestRepository requests, VendorRepository vendors,
                            CoworkerRepository coworkers, AccessPolicy access) {
        this.requests = requests;
        this.vendors = vendors;
        this.coworkers = coworkers;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        access.requireUser();
        Long meals = requests.totalMealsDelivered();
        return new Summary(meals == null ? 0 : meals, vendors.countByActiveTrue(), coworkers.countByActiveTrue(),
                requests.countByStatus(RequestStatus.PENDING));
    }
}
