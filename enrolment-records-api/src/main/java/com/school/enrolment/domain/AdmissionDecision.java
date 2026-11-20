package com.school.enrolment.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "admission_decisions")
public class AdmissionDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisionType decision;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 100)
    private String decidedBy;

    @Column(nullable = false)
    private Instant decidedAt;

    protected AdmissionDecision() {
    }

    public AdmissionDecision(Long applicationId, DecisionType decision, String reason, String decidedBy) {
        this.applicationId = applicationId;
        this.decision = decision;
        this.reason = reason;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public DecisionType getDecision() { return decision; }
    public String getReason() { return reason; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
}
