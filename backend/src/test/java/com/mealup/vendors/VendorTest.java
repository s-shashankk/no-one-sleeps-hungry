package com.mealup.vendors;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.mealup.support.ApiTestSupport;

/** Add, view, edit and switch vendors on and off. */
class VendorTest extends ApiTestSupport {

    @Test
    void addAVendorAndTheNumberIsStoredWithoutSpaces() throws Exception {
        MockHttpSession admin = loginAsFounder();

        postJson("/api/v1/vendors", vendorBody(), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneNumber").value("+919876543210"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void contactPersonAndNotesAreOptional() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> body = vendorBody();
        body.remove("contactPerson");

        postJson("/api/v1/vendors", body, admin).andExpect(status().isCreated());
    }

    @Test
    void nameNumberAndLocationAreRequiredAndTheNumberMustLookRight() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> noName = vendorBody();
        noName.put("name", "");
        Map<String, Object> badPhone = vendorBody();
        badPhone.put("phoneNumber", "12345");
        Map<String, Object> noLocation = vendorBody();
        noLocation.remove("location");

        postJson("/api/v1/vendors", noName, admin).andExpect(status().isBadRequest());
        postJson("/api/v1/vendors", badPhone, admin).andExpect(status().isBadRequest());
        postJson("/api/v1/vendors", noLocation, admin).andExpect(status().isBadRequest());
    }

    @Test
    void editChangesTheDetails() throws Exception {
        MockHttpSession admin = loginAsFounder();
        long id = createdId("/api/v1/vendors", vendorBody(), admin);
        Map<String, Object> changed = vendorBody();
        changed.put("name", "Renamed Hotel");
        changed.put("location", "New address");

        putJson("/api/v1/vendors/" + id, changed, admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Hotel"))
                .andExpect(jsonPath("$.location").value("New address"));
        getAs(admin, "/api/v1/vendors/" + id).andExpect(jsonPath("$.name").value("Renamed Hotel"));
    }

    @Test
    void switchedOffVendorsLeaveTheActiveListButStayInTheRecords() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> body = vendorBody();
        long id = createdId("/api/v1/vendors", body, admin);
        String name = (String) body.get("name");

        body.put("active", false);
        putJson("/api/v1/vendors/" + id, body, admin).andExpect(jsonPath("$.active").value(false));

        getAs(admin, "/api/v1/vendors?active=true&q=" + name).andExpect(jsonPath("$.content[*].id", not(hasItem((int) id))));
        getAs(admin, "/api/v1/vendors?active=false&q=" + name).andExpect(jsonPath("$.content[*].id", hasItem((int) id)));
    }

    @Test
    void searchFindsByNameLocationOrPhone() throws Exception {
        MockHttpSession admin = loginAsFounder();
        Map<String, Object> body = vendorBody();
        body.put("name", "Zephyr Banquets");
        body.put("location", "Quarry Lane");
        body.put("phoneNumber", "+91 90000 12345");
        long id = createdId("/api/v1/vendors", body, admin);

        for (String q : new String[] {"zephyr", "quarry", "90000", "9000012345"}) {
            getAs(admin, "/api/v1/vendors?q=" + q).andExpect(jsonPath("$.content[*].id", hasItem((int) id)));
        }
    }

    @Test
    void vendorsAreNotVisibleWithoutSigningIn() throws Exception {
        getAs(null, "/api/v1/vendors").andExpect(status().isUnauthorized());
        postJson("/api/v1/vendors", vendorBody(), null).andExpect(status().isUnauthorized());
    }

    @Test
    void anUnknownVendorIsNotFound() throws Exception {
        getAs(loginAsFounder(), "/api/v1/vendors/99999999").andExpect(status().isNotFound());
    }
}
