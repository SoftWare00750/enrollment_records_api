package com.school.enrolment.service;

import com.school.enrolment.domain.Applicant;
import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationDocument;
import com.school.enrolment.domain.ClassOffering;
import com.school.enrolment.domain.Guardian;
import com.school.enrolment.web.ApiDtos.ApplicantView;
import com.school.enrolment.web.ApiDtos.ApplicationResponse;
import com.school.enrolment.web.ApiDtos.ApplicationSummary;
import com.school.enrolment.web.ApiDtos.DocumentView;
import com.school.enrolment.web.ApiDtos.GuardianView;

import java.util.Comparator;
import java.util.List;

/** Entity -> DTO mapping. Must be called inside a transaction (touches lazy associations). */
final class Mappers {

    private Mappers() {
    }

    static ApplicationResponse toResponse(Application a) {
        Applicant applicant = a.getApplicant();
        Guardian guardian = applicant.getGuardian();
        ClassOffering offering = a.getOffering();

        List<DocumentView> documents = a.getDocuments().stream()
                .sorted(Comparator.comparing(ApplicationDocument::getDocType))
                .map(d -> new DocumentView(d.getDocType(), d.getNameOnDocument(), d.getDateOnDocument(),
                        d.getReceivedAt()))
                .toList();

        return new ApplicationResponse(
                a.getId(),
                a.getReferenceNo(),
                a.getStatus(),
                a.getEligibilityStatus(),
                offering.getAcademicYear().getLabel(),
                offering.getGradeLevel().getCode(),
                new ApplicantView(applicant.getFirstName(), applicant.getLastName(),
                        applicant.getDateOfBirth(), applicant.getPreviousSchool()),
                new GuardianView(guardian.getFullName(), guardian.getEmail(), guardian.getPhone()),
                documents,
                a.getSubmittedAt(),
                a.getUpdatedAt());
    }

    static ApplicationSummary toSummary(Application a) {
        ClassOffering offering = a.getOffering();
        return new ApplicationSummary(
                a.getId(),
                a.getReferenceNo(),
                a.getApplicant().fullName(),
                offering.getAcademicYear().getLabel(),
                offering.getGradeLevel().getCode(),
                a.getStatus(),
                a.getEligibilityStatus(),
                a.getSubmittedAt());
    }
}
