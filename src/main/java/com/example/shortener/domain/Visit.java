package com.example.shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One row per redirect (append-only event log).
 *
 * <p>Storing the calendar day next to the instant is intentional: it is computed once, in the
 * configured statistics zone, so the 7-day query never has to convert time zones per row and
 * can use a plain B-tree range scan.</p>
 */
@Entity
@Table(name = "visits",
        indexes = @Index(name = "idx_visits_link_day", columnList = "link_id, visit_day"))
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_visits_link"))
    private Link link;

    @Column(name = "visit_day", nullable = false)
    private LocalDate visitDay;

    @Column(name = "visited_at", nullable = false)
    private Instant visitedAt;

    protected Visit() {
        // for JPA
    }

    public Visit(Link link, LocalDate visitDay, Instant visitedAt) {
        this.link = link;
        this.visitDay = visitDay;
        this.visitedAt = visitedAt;
    }

    public Long getId() {
        return id;
    }

    public Link getLink() {
        return link;
    }

    public LocalDate getVisitDay() {
        return visitDay;
    }

    public Instant getVisitedAt() {
        return visitedAt;
    }
}
