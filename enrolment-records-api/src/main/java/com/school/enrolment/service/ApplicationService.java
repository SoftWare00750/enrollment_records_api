package com.school.enrolment.service;

import com.school.enrolment.domain.AcademicYear;
import com.school.enrolment.domain.AdmissionDecision;
import com.school.enrolment.domain.Applicant;
import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationDocument;
import com.school.enrolment.domain.ApplicationStatus;
import com.school.enrolment.domain.ClassOffering;
import com.school.enrolment.domain.DecisionType;
import com.school.enrolment.domain.DocumentType;
import com.school.enrolment.domain.EligibilityStatus;
import com.school.enrolment.domain.GradeLevel;
import com.school.enrolment.domain.Guardian;
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
import com.school.enrolment.web.ApiDtos.ApplicationSummary;
import com.school.enrolment.web.ApiDtos.AuditView;
import com.school.enrolment.web.ApiDtos.CreateApplicationRequest;
import com.school.enrolment.web.ApiDtos.DecisionRequest;
import com.school.enrolment.web.ApiDtos.DecisionView;
import com.school.enrolment.web.ApiDtos.DocumentDto;
import com.school.enrolment.web.ApiDtos.DocumentReviewReport;
import com.school.enrolment.web.ApiDtos.EligibilityReport;
import com.school.enrolment.web.ApiDtos.EligibilityResponse;
import com.school.enrolment.web.ApiDtos.PageView;
import com.school.enrolment.web.ApiDtos.RuleResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the enrolment workflow. Every state change and its audit row are written in the
 * same transaction.
 */
@Service
public class ApplicationService {

    private static final String ENTITY = "APPLICATION";
    private static final Set<ApplicationStatus> CLOSED = EnumSet.of(ApplicationStatus.REJECTED,
            ApplicationStatus.WITHDRAWN);

    private final ApplicationRepository applicationRepo;
    private final ClassOfferingRepository offeringRepo;
    private final AcademicYearRepository yearRepo;
    private final GradeLevelRepository gradeRepo;
    private final GuardianRepository guardianRepo;
    private final ApplicantRepository applicantRepo;
    private final AdmissionDecisionRepository decisionRepo;
    private final AuditEntryRepository auditRepo;
    private final EligibilityService eligibilityService;
    private final DocumentReviewer documentReviewer;
    private final AuditService auditService;

