package com.example.shortener;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationTest {

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("invalidBodies")
    void rejectsInvalidInputWithA400AndAStableErrorCode(String body, String expectedError) throws Exception {
        mvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value(expectedError))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    static Stream<Arguments> invalidBodies() {
        // "https://example.com/" is 20 characters, so 2030 more puts us at 2050 > 2048.
        String tooLong = "{\"url\":\"https://example.com/" + "a".repeat(2030) + "\"}";
        return Stream.of(
                Arguments.of("{}", "VALIDATION_FAILED"),
                Arguments.of("{\"url\":\"\"}", "VALIDATION_FAILED"),
                Arguments.of("{\"url\":\"   \"}", "VALIDATION_FAILED"),
                Arguments.of("{\"url\":\"not-a-url\"}", "INVALID_URL"),
                Arguments.of("{\"url\":\"ftp://example.com/file.txt\"}", "INVALID_URL"),
                Arguments.of("{\"url\":\"http://\"}", "INVALID_URL"),
                Arguments.of(tooLong, "VALIDATION_FAILED"),
                Arguments.of("{ this is not json", "MALFORMED_JSON"));
    }
}
