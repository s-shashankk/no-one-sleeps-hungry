package com.mealup.requests;

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
import com.mealup.requests.RequestDtos.*;

import jakarta.persistence.criteria.Predicate;

/** Requests: view, add, correct, and close them (completed / accepted / rejected / reopened). */
@Service
public class RequestService {

    static final String THANKS_NEED = "Thank you. Our team will check this and call if needed.";
    static final String THANKS_SURPLUS = "Thank you. Our team will call you to arrange the pickup.";

    private final FoodRequestRepository requests;
    private final AccessPolicy access;

    public RequestService(FoodRequestRepository requests, AccessPolicy access) {
        this.requests = requests;
        this.access = access;
    }

    // ---- admin ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<RequestResponse> list(RequestType type, RequestStatus status, String q, Pageable pageable) {
        access.requireUser();
        Specification<FoodRequest> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (type != null) p.add(cb.equal(root.get("type"), type));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (q != null && !q.isBlank()) {
                String text = "%" + q.trim().toLowerCase() + "%";
                String phone = "%" + Phones.normaliseForSearch(q.trim()) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("location")), text),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("contactName"), "")), text),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("details"), "")), text),
                        cb.like(cb.coalesce(root.<String>get("contactPhone"), ""), phone)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return requests.findAll(spec, pageable).map(RequestService::toResponse);
    }

    @Transactional(readOnly = true)
    public RequestResponse get(Long id) {
        access.requireUser();
        return toResponse(find(id));
    }

    @Transactional
    public RequestResponse create(CreateRequest req) {
        access.requireUser();
        FoodRequest r = new FoodRequest(req.type(), Text.clean(req.contactName()), optionalPhone(req.contactPhone()),
                req.location().trim(), req.quantity(), Text.clean(req.details()));
        return toResponse(requests.save(r));
    }

    @Transactional
    public RequestResponse update(Long id, UpdateRequest req) {
        access.requireUser();
        FoodRequest r = find(id);
        r.editDetails(Text.clean(req.contactName()), optionalPhone(req.contactPhone()), req.location().trim(),
                req.quantity(), Text.clean(req.details()));
        return toResponse(requests.saveAndFlush(r));
    }

    /** Food was delivered: record how many meals. Closes a "need food" request. */
    @Transactional
    public RequestResponse complete(Long id, CompleteRequest req) {
        access.requireUser();
        FoodRequest r = find(id);
        r.complete(req.mealsDelivered(), Text.clean(req.note()));
        return toResponse(requests.saveAndFlush(r));
    }

    /** Surplus food accepted: a team will be sent to collect it. */
    @Transactional
    public RequestResponse accept(Long id, NoteRequest req) {
        access.requireUser();
        FoodRequest r = find(id);
        r.accept(Text.clean(req.note()));
        return toResponse(requests.saveAndFlush(r));
    }

    /** "Unable to serve" / "unable to collect": the request goes off the pending list. */
    @Transactional
    public RequestResponse reject(Long id, NoteRequest req) {
        access.requireUser();
        FoodRequest r = find(id);
        r.reject(Text.clean(req.note()));
        return toResponse(requests.saveAndFlush(r));
    }

    @Transactional
    public RequestResponse reopen(Long id) {
        access.requireUser();
        FoodRequest r = find(id);
        r.reopen();
        return toResponse(requests.saveAndFlush(r));
    }

    // ---- public forms (no sign-in; they only ever create pending requests) ------------------------

    @Transactional
    public PublicSubmissionResponse submitNeedFood(PublicNeedFoodRequest req) {
        if (isBot(req.website())) {
            return new PublicSubmissionResponse(THANKS_NEED);       // pretend it worked; store nothing
        }
        requests.save(new FoodRequest(RequestType.NEED_FOOD, Text.clean(req.name()), optionalPhone(req.phone()),
                req.location().trim(), req.people(), Text.clean(req.details())));
        return new PublicSubmissionResponse(THANKS_NEED);
    }

    @Transactional
    public PublicSubmissionResponse submitSurplus(PublicSurplusRequest req) {
        if (isBot(req.website())) {
            return new PublicSubmissionResponse(THANKS_SURPLUS);
        }
        requests.save(new FoodRequest(RequestType.SURPLUS_FOOD, req.name().trim(), optionalPhone(req.phone()),
                req.location().trim(), req.meals(), Text.clean(req.details())));
        return new PublicSubmissionResponse(THANKS_SURPLUS);
    }

    // ---- helpers ---------------------------------------------------------------------------

    private FoodRequest find(Long id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Request not found."));
    }

    /** The hidden "website" field is for bots: people never see it, so any value means a bot. */
    private static boolean isBot(String honeypot) {
        return honeypot != null && !honeypot.isBlank();
    }

    private static String optionalPhone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String phone = Phones.normalise(raw);
        if (!Phones.isValid(phone)) {
            throw new BadRequestException("Enter a valid phone number (10 to 15 digits, optional leading +).");
        }
        return phone;
    }

    private static RequestResponse toResponse(FoodRequest r) {
        return new RequestResponse(r.getId(), r.getType(), r.getStatus(), r.getContactName(), r.getContactPhone(),
                r.getLocation(), r.getQuantity(), r.getDetails(), r.getMealsDelivered(), r.getNote(),
                Utc.toInstant(r.getCreatedAt()), Utc.toInstant(r.getUpdatedAt()), Utc.toInstant(r.getClosedAt()));
    }
}
