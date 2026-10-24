package com.example.shortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.shortener.error.InvalidUrlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UrlValidatorTest {

    private final UrlValidator validator = new UrlValidator();

    @Test
    void acceptsHttpAndHttps() {
        assertThat(validator.validateAndNormalize("https://example.com/a?b=1"))
                .isEqualTo("https://example.com/a?b=1");
        assertThat(validator.validateAndNormalize("http://example.com:8081/a"))
                .isEqualTo("http://example.com:8081/a");
    }

    /**
     * {@code new URI("not-a-url")} does not throw, which is exactly why the scheme and host
     * checks exist. Each of these must be a 400.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "not-a-url",
            "//example.com/path",
            "ftp://example.com/file.txt",
            "javascript:alert(1)",
            "http://",
            "https:///nohost"
    })
    void rejectsAnythingThatIsNotHttpOrHttpsWithAHost(String input) {
        assertThatThrownBy(() -> validator.validateAndNormalize(input))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void rejectsUrlsLongerThan2048Characters() {
        String tooLong = "https://example.com/" + "a".repeat(2048);
        assertThatThrownBy(() -> validator.validateAndNormalize(tooLong))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("2048");
    }

    @Test
    void acceptsAUrlOfExactly2048Characters() {
        String prefix = "https://example.com/";
        String exact = prefix + "a".repeat(2048 - prefix.length());
        assertThat(validator.validateAndNormalize(exact)).hasSize(2048);
    }

    @Test
    void rejectsSurroundingSpacesInsideTheUrl() {
        assertThatThrownBy(() -> validator.validateAndNormalize("https://example.com/a b"))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    void percentEncodesNonAsciiCharactersInsteadOfRejectingThem() {
        assertThat(validator.validateAndNormalize("https://example.com/测试?q=中文"))
                .isEqualTo("https://example.com/%E6%B5%8B%E8%AF%95?q=%E4%B8%AD%E6%96%87");
    }

    @Test
    void leavesExistingPercentEscapesAlone() {
        assertThat(validator.validateAndNormalize("https://example.com/a%20b"))
                .isEqualTo("https://example.com/a%20b");
    }
}
