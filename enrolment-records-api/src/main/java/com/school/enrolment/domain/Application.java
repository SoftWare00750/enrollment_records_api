package com.school.enrolment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id")
    private Applicant applicant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id")
    private ClassOffering offering;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EligibilityStatus eligibilityStatus;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Optimistic lock: two staff members cannot silently overwrite each other's decision. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationDocument> documents = new ArrayList<>();

    protected Application() {
    }

    public Application(String referenceNo, Applicant applicant, ClassOffering offering) {
        this.referenceNo = referenceNo;
        this.applicant = applicant;
        this.offering = offering;
        this.status = ApplicationStatus.SUBMITTED;
        this.eligibilityStatus = EligibilityStatus.PENDING;
        this.submittedAt = Instant.now();
        this.updatedAt = this.submittedAt;
    }

    public void addDocument(ApplicationDocument document) {
        document.attachTo(this);
        documents.add(document);
        this.updatedAt = Instant.now();
    }

    public void changeStatus(ApplicationStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void changeEligibility(EligibilityStatus newEligibility) {
        this.eligibilityStatus = newEligibility;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReferenceNo() { return referenceNo; }
    public Applicant getApplicant() { return applicant; }
    public ClassOffering getOffering() { return offering; }
    public ApplicationStatus getStatus() { return status; }
    public EligibilityStatus getEligibilityStatus() { return eligibilityStatus; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public List<ApplicationDocument> getDocuments() { return documents; }
}
