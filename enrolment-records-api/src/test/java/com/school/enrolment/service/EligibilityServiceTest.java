package com.school.enrolment.service;

import com.school.enrolment.TestData;
import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationDocument;
import com.school.enrolment.domain.DocumentType;
import com.school.enrolment.web.ApiDtos.EligibilityReport;
import com.school.enrolment.web.ApiDtos.RuleResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityServiceTest {

    private final EligibilityService service = new EligibilityService();

    private Application primary1Applicant(LocalDate dob) {
        return TestData.withStandardDocuments(
                TestData.application(TestData.offering(TestData.primary1(), 30), "Ada", "Okafor", dob, null));
    }

    private RuleResult rule(EligibilityReport report, String name) {
        return report.rules().stream().filter(r -> r.rule().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void eligible_whenAgeInRange_andAllDocumentsPresent() {
        EligibilityReport report = service.evaluate(primary1Applicant(LocalDate.of(2020, 3, 10)));

        assertThat(report.eligible()).isTrue();
        assertThat(report.rules()).allMatch(RuleResult::passed);
    }

    @Test
    void age_isMeasuredOnFirstDayOfAcademicYear_exactlyMinimumAgeIsAccepted() {
        // Turns 5 exactly on 2026-09-14
        EligibilityReport report = service.evaluate(primary1Applicant(LocalDate.of(2021, 9, 14)));

        assertThat(rule(report, "AGE_RANGE").passed()).isTrue();
    }

    @Test
    void age_oneDayTooYoung_isRejected() {
        // Turns 5 on 2026-09-15, one day after the cut-off
        EligibilityReport report = service.evaluate(primary1Applicant(LocalDate.of(2021, 9, 15)));

        assertThat(report.eligible()).isFalse();
        assertThat(rule(report, "AGE_RANGE").passed()).isFalse();
    }

    @Test
    void age_tooOld_isRejected() {
        EligibilityReport report = service.evaluate(primary1Applicant(LocalDate.of(2012, 1, 1)));

        assertThat(rule(report, "AGE_RANGE").passed()).isFalse();
        assertThat(rule(report, "AGE_RANGE").message()).contains("14");
    }

    @Test
    void age_bornAfterCutoff_isRejected() {
        EligibilityReport report = service.evaluate(primary1Applicant(LocalDate.of(2026, 12, 1)));

        assertThat(rule(report, "AGE_RANGE").passed()).isFalse();
    }

    @Test
    void missingDocument_isNamedInTheFailure() {
        Application application = TestData.application(TestData.offering(TestData.primary1(), 30),
                "Bola", "Adeyemi", LocalDate.of(2020, 5, 2), null);
        application.addDocument(new ApplicationDocument(DocumentType.BIRTH_CERTIFICATE, "Bola Adeyemi",
                LocalDate.of(2020, 5, 2)));
        application.addDocument(new ApplicationDocument(DocumentType.PASSPORT_PHOTO, null, null));

        EligibilityReport report = service.evaluate(application);

        assertThat(report.eligible()).isFalse();
        assertThat(rule(report, "REQUIRED_DOCUMENTS").message()).contains("IMMUNISATION_RECORD");
    }

    @Test
    void gradeThatRequiresPreviousReport_needsThatDocumentAndPreviousSchool() {
        Application application = TestData.withStandardDocuments(
                TestData.application(TestData.offering(TestData.primary4(), 30), "Chidi", "Eze",
                        LocalDate.of(2017, 5, 5), null));

        EligibilityReport report = service.evaluate(application);

        assertThat(report.eligible()).isFalse();
        assertThat(rule(report, "REQUIRED_DOCUMENTS").message()).contains("PREVIOUS_REPORT");
        assertThat(rule(report, "PREVIOUS_SCHOOL").passed()).isFalse();
    }

    @Test
    void gradeThatRequiresPreviousReport_passesWhenBothArePresent() {
        Application application = TestData.withStandardDocuments(
                TestData.application(TestData.offering(TestData.primary4(), 30), "Chidi", "Eze",
                        LocalDate.of(2017, 5, 5), "Sunrise Primary"));
        application.addDocument(new ApplicationDocument(DocumentType.PREVIOUS_REPORT, null, null));

        assertThat(service.evaluate(application).eligible()).isTrue();
    }
}
