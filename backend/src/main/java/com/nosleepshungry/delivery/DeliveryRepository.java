package com.nosleepshungry.delivery;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findAllByOrderByCreatedAtDesc();

    List<Delivery> findByStatusOrderByCreatedAtDesc(Delivery.Status status);
}
