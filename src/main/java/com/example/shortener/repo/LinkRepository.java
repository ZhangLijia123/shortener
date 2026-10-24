package com.example.shortener.repo;

import com.example.shortener.domain.Link;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByCode(String code);

    /**
     * Atomic increment. This is the whole answer to "no lost updates": the database reads and
     * writes the column inside a single row-locked statement, so two concurrent redirects can
     * never both observe 128 and both write 129.
     *
     * @return number of rows updated (0 means the link disappeared concurrently)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("update Link l set l.visitCount = l.visitCount + 1 where l.id = :id")
    int incrementVisitCount(@Param("id") Long id);

    /**
     * Sorted by id as a tie-breaker: created_at is stored with second precision, so several
     * links created in the same second would otherwise come back in an arbitrary order.
     */
    List<Link> findTop20ByOrderByCreatedAtDescIdDesc();
}
