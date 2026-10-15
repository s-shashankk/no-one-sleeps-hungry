package com.mealup.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mealup.common.validation.Phones;

class PhonesTest {

    @Test
    void removesSpacesDashesAndBrackets() {
        assertThat(Phones.normalise("+91 98765-43210")).isEqualTo("+919876543210");
        assertThat(Phones.normalise("(080) 2345 6789")).isEqualTo("08023456789");
    }

    @Test
    void acceptsOnlyPlausibleNumbers() {
        assertThat(Phones.isValid("+919876543210")).isTrue();
        assertThat(Phones.isValid("9876543210")).isTrue();
        assertThat(Phones.isValid("12345")).isFalse();
        assertThat(Phones.isValid("98765abcde")).isFalse();
        assertThat(Phones.isValid(null)).isFalse();
    }
}
