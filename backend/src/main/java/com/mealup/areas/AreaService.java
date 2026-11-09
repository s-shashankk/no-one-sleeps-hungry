package com.mealup.areas;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.areas.AreaDtos.AreaRequest;
import com.mealup.areas.AreaDtos.AreaResponse;
import com.mealup.common.exception.ConflictException;
import com.mealup.common.exception.NotFoundException;

/** The list of areas. Areas are marked "open" or "coming soon" by the admin and never deleted. */
@Service
public class AreaService {

    private final AreaRepository areas;
    private final AccessPolicy access;

    public AreaService(AreaRepository areas, AccessPolicy access) {
        this.areas = areas;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> list() {
        access.requireUser();
        return areas.findAllByOrderBySortOrderAscNameAsc().stream().map(AreaService::toResponse).toList();
    }

    @Transactional
    public AreaResponse create(AreaRequest req) {
        access.requireUser();
        String city = req.city().trim();
        String name = req.name().trim();
        if (areas.existsByCityIgnoreCaseAndNameIgnoreCase(city, name)) {
            throw new ConflictException(name + ", " + city + " is already in the list.");
        }
        int next = (int) areas.count() + 1;
        return toResponse(areas.saveAndFlush(new Area(city, name, req.open(), next)));
    }

    @Transactional
    public AreaResponse update(Long id, AreaRequest req) {
        access.requireUser();
        Area a = areas.findById(id).orElseThrow(() -> new NotFoundException("Area not found."));
        String city = req.city().trim();
        String name = req.name().trim();
        boolean renamed = !a.getCity().equalsIgnoreCase(city) || !a.getName().equalsIgnoreCase(name);
        if (renamed && areas.existsByCityIgnoreCaseAndNameIgnoreCase(city, name)) {
            throw new ConflictException(name + ", " + city + " is already in the list.");
        }
        a.change(city, name, req.open());
        return toResponse(areas.saveAndFlush(a));
    }

    static AreaResponse toResponse(Area a) {
        return new AreaResponse(a.getId(), a.getCity(), a.getName(), a.isOpen());
    }
}
