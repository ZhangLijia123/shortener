package com.example.shortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.shortener.config.AppProperties;
import com.example.shortener.domain.Link;
import com.example.shortener.error.InvalidUrlException;
import com.example.shortener.error.ShortCodeExhaustedException;
import com.example.shortener.repo.LinkRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unit-level proof that a colliding code is retried instead of failing the request.
 * No Spring context, so it stays fast and independent of the database.
 */
class LinkServiceTest {

    private final ShortCodeGenerator generator = mock(ShortCodeGenerator.class);
    private final LinkInserter inserter = mock(LinkInserter.class);
    private final LinkRepository links = mock(LinkRepository.class);
    private final AppProperties properties = new AppProperties("http://localhost:8080", "UTC", 7, 5);

    private final LinkService service =
            new LinkService(generator, inserter, new UrlValidator(), links, properties, Clock.systemUTC());

    @Test
    void retriesWhenTheDatabaseRejectsADuplicateCode() {
        when(generator.next()).thenReturn("TAKEN01", "FRESH01");
        when(inserter.insertOnce(eq("TAKEN01"), anyString(), any()))
                .thenThrow(new DataIntegrityViolationException("uk_links_code"));
        when(inserter.insertOnce(eq("FRESH01"), anyString(), any()))
                .thenAnswer(invocation -> new Link(
                        invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        Link link = service.create("https://example.com/x");

        assertThat(link.getCode()).isEqualTo("FRESH01");
        assertThat(link.getUrl()).isEqualTo("https://example.com/x");
        verify(inserter, times(2)).insertOnce(anyString(), anyString(), any());
    }

    @Test
    void givesUpAfterTheConfiguredNumberOfAttempts() {
        when(generator.next()).thenReturn("TAKEN01");
        when(inserter.insertOnce(anyString(), anyString(), any()))
                .thenThrow(new DataIntegrityViolationException("uk_links_code"));

        assertThatThrownBy(() -> service.create("https://example.com/x"))
                .isInstanceOf(ShortCodeExhaustedException.class)
                .hasMessageContaining("5 attempts");

        verify(inserter, times(5)).insertOnce(anyString(), anyString(), any());
    }

    @Test
    void rejectsAnInvalidUrlBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> service.create("ftp://example.com/file"))
                .isInstanceOf(InvalidUrlException.class);

        verifyNoInteractions(inserter);
    }

    @Test
    void trimsSurroundingWhitespaceBeforeStoring() {
        when(generator.next()).thenReturn("FRESH02");
        when(inserter.insertOnce(anyString(), anyString(), any()))
                .thenAnswer(invocation -> new Link(
                        invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));

        Link link = service.create("  https://example.com/x  ");

        assertThat(link.getUrl()).isEqualTo("https://example.com/x");
    }
}