    public ApplicationService(ApplicationRepository applicationRepo,
                              ClassOfferingRepository offeringRepo,
                              AcademicYearRepository yearRepo,
                              GradeLevelRepository gradeRepo,
                              GuardianRepository guardianRepo,
                              ApplicantRepository applicantRepo,
                              AdmissionDecisionRepository decisionRepo,
                              AuditEntryRepository auditRepo,
                              EligibilityService eligibilityService,
                              DocumentReviewer documentReviewer,
                              AuditService auditService) {
        this.applicationRepo = applicationRepo;
        this.offeringRepo = offeringRepo;
        this.yearRepo = yearRepo;
        this.gradeRepo = gradeRepo;
        this.guardianRepo = guardianRepo;
        this.applicantRepo = applicantRepo;
        this.decisionRepo = decisionRepo;
        this.auditRepo = auditRepo;
        this.eligibilityService = eligibilityService;
        this.documentReviewer = documentReviewer;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------ registration

    @Transactional
    public ApplicationResponse register(CreateApplicationRequest request, String actor) {
        AcademicYear year = yearRepo.findById(request.academicYearId())
                .orElseThrow(() -> ApiException.notFound("Academic year " + request.academicYearId() + " not found."));
        GradeLevel grade = gradeRepo.findById(request.gradeLevelId())
                .orElseThrow(() -> ApiException.notFound("Grade level " + request.gradeLevelId() + " not found."));
        ClassOffering offering = offeringRepo.findByAcademicYearIdAndGradeLevelId(year.getId(), grade.getId())
                .orElseThrow(() -> ApiException.unprocessable(
                        grade.getCode() + " is not offered in " + year.getLabel() + "."));

        if (!year.isApplicationsOpen()) {
            throw ApiException.unprocessable("Applications for " + year.getLabel() + " are closed.");
        }

        List<DocumentDto> documents = request.documents() == null ? List.of() : request.documents();
        Set<DocumentType> seen = EnumSet.noneOf(DocumentType.class);
        for (DocumentDto document : documents) {
            if (!seen.add(document.type())) {
                throw ApiException.unprocessable("Document type " + document.type() + " was supplied more than once.");
            }
        }

        String email = request.guardian().email().trim().toLowerCase(Locale.ROOT);
        Guardian guardian = guardianRepo.findByEmail(email)
                .orElseGet(() -> guardianRepo.save(new Guardian(
                        request.guardian().fullName().trim(), email, request.guardian().phone().trim())));

        String firstName = request.applicant().firstName().trim();
        String lastName = request.applicant().lastName().trim();
        Applicant applicant = applicantRepo
                .findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirthAndGuardianId(
                        firstName, lastName, request.applicant().dateOfBirth(), guardian.getId())
                .orElseGet(() -> applicantRepo.save(new Applicant(firstName, lastName,
                        request.applicant().dateOfBirth(), clean(request.applicant().previousSchool()), guardian)));

        if (applicationRepo.existsByApplicantIdAndOfferingAcademicYearIdAndStatusNotIn(
                applicant.getId(), year.getId(), CLOSED)) {
            throw ApiException.conflict(applicant.fullName() + " already has an open application for "
                    + year.getLabel() + ".");
        }

        String referenceNo = "ENR-%d-%06d".formatted(year.getStartDate().getYear(), applicationRepo.nextReferenceNumber());
        Application application = new Application(referenceNo, applicant, offering);
        for (DocumentDto document : documents) {
            application.addDocument(new ApplicationDocument(document.type(), clean(document.nameOnDocument()),
                    document.dateOnDocument()));
        }
        applicationRepo.save(application);

        auditService.record(ENTITY, application.getId(), "SUBMITTED", null, ApplicationStatus.SUBMITTED.name(),
                actor, "Reference " + referenceNo + " for " + grade.getCode() + " " + year.getLabel());
        return Mappers.toResponse(application);
    }

    @Transactional
    public ApplicationResponse addDocument(Long id, DocumentDto dto, String actor) {
        Application application = load(id);
        requireDecidable(application, "Documents can only be added while the application is open");
        boolean alreadyThere = application.getDocuments().stream().anyMatch(d -> d.getDocType() == dto.type());
        if (alreadyThere) {
            throw ApiException.conflict("A " + dto.type() + " document has already been received.");
        }

        EligibilityStatus previous = application.getEligibilityStatus();
        application.addDocument(new ApplicationDocument(dto.type(), clean(dto.nameOnDocument()), dto.dateOnDocument()));
        application.changeEligibility(EligibilityStatus.PENDING);

        auditService.record(ENTITY, id, "DOCUMENT_ADDED", null, dto.type().name(), actor,
                "Eligibility reset from " + previous + " to PENDING; re-run the eligibility check.");
        return Mappers.toResponse(application);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public ApplicationResponse get(Long id) {
        return Mappers.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public PageView<ApplicationSummary> list(ApplicationStatus status, Long academicYearId, Long gradeLevelId,
                                             int page, int size) {
        Specification<Application> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (academicYearId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("offering").get("academicYear").get("id"), academicYearId));
        }
        if (gradeLevelId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("offering").get("gradeLevel").get("id"), gradeLevelId));
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Application> result = applicationRepo.findAll(spec, pageable);
        return PageView.of(result.map(Mappers::toSummary));
    }

