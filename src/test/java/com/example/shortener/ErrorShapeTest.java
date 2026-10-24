package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shortener.web.dto.ApiError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The spec asks for one consistent error shape across every endpoint; this class is the proof.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorShapeTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void unknownCodeIs404OnAllThreeEndpoints() throws Exception {
        for (String path : new String[]{"/zzzzzz", "/api/links/zzzzzz", "/api/links/zzzzzz/stats"}) {
            MvcResult result = mvc.perform(get(path))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andReturn();
            assertSingleErrorShape(result);
        }
    }

    @Test
    void unmatchedPathsUseTheSameShape() throws Exception {
        for (String path : new String[]{"/nope", "/api/unknown/thing"}) {
            MvcResult result = mvc.perform(get(path))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andReturn();
            assertSingleErrorShape(result);
        }
    }

    @Test
    void wrongMethodIs405WithTheSameShape() throws Exception {
        MvcResult result = mvc.perform(post("/zzzzzz"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"))
                .andReturn();
        assertSingleErrorShape(result);
    }

    @Test
    void wrongContentTypeIs415WithTheSameShape() throws Exception {
        MvcResult result = mvc.perform(post("/api/links")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("https://example.com"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andReturn();
        assertSingleErrorShape(result);
    }

    private void assertSingleErrorShape(MvcResult result) throws Exception {
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.size()).isEqualTo(3);
        assertThat(body.get("status").asInt()).isEqualTo(result.getResponse().getStatus());
        assertThat(body.get("error").asText()).isNotBlank();
        assertThat(body.get("message").asText()).isNotBlank();
        // Same shape means it deserialises into the documented record.
        ApiError parsed = json.readValue(result.getResponse().getContentAsString(), ApiError.class);
        assertThat(parsed.status()).isEqualTo(result.getResponse().getStatus());
    }
}
