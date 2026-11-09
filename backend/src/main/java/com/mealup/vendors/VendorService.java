package com.mealup.vendors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mealup.access.AccessPolicy;
import com.mealup.areas.Area;
import com.mealup.areas.AreaRepository;
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
    static final int MAX_LOGO_BYTES = 500 * 1024;

    private final AreaRepository areas;
    private final LogoStore logos;
    private final AccessPolicy access;

    public VendorService(VendorRepository vendors, AreaRepository areas, LogoStore logos, AccessPolicy access) {
        this.vendors = vendors;
        this.areas = areas;
        this.logos = logos;
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
        Page<Vendor> page = vendors.findAll(spec, pageable);
        Map<Long, Long> versions = logos.versions(page.getContent().stream().map(Vendor::getId).toList());
        return page.map(v -> toResponse(v, versions.get(v.getId())));
    }

    @Transactional(readOnly = true)
    public VendorResponse get(Long id) {
        access.requireUser();
        return respond(find(id));
    }

    @Transactional
    public VendorResponse create(VendorRequest req) {
        access.requireUser();
        Vendor v = new Vendor(req.name().trim(), Text.clean(req.contactPerson()), validPhone(req.phoneNumber()),
                req.location().trim(), Text.clean(req.notes()));
        v.setArea(findArea(req.areaId()));
        if (req.active() != null) v.setActive(req.active());
        if (req.dailyLimit() != null) v.setDailyLimit(req.dailyLimit());
        if (req.mapUrl() != null) v.setMapUrl(cleanMapUrl(req.mapUrl()));
        return respond(vendors.save(v));
    }

    @Transactional
    public VendorResponse update(Long id, VendorRequest req) {
        access.requireUser();
        Vendor v = find(id);
        v.change(req.name().trim(), Text.clean(req.contactPerson()), validPhone(req.phoneNumber()),
                req.location().trim(), Text.clean(req.notes()), findArea(req.areaId()));
        if (req.active() != null) v.setActive(req.active());
        if (req.dailyLimit() != null) v.setDailyLimit(req.dailyLimit());
        if (req.mapUrl() != null) v.setMapUrl(cleanMapUrl(req.mapUrl()));
        return respond(vendors.saveAndFlush(v));
    }

    /** Saves the hotel's logo (PNG, JPEG or WebP, up to 500 KB). The type is read from the file itself, not from its name. */
    @Transactional
    public VendorResponse setLogo(Long id, MultipartFile file) {
        access.requireUser();
        Vendor v = find(id);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("The file could not be read. Please try again.");
        }
        if (bytes.length == 0) throw new BadRequestException("Choose an image file first.");
        if (bytes.length > MAX_LOGO_BYTES) throw new BadRequestException("That file is too big. Use an image smaller than 500 KB.");
        String type = detectImageType(bytes);
        if (type == null) throw new BadRequestException("Use a PNG, JPG or WebP image.");
        logos.save(v.getId(), type, bytes);
        return respond(v);
    }

    @Transactional
    public VendorResponse removeLogo(Long id) {
        access.requireUser();
        Vendor v = find(id);
        logos.delete(v.getId());
        return respond(v);
    }

    /** PNG, JPEG and WebP only. SVG is refused on purpose: it can carry scripts. */
    static String detectImageType(byte[] b) {
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }

    private VendorResponse respond(Vendor v) {
        return toResponse(v, logos.version(v.getId()));
    }

    private Area findArea(Long areaId) {
        if (areaId == null) return null;
        return areas.findById(areaId).orElseThrow(() -> new BadRequestException("Choose an area from the list."));
    }

    private Vendor find(Long id) {
        return vendors.findById(id).orElseThrow(() -> new NotFoundException("Vendor not found."));
    }

    /** A blank link means "none". Otherwise it must be a full https link, so nothing else can end up behind a button. */
    public static String cleanMapUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty()) return null;
        java.net.URI uri;
        try {
            uri = new java.net.URI(url);
        } catch (java.net.URISyntaxException e) {
            throw new BadRequestException("Paste the full Google Maps link (it starts with https://).");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || url.length() > 500) {
            throw new BadRequestException("Paste the full Google Maps link (it starts with https://).");
        }
        return url;
    }

    static String validPhone(String raw) {
        String phone = Phones.normalise(raw);
        if (!Phones.isValid(phone)) {
            throw new BadRequestException("Enter a valid phone number (10 to 15 digits, optional leading +).");
        }
        return phone;
    }

    private static VendorResponse toResponse(Vendor v, Long logoVersion) {
        return new VendorResponse(v.getId(), v.getName(), v.getContactPerson(), v.getPhoneNumber(), v.getLocation(),
                v.getNotes(), v.getArea() == null ? null : v.getArea().getId(),
                v.getArea() == null ? null : v.getArea().getName(), logoVersion, v.getDailyLimit(), v.isActive(), Utc.toInstant(v.getCreatedAt()), v.getMapUrl());
    }
}
