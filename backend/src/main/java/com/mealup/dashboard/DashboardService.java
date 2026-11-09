package com.mealup.dashboard;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.areas.AreaRepository;
import com.mealup.coworkers.CoworkerRepository;
import com.mealup.dashboard.DashboardDtos.Summary;
import com.mealup.joins.JoinRequestRepository;
import com.mealup.joins.JoinStatus;
import com.mealup.pickups.PickupRepository;
import com.mealup.pickups.PickupStatus;
import com.mealup.publicapi.ImpactService;
import com.mealup.requests.FoodRequestRepository;
import com.mealup.requests.RequestStatus;
import com.mealup.users.Role;
import com.mealup.users.UserRepository;
import com.mealup.vendors.VendorRepository;

/** The numbers on the admin home page (and the badges in the admin menu). */
@Service
public class DashboardService {

    private final FoodRequestRepository requests;
    private final VendorRepository vendors;
    private final CoworkerRepository coworkers;
    private final PickupRepository pickups;
    private final JoinRequestRepository joins;
    private final AreaRepository areas;
    private final ImpactService impact;
    private final UserRepository users;
    private final AccessPolicy access;
    private final com.mealup.feedback.FeedbackRepository feedback;

    public DashboardService(FoodRequestRepository requests, VendorRepository vendors, CoworkerRepository coworkers,
                            PickupRepository pickups, JoinRequestRepository joins, AreaRepository areas,
                            ImpactService impact, UserRepository users, AccessPolicy access,
                            com.mealup.feedback.FeedbackRepository feedback) {
        this.feedback = feedback;
        this.users = users;
        this.requests = requests;
        this.vendors = vendors;
        this.coworkers = coworkers;
        this.pickups = pickups;
        this.joins = joins;
        this.areas = areas;
        this.impact = impact;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        access.requireUser();
        return new Summary(impact.totalMealsServed(), vendors.countByActiveTrue(), coworkers.countByActiveTrue(),
                requests.countByStatus(RequestStatus.PENDING), pickups.countByStatus(PickupStatus.PLEDGED),
                joins.countByStatus(JoinStatus.NEW), areas.countByOpenTrue(), areas.countOpenCities(),
                users.countByRole(Role.VOLUNTEER),
                feedback.countByStatus(com.mealup.feedback.FeedbackStatus.NEW));
    }
}
