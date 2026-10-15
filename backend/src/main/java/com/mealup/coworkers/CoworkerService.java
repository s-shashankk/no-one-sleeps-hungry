package com.mealup.coworkers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.common.exception.BadRequestException;
import com.mealup.common.exception.ConflictException;
import com.mealup.common.exception.NotFoundException;
import com.mealup.common.time.Utc;
import com.mealup.common.validation.Phones;
import com.mealup.common.validation.Text;
import com.mealup.coworkers.CoworkerDtos.CoworkerRequest;
import com.mealup.coworkers.CoworkerDtos.CoworkerResponse;

import jakarta.persistence.criteria.Predicate;

/** The coworker list. People are switched off (inactive), never deleted. */
@Service
public class CoworkerService {

    private final CoworkerRepository coworkers;
    private final AccessPolicy access;

    public CoworkerService(CoworkerRepository coworkers, AccessPolicy access) {
        this.coworkers = coworkers;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public Page<CoworkerResponse> list(String q, Boolean active, Pageable pageable) {
        access.requireUser();
        Specification<Coworker> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (active != null) p.add(cb.equal(root.get("active"), active));
            if (q != null && !q.isBlank()) {
                String text = "%" + q.trim().toLowerCase() + "%";
                String phone = "%" + Phones.normaliseForSearch(q.trim()) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), text),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("location"), "")), text),
                        cb.like(root.get("phoneNumber"), phone)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return coworkers.findAll(spec, pageable).map(CoworkerService::toResponse);
    }

    @Transactional(readOnly = true)
    public CoworkerResponse get(Long id) {
        access.requireUser();
        return toResponse(find(id));
    }

    @Transactional
    public CoworkerResponse create(CoworkerRequest req) {
        access.requireUser();
        String phone = validPhone(req.phoneNumber());
        if (coworkers.existsByPhoneNumber(phone)) {
            throw new ConflictException("A coworker with this phone number already exists.");
        }
        Coworker c = new Coworker(req.name().trim(), phone, req.location().trim(), Text.clean(req.notes()));
        if (req.active() != null) c.setActive(req.active());
        return toResponse(coworkers.save(c));
    }

    @Transactional
    public CoworkerResponse update(Long id, CoworkerRequest req) {
        access.requireUser();
        Coworker c = find(id);
        String phone = validPhone(req.phoneNumber());
        if (coworkers.existsByPhoneNumberAndIdNot(phone, id)) {
            throw new ConflictException("Another coworker already has this phone number.");
        }
        c.change(req.name().trim(), phone, req.location().trim(), Text.clean(req.notes()));
        if (req.active() != null) c.setActive(req.active());
        return toResponse(coworkers.saveAndFlush(c));
    }

    private Coworker find(Long id) {
        return coworkers.findById(id).orElseThrow(() -> new NotFoundException("Coworker not found."));
    }

    private static String validPhone(String raw) {
        String phone = Phones.normalise(raw);
        if (!Phones.isValid(phone)) {
            throw new BadRequestException("Enter a valid phone number (10 to 15 digits, optional leading +).");
        }
        return phone;
    }

    private static CoworkerResponse toResponse(Coworker c) {
        return new CoworkerResponse(c.getId(), c.getName(), c.getPhoneNumber(), c.getLocation(), c.getNotes(),
                c.isActive(), Utc.toInstant(c.getCreatedAt()));
    }
}
