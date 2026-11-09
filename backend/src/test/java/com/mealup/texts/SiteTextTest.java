package com.mealup.texts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.mealup.support.ApiTestSupport;

/** The admin can change website wording; everyone can read it; a blank text brings the original back. */
class SiteTextTest extends ApiTestSupport {

    @Test
    void adminEditsTextAndBlankRemovesIt() throws Exception {
        MockHttpSession admin = loginAsFounder();

        putJson("/api/v1/texts", Map.of("values", Map.of("home.hero.title", "Hello, kind heart.")), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['home.hero.title']").value("Hello, kind heart."));

        // visitors need no sign-in to read it
        getAs(null, "/api/v1/public/texts").andExpect(status().isOk())
                .andExpect(jsonPath("$['home.hero.title']").value("Hello, kind heart."));

        // an empty text removes the change
        putJson("/api/v1/texts", Map.of("values", Map.of("home.hero.title", "")), admin).andExpect(status().isOk());
        String all = getAs(null, "/api/v1/public/texts").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(all).doesNotContain("home.hero.title");
    }

    @Test
    void onlyAdminsCanChangeTextAndBadNamesAreRefused() throws Exception {
        MockHttpSession volunteer = registerVolunteer(uniqueEmail());
        putJson("/api/v1/texts", Map.of("values", Map.of("home.hero.title", "Hacked")), volunteer)
                .andExpect(status().isForbidden());

        MockHttpSession admin = loginAsFounder();
        putJson("/api/v1/texts", Map.of("values", Map.of("Bad Name!", "x")), admin).andExpect(status().isBadRequest());
    }
}
