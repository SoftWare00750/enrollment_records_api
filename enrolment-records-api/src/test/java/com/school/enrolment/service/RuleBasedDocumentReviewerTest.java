package com.school.enrolment.service;

import com.school.enrolment.TestData;
import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationDocument;
import com.school.enrolment.domain.DocumentType;
import com.school.enrolment.web.ApiDtos.DocumentReviewReport;
import com.school.enrolment.web.ApiDtos.ReviewFlag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedDocumentReviewerTest {

    private final RuleBasedDocumentReviewer reviewer = new RuleBasedDocumentReviewer();
    private static final LocalDate DOB = LocalDate.of(2020, 3, 10);

    private Application baseApplication() {
        return TestData.application(TestData.offering(TestData.primary1(), 30), "Ada", "Okafor", DOB, null);
    }

    private static boolean hasFlag(DocumentReviewReport report, String code) {
        return report.flags().stream().map(ReviewFlag::code).anyMatch(code::equals);
    }

    @Test
    void consistentDocuments_produceNoFlags() {
        DocumentReviewReport report = reviewer.review(TestData.withStandardDocuments(baseApplication()));

        assertThat(report.flags()).isEmpty();
        assertThat(report.overall()).isEqualTo("NO_ISSUES_FOUND");
        assertThat(report.notice()).contains("Advisory only");
    }

    @Test
    void nameOrderAndCaseDoNotMatter() {
        Application application = baseApplication();
        application.addDocument(new ApplicationDocument(DocumentType.BIRTH_CERTIFICATE, "OKAFOR, Ada", DOB));
        application.addDocument(new ApplicationDocument(DocumentType.IMMUNISATION_RECORD, null, null));
        application.addDocument(new ApplicationDocument(DocumentType.PASSPORT_PHOTO, null, null));

        assertThat(reviewer.review(application).flags()).isEmpty();
    }

    @Test
    void completelyDifferentName_isFlaggedHigh() {
        Application application = baseApplication();
        application.addDocument(new ApplicationDocument(DocumentType.BIRTH_CERTIFICATE, "Tunde Bakare", DOB));
        application.addDocument(new ApplicationDocument(DocumentType.IMMUNISATION_RECORD, null, null));
        application.addDocument(new ApplicationDocument(DocumentType.PASSPORT_PHOTO, null, null));

        DocumentReviewReport report = reviewer.review(application);

        assertThat(hasFlag(report, "NAME_MISMATCH")).isTrue();
        assertThat(report.overall()).isEqualTo("NEEDS_STAFF_REVIEW");
    }

    @Test
    void partialNameMatch_isFlaggedForReviewNotRejected() {
        Application application = baseApplication();
        application.addDocument(new ApplicationDocument(DocumentType.BIRTH_CERTIFICATE, "Ada Obi", DOB));
        application.addDocument(new ApplicationDocument(DocumentType.IMMUNISATION_RECORD, null, null));
        application.addDocument(new ApplicationDocument(DocumentType.PASSPORT_PHOTO, null, null));

        assertThat(hasFlag(reviewer.review(application), "NAME_PARTIAL_MATCH")).isTrue();
    }

    @Test
    void middleNameOnDocument_isNotAMismatch() {
        Application application = baseApplication();
        application.addDocument(new ApplicationDocument(DocumentType.BIRTH_CERTIFICATE, "Ada Chioma Okafor", DOB));
        application.addDocument(new ApplicationDocument(DocumentType.IMMUNISATION_RECORD, null, null));
        application.addDocument(new ApplicationDocument(DocumentType.PASSPORT_PHOTO, null, null));

        assertThat(reviewer.review(application).flags()).isEmpty();
    }

    @Test
    void dateOfBirthMismatch_isFlagged() {
        Application application = baseApplication();
        application.addDocument(new ApplicationDocument(DocumentType.BIRTH_CERTIFICATE, "Ada Okafor",
                LocalDate.of(2019, 3, 10)));
        application.addDocument(new ApplicationDocument(DocumentType.IMMUNISATION_RECORD, null, null));
        application.addDocument(new ApplicationDocument(DocumentType.PASSPORT_PHOTO, null, null));

        assertThat(hasFlag(reviewer.review(application), "DATE_OF_BIRTH_MISMATCH")).isTrue();
    }

    @Test
    void missingDocuments_areFlagged_andUnverifiableOnesAreNotSilentlyPassed() {
        Application application = baseApplication();
        application.addDocument(new ApplicationDocument(DocumentType.BIRTH_CERTIFICATE, null, null));

        DocumentReviewReport report = reviewer.review(application);

        assertThat(hasFlag(report, "MISSING_DOCUMENT")).isTrue();
        assertThat(hasFlag(report, "UNVERIFIED_NAME")).isTrue();
        assertThat(hasFlag(report, "UNVERIFIED_DATE_OF_BIRTH")).isTrue();
    }
}
