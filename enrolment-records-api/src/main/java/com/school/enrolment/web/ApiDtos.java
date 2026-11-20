package com.school.enrolment.web;

import com.school.enrolment.domain.ApplicationStatus;
import com.school.enrolment.domain.DecisionType;
import com.school.enrolment.domain.DocumentType;
import com.school.enrolment.domain.EligibilityStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request and response shapes. Entities never leave the service layer. */
public final class ApiDtos {

    private ApiDtos() {
    }

    // ---------------------------------------------------------------- requests

    public record GuardianDto(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^[+0-9 ()\\-]{7,20}$",
                    message = "must be 7-20 characters: digits, spaces, +, - or parentheses") String phone) {
    }

    public record ApplicantDto(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            @Size(max = 200) String previousSchool) {
    }

    public record DocumentDto(
            @NotNull DocumentType type,
            @Size(max = 200) String nameOnDocument,
            @PastOrPresent LocalDate dateOnDocument) {
    }

    public record CreateApplicationRequest(
            @NotNull @Valid ApplicantDto applicant,
            @NotNull @Valid GuardianDto guardian,
            @NotNull Long academicYearId,
            @NotNull Long gradeLevelId,
            @Valid List<@Valid DocumentDto> documents) {
    }

    public record DecisionRequest(
            @NotNull DecisionType decision,
            @NotBlank @Size(max = 500) String reason) {
    }

    // --------------------------------------------------------------- responses

    public record ApplicantView(String firstName, String lastName, LocalDate dateOfBirth, String previousSchool) {
    }

    public record GuardianView(String fullName, String email, String phone) {
    }

    public record DocumentView(DocumentType type, String nameOnDocument, LocalDate dateOnDocument, Instant receivedAt) {
    }

    public record ApplicationResponse(
            Long id,
            String referenceNo,
            ApplicationStatus status,
            EligibilityStatus eligibilityStatus,
            String academicYear,
            String gradeLevel,
            ApplicantView applicant,
            GuardianView guardian,
            List<DocumentView> documents,
            Instant submittedAt,
            Instant updatedAt) {
    }

    public record ApplicationSummary(
            Long id,
            String referenceNo,
            String applicantName,
            String academicYear,
            String gradeLevel,
            ApplicationStatus status,
            EligibilityStatus eligibilityStatus,
            Instant submittedAt) {
    }

    public record PageView<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
        public static <T> PageView<T> of(Page<T> page) {
            return new PageView<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }

    // ------------------------------------------------------------- eligibility

    public record RuleResult(String rule, boolean passed, String message) {
    }

    public record EligibilityReport(boolean eligible, List<RuleResult> rules) {
    }

    public record EligibilityResponse(
            Long applicationId,
            EligibilityStatus eligibilityStatus,
            ApplicationStatus applicationStatus,
            List<RuleResult> rules) {
    }

    // --------------------------------------------------- document review (advisory)

    public enum Severity {LOW, MEDIUM, HIGH}

    public record ReviewFlag(String code, Severity severity, String message) {
    }

    public record DocumentReviewReport(
            Long applicationId,
            String reviewer,
            String overall,
            List<ReviewFlag> flags,
            String notice) {
    }

    // ------------------------------------------------------- history & reference

    public record DecisionView(Long id, DecisionType decision, String reason, String decidedBy, Instant decidedAt) {
    }

    public record AuditView(Long id, String action, String fromValue, String toValue, String actor,
                            String details, Instant createdAt) {
    }

    public record AcademicYearView(Long id, String label, LocalDate startDate, LocalDate endDate,
                                   boolean applicationsOpen) {
    }

    public record GradeLevelView(Long id, String code, String name, int minAge, int maxAge,
                                 boolean requiresPreviousReport) {
    }

    public record OfferingView(Long id, String academicYear, String gradeLevel, String gradeLevelName,
                               int capacity, long admitted, long seatsRemaining, boolean applicationsOpen) {
    }
}
