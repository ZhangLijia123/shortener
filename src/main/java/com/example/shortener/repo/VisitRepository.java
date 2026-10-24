package com.example.shortener.repo;

import com.example.shortener.domain.Visit;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    /**
     * Aggregates the 7-day window in one indexed pass. {@code idx_visits_link_day(link_id,
     * visit_day)} turns this into a range scan whose second column is already sorted, so there
     * is no sort and no full table scan.
     *
     * <p>Days with no visits simply do not come back; the caller zero-fills them.</p>
     */
    @Query("""
            select v.visitDay as day, count(v) as cnt
              from Visit v
             where v.link.id = :linkId
               and v.visitDay between :from and :to
             group by v.visitDay
             order by v.visitDay
            """)
    List<DayCount> countByDay(@Param("linkId") Long linkId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    /** Total number of rows for a link; used by tests to cross-check the counter. */
    @Query("select count(v) from Visit v where v.link.id = :linkId")
    long countByLinkId(@Param("linkId") Long linkId);

    /** Projection for the aggregate query above. */
    interface DayCount {
        LocalDate getDay();

        Long getCnt();
    }
}
