package com.mealup.vendors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.common.exception.BadRequestException;
import com.mealup.common.exception.NotFoundException;
import com.mealup.common.time.Utc;
import com.mealup.common.validation.Phones;
import com.mealup.common.validation.Text;
import com.mealup.vendors.VendorDtos.VendorRequest;
import com.mealup.vendors.VendorDtos.VendorResponse;

import jakarta.persistence.criteria.Predicate;

/** The vendor list. Vendors are switched off (inactive), never deleted. */
@Service
public class VendorService {

    private final VendorRepository vendors;
    private final AccessPolicy access;

    public VendorService(VendorRepository vendors, AccessPolicy access) {
        this.vendors = vendors;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public Page<VendorResponse> list(String q, Boolean active, Pageable pageable) {
        access.requireUser();
        Specification<Vendor> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (active != null) p.add(cb.equal(root.get("active"), active));
            if (q != null && !q.isBlank()) {
                String text = "%" + q.trim().toLowerCase() + "%";
                String phone = "%" + Phones.normaliseForSearch(q.trim()) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), text),
                        cb.like(cb.lower(root.get("location")), text),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("contactPerson"), "")), text),
                        cb.like(root.get("phoneNumber"), phone)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return vendors.findAll(spec, pageable).map(VendorService::toResponse);
    }

    @Transactional(readOnly = true)
    public VendorResponse get(Long id) {
        access.requireUser();
        return toResponse(find(id));
    }

    @Transactional
    public VendorResponse create(VendorRequest req) {
        access.requireUser();
        Vendor v = new Vendor(req.name().trim(), Text.clean(req.contactPerson()), validPhone(req.phoneNumber()),
                req.location().trim(), Text.clean(req.notes()));
        if (req.active() != null) v.setActive(req.active());
        return toResponse(vendors.save(v));
    }

    @Transactional
    public VendorResponse update(Long id, VendorRequest req) {
        access.requireUser();
        Vendor v = find(id);
        v.change(req.name().trim(), Text.clean(req.contactPerson()), validPhone(req.phoneNumber()),
                req.location().trim(), Text.clean(req.notes()));
        if (req.active() != null) v.setActive(req.active());
        return toResponse(vendors.saveAndFlush(v));
    }

    private Vendor find(Long id) {
        return vendors.findById(id).orElseThrow(() -> new NotFoundException("Vendor not found."));
    }

    static String validPhone(String raw) {
        String phone = Phones.normalise(raw);
        if (!Phones.isValid(phone)) {
            throw new BadRequestException("Enter a valid phone number (10 to 15 digits, optional leading +).");
        }
        return phone;
    }

    private static VendorResponse toResponse(Vendor v) {
        return new VendorResponse(v.getId(), v.getName(), v.getContactPerson(), v.getPhoneNumber(), v.getLocation(),
                v.getNotes(), v.isActive(), Utc.toInstant(v.getCreatedAt()));
    }
}
