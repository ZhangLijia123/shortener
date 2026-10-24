package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LinkApiIntegrationTest {

    private static final String LONG_URL = "https://example.com/some/very/long/path?query=1";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void createThenRedirectThenInspectThenStats() throws Exception {
        String created = mvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + LONG_URL + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/[A-Za-z0-9]{6,8}")))
                .andExpect(jsonPath("$.code", matchesPattern("[A-Za-z0-9]{6,8}")))
                .andExpect(jsonPath("$.url").value(LONG_URL))
                .andExpect(jsonPath("$.short_url",
                        matchesPattern("http://localhost:8080/[A-Za-z0-9]{6,8}")))
                .andExpect(jsonPath("$.created_at").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String code = json.readTree(created).get("code").asText();

        // First visit.
        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", LONG_URL))
                .andExpect(header().string("Cache-Control", "no-store"));

        // Second visit: 302 is not cached, so the server sees it.
        mvc.perform(get("/" + code)).andExpect(status().isFound());

        mvc.perform(get("/api/links/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.url").value(LONG_URL))
                .andExpect(jsonPath("$.visit_count").value(2));

        mvc.perform(get("/api/links/" + code + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[6].count").value(2))
                .andExpect(jsonPath("$.days[6].date").isNotEmpty());
    }

    @Test
    void submittingTheSameUrlTwiceYieldsTwoDistinctCodes() throws Exception {
        String first = createLink(LONG_URL);
        String second = createLink(LONG_URL);

        assertThat(first).isNotEqualTo(second);
        mvc.perform(get("/api/links/" + first)).andExpect(status().isOk());
        mvc.perform(get("/api/links/" + second)).andExpect(status().isOk());
    }

    @Test
    void redirectsAreCountedIndependentlyPerLink() throws Exception {
        String first = createLink("https://example.com/a");
        String second = createLink("https://example.com/b");

        mvc.perform(get("/" + first)).andExpect(status().isFound());
        mvc.perform(get("/" + first)).andExpect(status().isFound());
        mvc.perform(get("/" + second)).andExpect(status().isFound());

        mvc.perform(get("/api/links/" + first))
                .andExpect(jsonPath("$.visit_count").value(2));
        mvc.perform(get("/api/links/" + second))
                .andExpect(jsonPath("$.visit_count").value(1));
    }

    @Test
    void statsAreZeroFilledAndOrderedOldestFirst() throws Exception {
        String code = createLink("https://example.com/zerofill");

        String body = mvc.perform(get("/api/links/" + code + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(7))
                .andReturn().getResponse().getContentAsString();

        var days = json.readTree(body).get("days");
        assertThat(days).hasSize(7);
        for (int i = 0; i < 7; i++) {
            assertThat(days.get(i).get("count").asLong()).isZero();
            assertThat(days.get(i).get("date").asText()).matches("\\d{4}-\\d{2}-\\d{2}");
            if (i > 0) {
                assertThat(days.get(i).get("date").asText())
                        .isGreaterThan(days.get(i - 1).get("date").asText());
            }
        }
    }

    @Test
    void latestLinksAreListedForTheDemoPage() throws Exception {
        String code = createLink("https://example.com/listed");
        mvc.perform(get("/api/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").isNotEmpty())
                .andExpect(jsonPath("$[?(@.code == '" + code + "')]").exists());
    }

    /**
     * Regression guard: a bare {@code @GetMapping("/{code}")} matches "/" with an empty code
     * and shadows the welcome page, so the redirect route must exclude the root path.
     *
     * <p>The welcome page is a forward to {@code index.html}; MockMvc does not dispatch the
     * forward, so the assertion is on the forwarded URL rather than on the HTML body.</p>
     */
    @Test
    void rootServesTheDemoPage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    private String createLink(String url) throws Exception {
        String body = mvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("code").asText();
    }
}
