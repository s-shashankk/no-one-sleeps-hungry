package com.mealup.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.common.exception.NotFoundException;
import com.mealup.common.time.Utc;
import com.mealup.common.validation.Text;
import com.mealup.feedback.FeedbackDtos.*;
import com.mealup.users.User;
import com.mealup.users.UserRepository;

/** Volunteers and hotel staff write to the admin; the admin reads and marks them done. */
@Service
public class FeedbackService {

    private final FeedbackRepository repo;
    private final UserRepository users;
    private final AccessPolicy access;

    public FeedbackService(FeedbackRepository repo, UserRepository users, AccessPolicy access) {
        this.repo = repo;
        this.users = users;
        this.access = access;
    }

    @Transactional
    public MyMessage send(SendRequest req) {
        var me = access.requireAccountHolder();
        User u = users.findById(me.id()).orElseThrow(() -> new NotFoundException("Account not found."));
        Integer rating = req.kind() == FeedbackKind.FEEDBACK ? req.rating() : null;
        String body = Text.clean(req.body());
        if (body == null) throw new com.mealup.common.exception.BadRequestException("Please write something first.");
        return mine(repo.save(new Feedback(u, req.kind(), rating, body)));
    }

    @Transactional(readOnly = true)
    public Page<MyMessage> myMessages(Pageable pageable) {
        var me = access.requireAccountHolder();
        return repo.findByUser_Id(me.id(), pageable).map(FeedbackService::mine);
    }

    @Transactional(readOnly = true)
    public Page<AdminMessage> list(FeedbackStatus status, Pageable pageable) {
        access.requireUser();
        return (status == null ? repo.findAll(pageable) : repo.findByStatus(status, pageable)).map(FeedbackService::admin);
    }

    @Transactional
    public AdminMessage done(Long id) {
        access.requireUser();
        Feedback f = find(id);
        f.markDone();
        return admin(repo.saveAndFlush(f));
    }

    @Transactional
    public AdminMessage reopen(Long id) {
        access.requireUser();
        Feedback f = find(id);
        f.reopen();
        return admin(repo.saveAndFlush(f));
    }

    private Feedback find(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Message not found."));
    }

    private static MyMessage mine(Feedback f) {
        return new MyMessage(f.getId(), f.getKind(), f.getRating(), f.getBody(), f.getStatus(), Utc.toInstant(f.getCreatedAt()));
    }

    private static AdminMessage admin(Feedback f) {
        User u = f.getUser();
        return new AdminMessage(f.getId(), f.getKind(), f.getRating(), f.getBody(), f.getStatus(),
                u.getId(), u.getFullName(), u.getEmail(), u.getPhoneNumber(), u.getRole(),
                u.getVendor() == null ? null : u.getVendor().getName(),
                Utc.toInstant(f.getCreatedAt()), Utc.toInstant(f.getHandledAt()));
    }
}
