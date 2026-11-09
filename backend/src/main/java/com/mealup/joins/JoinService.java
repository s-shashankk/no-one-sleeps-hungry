package com.mealup.joins;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.common.exception.BadRequestException;
import com.mealup.common.exception.NotFoundException;
import com.mealup.common.time.Utc;
import com.mealup.common.validation.Phones;
import com.mealup.common.validation.Text;
import com.mealup.joins.JoinDtos.JoinResponse;
import com.mealup.joins.JoinDtos.PublicJoinRequest;
import com.mealup.publicapi.PublicDtos.PublicMessage;

/** Requests to join the community: arrive from the public site, the admin calls them and marks them contacted. */
@Service
public class JoinService {

    static final String THANKS = "Welcome! Our team will call you soon to say hello and tell you how you can help.";

    private final JoinRequestRepository joins;
    private final AccessPolicy access;

    public JoinService(JoinRequestRepository joins, AccessPolicy access) {
        this.joins = joins;
        this.access = access;
    }

    @Transactional
    public PublicMessage submit(PublicJoinRequest req) {
        if (req.website() != null && !req.website().isBlank()) {
            return new PublicMessage(THANKS);
        }
        String phone = Phones.normalise(req.phone());
        if (!Phones.isValid(phone)) {
            throw new BadRequestException("Enter a valid phone number (10 to 15 digits, optional leading +).");
        }
        joins.save(new JoinRequest(req.name().trim(), phone, Text.clean(req.email()), Text.clean(req.location()),
                Text.clean(req.message())));
        return new PublicMessage(THANKS);
    }

    @Transactional(readOnly = true)
    public Page<JoinResponse> list(JoinStatus status, Pageable pageable) {
        access.requireUser();
        Page<JoinRequest> page = status == null ? joins.findAll(pageable) : joins.findByStatus(status, pageable);
        return page.map(JoinService::toResponse);
    }

    @Transactional
    public JoinResponse markContacted(Long id) {
        access.requireUser();
        JoinRequest j = find(id);
        j.markContacted();
        return toResponse(joins.saveAndFlush(j));
    }

    @Transactional
    public JoinResponse reopen(Long id) {
        access.requireUser();
        JoinRequest j = find(id);
        j.reopen();
        return toResponse(joins.saveAndFlush(j));
    }

    private JoinRequest find(Long id) {
        return joins.findById(id).orElseThrow(() -> new NotFoundException("Join request not found."));
    }

    private static JoinResponse toResponse(JoinRequest j) {
        return new JoinResponse(j.getId(), j.getStatus(), j.getName(), j.getPhoneNumber(), j.getEmail(),
                j.getLocation(), j.getMessage(), Utc.toInstant(j.getCreatedAt()), Utc.toInstant(j.getContactedAt()));
    }
}
