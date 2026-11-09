package com.mealup.vendors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.mealup.common.exception.BadRequestException;

class MapUrlCleaningTest {

    @Test
    void blankMeansNoLink() {
        assertThat(VendorService.cleanMapUrl("   ")).isNull();
        assertThat(VendorService.cleanMapUrl("")).isNull();
    }

    @Test
    void aFullHttpsLinkIsKeptAndTrimmed() {
        assertThat(VendorService.cleanMapUrl("  https://maps.app.goo.gl/abc123 ")).isEqualTo("https://maps.app.goo.gl/abc123");
    }

    @Test
    void anythingElseIsRefused() {
        assertThatThrownBy(() -> VendorService.cleanMapUrl("javascript:alert(1)")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> VendorService.cleanMapUrl("http://maps.example.com/x")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> VendorService.cleanMapUrl("not a link")).isInstanceOf(BadRequestException.class);
    }
}
