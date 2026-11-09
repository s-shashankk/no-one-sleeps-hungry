package com.mealup.pickups;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickupRepository extends JpaRepository<Pickup, Long> {

    boolean existsByVendor_IdAndNote(Long vendorId, String note);

    Page<Pickup> findByStatus(PickupStatus status, Pageable pageable);

    /** Open bookings created before a moment (used to close the ones nobody collected). */
    List<Pickup> findByStatusInAndCreatedAtBefore(Collection<PickupStatus> statuses, LocalDateTime before);

    long countByStatus(PickupStatus status);

    Page<Pickup> findByVolunteer_Id(Long userId, Pageable pageable);

    /** Pickups still in progress for one volunteer (used to stop someone pledging many pickups at once). */
    long countByVolunteer_IdAndStatusIn(Long userId, java.util.Collection<PickupStatus> statuses);

    /** Pickups booked at one hotel since a moment (cancelled ones do not use up the day's places). */
    long countByVendor_IdAndStatusNotAndCreatedAtGreaterThanEqual(Long vendorId, PickupStatus status, LocalDateTime since);

    /** Plates already promised at one hotel since a moment: the people volunteers said they will feed (cancelled bookings give their plates back). */
    @Query("select coalesce(sum(p.peopleCount), 0L) from Pickup p where p.vendor.id = :vendorId "
            + "and p.status <> com.mealup.pickups.PickupStatus.CANCELLED and p.createdAt >= :since")
    long platesBookedSince(@Param("vendorId") Long vendorId, @Param("since") LocalDateTime since);

    Page<Pickup> findByVendor_IdAndStatusIn(Long vendorId, Collection<PickupStatus> statuses, Pageable pageable);

    Page<Pickup> findByVendor_Id(Long vendorId, Pageable pageable);

    long countByVendor_IdAndStatusIn(Long vendorId, Collection<PickupStatus> statuses);

    /** [vendorId, plates booked today] for the hotels of one area. */
    @Query("select p.vendor.id, coalesce(sum(p.peopleCount), 0L) from Pickup p where p.area.id = :areaId "
            + "and p.status <> com.mealup.pickups.PickupStatus.CANCELLED and p.createdAt >= :since group by p.vendor.id")
    List<Object[]> bookingsSince(@Param("areaId") Long areaId, @Param("since") LocalDateTime since);

    /** Plates served across all completed pickups. */
    @Query("select coalesce(sum(p.platesServed), 0L) from Pickup p "
            + "where p.status = com.mealup.pickups.PickupStatus.COMPLETED")
    Long totalPlatesServed();

    /** [areaId, plates served] for completed pickups, one row per area. */
    @Query("select p.area.id, coalesce(sum(p.platesServed), 0L) from Pickup p "
            + "where p.status = com.mealup.pickups.PickupStatus.COMPLETED group by p.area.id")
    List<Object[]> platesServedByArea();
}
