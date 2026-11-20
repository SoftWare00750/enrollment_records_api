package com.school.enrolment.service;

import com.school.enrolment.TestData;
import com.school.enrolment.domain.AdmissionDecision;
import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationStatus;
import com.school.enrolment.domain.ClassOffering;
import com.school.enrolment.domain.DecisionType;
import com.school.enrolment.domain.EligibilityStatus;
import com.school.enrolment.exception.ApiException;
import com.school.enrolment.repository.AcademicYearRepository;
import com.school.enrolment.repository.AdmissionDecisionRepository;
import com.school.enrolment.repository.ApplicantRepository;
import com.school.enrolment.repository.ApplicationRepository;
import com.school.enrolment.repository.AuditEntryRepository;
import com.school.enrolment.repository.ClassOfferingRepository;
import com.school.enrolment.repository.GradeLevelRepository;
import com.school.enrolment.repository.GuardianRepository;
import com.school.enrolment.web.ApiDtos.ApplicationResponse;
import com.school.enrolment.web.ApiDtos.DecisionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Decision rules: status transitions, eligibility gate, and the seat-capacity rule. */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceDecisionTest {

    @Mock ApplicationRepository applicationRepo;
    @Mock ClassOfferingRepository offeringRepo;
    @Mock AcademicYearRepository yearRepo;
    @Mock GradeLevelRepository gradeRepo;
    @Mock GuardianRepository guardianRepo;
    @Mock ApplicantRepository applicantRepo;
    @Mock AdmissionDecisionRepository decisionRepo;
    @Mock AuditEntryRepository auditRepo;
    @Mock EligibilityService eligibilityService;
    @Mock DocumentReviewer documentReviewer;
    @Mock AuditService auditService;

    @InjectMocks ApplicationService service;

    private final ClassOffering offering = TestData.offering(TestData.primary1(), 2);

    private Application application(ApplicationStatus status, EligibilityStatus eligibility) {
        Application application = TestData.application(offering, "Ada", "Okafor", LocalDate.of(2020, 3, 10), null);
        TestData.inState(application, status, eligibility);
        when(applicationRepo.findWithDetailsById(1L)).thenReturn(Optional.of(application));
        return application;
    }

    private static DecisionRequest request(DecisionType type) {
        return new DecisionRequest(type, "Reviewed by registrar");
    }

    private static void assertStatus(Throwable thrown, HttpStatus expected) {
        assertThat(thrown).isInstanceOf(ApiException.class);
        assertThat(((ApiException) thrown).getStatus()).isEqualTo(expected);
    }

    @Test
    void admit_whenSeatAvailable_setsAdmitted_recordsDecision_andAudits() {
        Application application = application(ApplicationStatus.UNDER_REVIEW, EligibilityStatus.ELIGIBLE);
        when(offeringRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(offering));
        when(applicationRepo.countByOfferingIdAndStatus(10L, ApplicationStatus.ADMITTED)).thenReturn(1L);

        ApplicationResponse response = service.decide(1L, request(DecisionType.ADMITTED), "registrar.jane");

        assertThat(response.status()).isEqualTo(ApplicationStatus.ADMITTED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ADMITTED);
        verify(decisionRepo).save(any(AdmissionDecision.class));
        verify(auditService).record(eq("APPLICATION"), eq(1L), eq("DECISION_ADMITTED"), eq("UNDER_REVIEW"),
                eq("ADMITTED"), eq("registrar.jane"), eq("Reviewed by registrar"));
    }

    @Test
    void admit_whenClassIsFull_isRejectedWithConflict_andNothingIsRecorded() {
        Application application = application(ApplicationStatus.UNDER_REVIEW, EligibilityStatus.ELIGIBLE);
        when(offeringRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(offering));
        when(applicationRepo.countByOfferingIdAndStatus(10L, ApplicationStatus.ADMITTED)).thenReturn(2L);

        assertThatThrownBy(() -> service.decide(1L, request(DecisionType.ADMITTED), "registrar.jane"))
                .satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        verify(decisionRepo, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void admit_whenNotEligible_isUnprocessable_andDoesNotEvenLockTheClass() {
        application(ApplicationStatus.UNDER_REVIEW, EligibilityStatus.INELIGIBLE);

        assertThatThrownBy(() -> service.decide(1L, request(DecisionType.ADMITTED), "registrar.jane"))
                .satisfies(e -> assertStatus(e, HttpStatus.UNPROCESSABLE_ENTITY));

        verifyNoInteractions(offeringRepo);
    }

    @Test
    void admit_whenEligibilityNeverChecked_isUnprocessable() {
        application(ApplicationStatus.SUBMITTED, EligibilityStatus.PENDING);

        assertThatThrownBy(() -> service.decide(1L, request(DecisionType.ADMITTED), "registrar.jane"))
                .satisfies(e -> assertStatus(e, HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void waitlist_whenEligible_works_withoutTouchingCapacity() {
        Application application = application(ApplicationStatus.UNDER_REVIEW, EligibilityStatus.ELIGIBLE);

        service.decide(1L, request(DecisionType.WAITLISTED), "registrar.jane");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WAITLISTED);
        verifyNoInteractions(offeringRepo);
    }

    @Test
    void waitlist_whenAlreadyWaitlisted_isUnprocessable() {
        application(ApplicationStatus.WAITLISTED, EligibilityStatus.ELIGIBLE);

        assertThatThrownBy(() -> service.decide(1L, request(DecisionType.WAITLISTED), "registrar.jane"))
                .satisfies(e -> assertStatus(e, HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void waitlistedApplicant_canLaterBeAdmitted_whenASeatFreesUp() {
        Application application = application(ApplicationStatus.WAITLISTED, EligibilityStatus.ELIGIBLE);
        when(offeringRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(offering));
        when(applicationRepo.countByOfferingIdAndStatus(10L, ApplicationStatus.ADMITTED)).thenReturn(1L);

        service.decide(1L, request(DecisionType.ADMITTED), "registrar.jane");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ADMITTED);
    }

    @Test
    void reject_isAllowed_evenWithoutAnEligibilityCheck() {
        Application application = application(ApplicationStatus.SUBMITTED, EligibilityStatus.PENDING);

        service.decide(1L, request(DecisionType.REJECTED), "registrar.jane");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void terminalApplications_cannotReceiveAnotherDecision() {
        application(ApplicationStatus.REJECTED, EligibilityStatus.ELIGIBLE);

        assertThatThrownBy(() -> service.decide(1L, request(DecisionType.ADMITTED), "registrar.jane"))
                .satisfies(e -> assertStatus(e, HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void admittedApplications_cannotBeDecidedAgain() {
        application(ApplicationStatus.ADMITTED, EligibilityStatus.ELIGIBLE);

        assertThatThrownBy(() -> service.decide(1L, request(DecisionType.REJECTED), "registrar.jane"))
                .satisfies(e -> assertStatus(e, HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void unknownApplication_isNotFound() {
        when(applicationRepo.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decide(99L, request(DecisionType.REJECTED), "registrar.jane"))
                .satisfies(e -> assertStatus(e, HttpStatus.NOT_FOUND));
    }

    @Test
    void withdraw_fromAdmitted_isAllowed_andReleasesTheSeat() {
        Application application = application(ApplicationStatus.ADMITTED, EligibilityStatus.ELIGIBLE);

        service.withdraw(1L, "registrar.jane");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void withdraw_whenAlreadyRejected_isUnprocessable() {
        application(ApplicationStatus.REJECTED, EligibilityStatus.INELIGIBLE);

        assertThatThrownBy(() -> service.withdraw(1L, "registrar.jane"))
                .satisfies(e -> assertStatus(e, HttpStatus.UNPROCESSABLE_ENTITY));
    }
}
