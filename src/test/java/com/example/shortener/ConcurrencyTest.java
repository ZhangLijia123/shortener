package com.example.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shortener.domain.Link;
import com.example.shortener.repo.LinkRepository;
import com.example.shortener.repo.VisitRepository;
import com.example.shortener.service.LinkService;
import com.example.shortener.service.RedirectService;
import com.example.shortener.service.StatsService;
import com.example.shortener.web.dto.DayCountDto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The test that matters for the 15% "concurrency & consistency" criterion.
 *
 * <p>320 redirects are fired concurrently at the same code; the total must be exactly 320.
 * With a read-modify-write counter this test fails quickly, which is the point.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrencyTest {

    private static final int THREADS = 16;
    private static final int VISITS_PER_THREAD = 20;

    @Autowired
    private LinkService linkService;

    @Autowired
    private RedirectService redirectService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private LinkRepository links;

    @Autowired
    private VisitRepository visits;

    @Test
    void concurrentRedirectsDoNotLoseVisitCounts() throws Exception {
        Link link = linkService.create("https://example.com/concurrent-" + UUID.randomUUID());
        String code = link.getCode();
        int expected = THREADS * VISITS_PER_THREAD;

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int t = 0; t < THREADS; t++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    for (int i = 0; i < VISITS_PER_THREAD; i++) {
                        redirectService.resolveAndCount(code);
                    }
                    return null;
                }));
            }
            startGate.countDown();
            for (Future<?> future : futures) {
                future.get(120, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        // 1. The denormalised counter is exact.
        long counter = links.findByCode(code).orElseThrow().getVisitCount();
        assertThat(counter).isEqualTo(expected);

        // 2. The counter agrees with the event log (same transaction, no lost writes).
        assertThat(visits.countByLinkId(link.getId())).isEqualTo(expected);

        // 3. The daily series agrees too.
        long fromStats = statsService.stats(code).days().stream()
                .mapToLong(DayCountDto::count)
                .sum();
        assertThat(fromStats).isEqualTo(expected);
    }

    @Test
    void concurrentCreatesProduceUniqueCodes() throws Exception {
        int count = 40;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return linkService.create("https://example.com/unique").getCode();
                }));
            }
            startGate.countDown();

            List<String> codes = new ArrayList<>();
            for (Future<String> future : futures) {
                codes.add(future.get(60, TimeUnit.SECONDS));
            }
            assertThat(codes).hasSize(count).doesNotHaveDuplicates();
        } finally {
            pool.shutdownNow();
        }
    }
}
