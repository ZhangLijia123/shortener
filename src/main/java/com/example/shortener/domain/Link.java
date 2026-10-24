package com.example.shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * A short link.
 *
 * <p>{@code uk_links_code} is not just an index: it is the actual guarantee that a code is
 * unique. Applications that check "does this code exist?" before inserting are racy; letting
 * the database reject the duplicate is not.</p>
 */
@Entity
@Table(name = "links",
        uniqueConstraints = @UniqueConstraint(name = "uk_links_code", columnNames = "code"))
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 8)
    private String code;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "visit_count", nullable = false)
    private long visitCount;

    protected Link() {
        // for JPA
    }

    public Link(String code, String url, Instant createdAt) {
        this.code = code;
        this.url = url;
        this.createdAt = createdAt;
        this.visitCount = 0L;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getUrl() {
        return url;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVisitCount() {
        return visitCount;
    }

    /*
     * Deliberately no setVisitCount(...): a read-modify-write in Java loses updates under
     * concurrency. All increments go through LinkRepository#incrementVisitCount, which is a
     * single atomic UPDATE executed by the database.
     */
}
