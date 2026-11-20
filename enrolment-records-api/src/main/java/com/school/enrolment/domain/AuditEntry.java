package com.school.enrolment.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** One immutable row of audit history. The database trigger forbids UPDATE and DELETE. */
@Entity
@Table(name = "audit_log")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 50)
    private String fromValue;

    @Column(length = 50)
    private String toValue;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuditEntry() {
    }

    public AuditEntry(String entityType, Long entityId, String action, String fromValue, String toValue,
                      String actor, String details) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.actor = actor;
        this.details = details == null || details.length() <= 1000 ? details : details.substring(0, 997) + "...";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getFromValue() { return fromValue; }
    public String getToValue() { return toValue; }
    public String getActor() { return actor; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
