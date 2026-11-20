package com.school.enrolment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Metadata about a document an applicant has handed in. The file itself is not stored;
 * "name on document" and "date on document" are what staff read off it, and are what the
 * document reviewer cross-checks against the application.
 */
@Entity
@Table(name = "application_documents")
public class ApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType docType;

    @Column(length = 200)
    private String nameOnDocument;

    private LocalDate dateOnDocument;

    @Column(nullable = false)
    private Instant receivedAt;

    protected ApplicationDocument() {
    }

    public ApplicationDocument(DocumentType docType, String nameOnDocument, LocalDate dateOnDocument) {
        this.docType = docType;
        this.nameOnDocument = nameOnDocument;
        this.dateOnDocument = dateOnDocument;
        this.receivedAt = Instant.now();
    }

    void attachTo(Application application) {
        this.application = application;
    }

    public Long getId() { return id; }
    public DocumentType getDocType() { return docType; }
    public String getNameOnDocument() { return nameOnDocument; }
    public LocalDate getDateOnDocument() { return dateOnDocument; }
    public Instant getReceivedAt() { return receivedAt; }
}