    @Transactional(readOnly = true)
    public List<DecisionView> decisions(Long id) {
        requireExists(id);
        return decisionRepo.findByApplicationIdOrderByIdAsc(id).stream()
                .map(d -> new DecisionView(d.getId(), d.getDecision(), d.getReason(), d.getDecidedBy(),
                        d.getDecidedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditView> audit(Long id) {
        requireExists(id);
        return auditRepo.findByEntityTypeAndEntityIdOrderByIdAsc(ENTITY, id).stream()
                .map(a -> new AuditView(a.getId(), a.getAction(), a.getFromValue(), a.getToValue(), a.getActor(),
                        a.getDetails(), a.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentReviewReport reviewDocuments(Long id) {
        return documentReviewer.review(load(id));
    }

    // --------------------------------------------------------------- eligibility

    @Transactional
    public EligibilityResponse checkEligibility(Long id, String actor) {
        Application application = load(id);
        requireDecidable(application, "Eligibility can only be checked while the application is open");

        EligibilityReport report = eligibilityService.evaluate(application);
        EligibilityStatus previous = application.getEligibilityStatus();
        EligibilityStatus outcome = report.eligible() ? EligibilityStatus.ELIGIBLE : EligibilityStatus.INELIGIBLE;
        application.changeEligibility(outcome);

        String failed = report.rules().stream().filter(r -> !r.passed()).map(RuleResult::message)
                .collect(Collectors.joining(" "));
        auditService.record(ENTITY, id, "ELIGIBILITY_CHECKED", previous.name(), outcome.name(), actor,
                report.eligible() ? "All rules passed." : failed);

        if (application.getStatus() == ApplicationStatus.SUBMITTED) {
            application.changeStatus(ApplicationStatus.UNDER_REVIEW);
            auditService.record(ENTITY, id, "STATUS_CHANGED", ApplicationStatus.SUBMITTED.name(),
                    ApplicationStatus.UNDER_REVIEW.name(), actor, "Eligibility check started review.");
        }
        return new EligibilityResponse(id, outcome, application.getStatus(), report.rules());
    }

    // ----------------------------------------------------------------- decisions

    @Transactional
    public ApplicationResponse decide(Long id, DecisionRequest request, String actor) {
        Application application = load(id);
        ApplicationStatus from = application.getStatus();
        DecisionType decision = request.decision();

        if (!from.isDecidable()) {
            throw ApiException.unprocessable("Application is " + from + " and can no longer receive a decision.");
        }
        if (decision == DecisionType.WAITLISTED && from == ApplicationStatus.WAITLISTED) {
            throw ApiException.unprocessable("Application is already waitlisted.");
        }
        if (decision != DecisionType.REJECTED && application.getEligibilityStatus() != EligibilityStatus.ELIGIBLE) {
            throw ApiException.unprocessable(
                    "Only an eligible application can be admitted or waitlisted. Run a passing eligibility check first.");
        }

        if (decision == DecisionType.ADMITTED) {
            // Lock the class row so concurrent admissions are serialised and seats cannot be oversold.
            ClassOffering offering = offeringRepo.findByIdForUpdate(application.getOffering().getId())
                    .orElseThrow(() -> ApiException.notFound("Class offering not found."));
            long admitted = applicationRepo.countByOfferingIdAndStatus(offering.getId(), ApplicationStatus.ADMITTED);
            if (admitted >= offering.getCapacity()) {
                throw ApiException.conflict("Class is full (" + admitted + " of " + offering.getCapacity()
                        + " seats taken). Waitlist the applicant instead.");
            }
        }

        application.changeStatus(decision.toStatus());
        decisionRepo.save(new AdmissionDecision(id, decision, request.reason().trim(), actor));
        auditService.record(ENTITY, id, "DECISION_" + decision.name(), from.name(), decision.toStatus().name(),
                actor, request.reason().trim());
        return Mappers.toResponse(application);
    }

    @Transactional
    public ApplicationResponse withdraw(Long id, String actor) {
        Application application = load(id);
        ApplicationStatus from = application.getStatus();
        if (!from.isWithdrawable()) {
            throw ApiException.unprocessable("Application is " + from + " and cannot be withdrawn.");
        }
        application.changeStatus(ApplicationStatus.WITHDRAWN);
        auditService.record(ENTITY, id, "WITHDRAWN", from.name(), ApplicationStatus.WITHDRAWN.name(), actor,
                from == ApplicationStatus.ADMITTED ? "Admitted place released." : null);
        return Mappers.toResponse(application);
    }

    // ------------------------------------------------------------------ helpers

    private Application load(Long id) {
        return applicationRepo.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Application " + id + " not found."));
    }

    private void requireExists(Long id) {
        if (!applicationRepo.existsById(id)) {
            throw ApiException.notFound("Application " + id + " not found.");
        }
    }

    private void requireDecidable(Application application, String prefix) {
        if (!application.getStatus().isDecidable()) {
            throw ApiException.unprocessable(prefix + " (current status: " + application.getStatus() + ").");
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
