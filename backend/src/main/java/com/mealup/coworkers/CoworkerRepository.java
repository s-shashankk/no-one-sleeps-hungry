package com.mealup.coworkers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CoworkerRepository extends JpaRepository<Coworker, Long>, JpaSpecificationExecutor<Coworker> {

    long countByActiveTrue();

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);
}
