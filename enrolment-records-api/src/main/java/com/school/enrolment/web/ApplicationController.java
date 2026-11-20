package com.school.enrolment.web;

import com.school.enrolment.domain.ApplicationStatus;
import com.school.enrolment.service.ApplicationService;
import com.school.enrolment.web.ApiDtos.ApplicationResponse;
import com.school.enrolment.web.ApiDtos.ApplicationSummary;
import com.school.enrolment.web.ApiDtos.AuditView;
import com.school.enrolment.web.ApiDtos.CreateApplicationRequest;
import com.school.enrolment.web.ApiDtos.DecisionRequest;
import com.school.enrolment.web.ApiDtos.DecisionView;
import com.school.enrolment.web.ApiDtos.DocumentDto;
import com.school.enrolment.web.ApiDtos.DocumentReviewReport;
import com.school.enrolment.web.ApiDtos.EligibilityResponse;
import com.school.enrolment.web.ApiDtos.PageView;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final String ACTOR_HEADER = "X-Actor";

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> register(
            @Valid @RequestBody CreateApplicationRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor) {
        ApplicationResponse created = service.register(request, actor(actor));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageView<ApplicationSummary> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long gradeLevelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(status, academicYearId, gradeLevelId, page, size);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<ApplicationResponse> addDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentDto document,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor) {
        return ResponseEntity.status(201).body(service.addDocument(id, document, actor(actor)));
    }

    @PostMapping("/{id}/eligibility-check")
    public EligibilityResponse checkEligibility(
            @PathVariable Long id,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor) {
        return service.checkEligibility(id, actor(actor));
    }

    /** Optional advisory checker: flags missing/inconsistent documents for staff; changes nothing. */
    @GetMapping("/{id}/document-review")
    public DocumentReviewReport reviewDocuments(@PathVariable Long id) {
        return service.reviewDocuments(id);
    }

    @PostMapping("/{id}/decision")
    public ApplicationResponse decide(
            @PathVariable Long id,
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor) {
        return service.decide(id, request, actor(actor));
    }

    @PostMapping("/{id}/withdraw")
    public ApplicationResponse withdraw(
            @PathVariable Long id,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actor) {
        return service.withdraw(id, actor(actor));
    }

    @GetMapping("/{id}/decisions")
    public List<DecisionView> decisions(@PathVariable Long id) {
        return service.decisions(id);
    }

    @GetMapping("/{id}/audit")
    public List<AuditView> audit(@PathVariable Long id) {
        return service.audit(id);
    }

    /**
     * Stand-in for authentication: who performed the action is taken from the X-Actor header.
     * A real deployment would derive this from the authenticated principal instead.
     */
    private static String actor(String header) {
        if (header == null || header.isBlank()) {
            return "anonymous";
        }
        String trimmed = header.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }
}
