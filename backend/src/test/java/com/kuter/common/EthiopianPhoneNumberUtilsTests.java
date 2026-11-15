package com.kuter.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EthiopianPhoneNumberUtilsTests {

    @Test
    @DisplayName("Should normalize 9-digit format without prefix: 982112233 -> +251982112233")
    void shouldNormalizeWithoutPrefix() {
        String result = EthiopianPhoneNumberUtils.normalize("982112233");
        assertThat(result).isEqualTo("+251982112233");
    }

    @Test
    @DisplayName("Should normalize leading zero format: 0982112233 -> +251982112233")
    void shouldNormalizeWithLeadingZero() {
        String result = EthiopianPhoneNumberUtils.normalize("0982112233");
        assertThat(result).isEqualTo("+251982112233");
    }

    @Test
    @DisplayName("Should preserve and normalize canonical format: +251982112233 -> +251982112233")
    void shouldNormalizeWithPlus251() {
        String result = EthiopianPhoneNumberUtils.normalize("+251982112233");
        assertThat(result).isEqualTo("+251982112233");
    }

    @Test
    @DisplayName("Should normalize without plus: 251982112233 -> +251982112233")
    void shouldNormalizeWith251() {
        String result = EthiopianPhoneNumberUtils.normalize("251982112233");
        assertThat(result).isEqualTo("+251982112233");
    }

    @Test
    @DisplayName("Should strip whitespace and hyphens: +251 982-112-233 -> +251982112233")
    void shouldNormalizeWithFormatting() {
        String result = EthiopianPhoneNumberUtils.normalize("+251 982-112-233");
        assertThat(result).isEqualTo("+251982112233");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678",      // too short
            "0882112233",    // starts with 8
            "98211223344",   // too long
            "abc982112233",  // letters
            "+12025550199",  // US number
            "0000000000"     // all zeros
    })
    @DisplayName("Should reject invalid phone numbers with BadRequestException")
    void shouldRejectInvalidNumbers(String invalid) {
        assertThatThrownBy(() -> EthiopianPhoneNumberUtils.normalize(invalid))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should return null for null or blank input")
    void shouldReturnNullForBlank() {
        assertThat(EthiopianPhoneNumberUtils.normalize(null)).isNull();
        assertThat(EthiopianPhoneNumberUtils.normalize("  ")).isNull();
    }
}
