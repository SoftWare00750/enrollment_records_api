package com.school.enrolment.service;

import com.school.enrolment.domain.Applicant;
import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationDocument;
import com.school.enrolment.domain.DocumentType;
import com.school.enrolment.web.ApiDtos.DocumentReviewReport;
import com.school.enrolment.web.ApiDtos.ReviewFlag;
import com.school.enrolment.web.ApiDtos.Severity;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deterministic document cross-checks: names and dates written on documents versus what was
 * entered on the application. Anything it cannot confirm is flagged "for staff review" rather
 * than silently passed or failed.
 */
@Component
public class RuleBasedDocumentReviewer implements DocumentReviewer {

    static final String NOTICE =
            "Advisory only. These flags do not change the application; staff must confirm against the originals.";

    @Override
    public DocumentReviewReport review(Application application) {
        Applicant applicant = application.getApplicant();
        List<ReviewFlag> flags = new ArrayList<>();

        Map<DocumentType, ApplicationDocument> byType = application.getDocuments().stream()
                .collect(Collectors.toMap(ApplicationDocument::getDocType, Function.identity()));

        // 1. Missing documents
        for (DocumentType required : DocumentType.requiredFor(application.getOffering().getGradeLevel())) {
            if (!byType.containsKey(required)) {
                flags.add(new ReviewFlag("MISSING_DOCUMENT", Severity.HIGH,
                        required + " has not been received."));
            }
        }

        // 2. Name on each document vs applicant name
        Set<String> expected = tokens(applicant.getFirstName() + " " + applicant.getLastName());
        for (ApplicationDocument doc : application.getDocuments()) {
            Set<String> found = tokens(doc.getNameOnDocument());
            if (found.isEmpty()) {
                if (doc.getDocType() == DocumentType.BIRTH_CERTIFICATE) {
                    flags.add(new ReviewFlag("UNVERIFIED_NAME", Severity.LOW,
                            "No name was recorded for the birth certificate, so it could not be cross-checked."));
                }
                continue;
            }
            if (!found.containsAll(expected)) {
                boolean partial = found.stream().anyMatch(expected::contains);
                flags.add(partial
                        ? new ReviewFlag("NAME_PARTIAL_MATCH", Severity.MEDIUM, doc.getDocType()
                        + ": name \"" + doc.getNameOnDocument() + "\" only partly matches \""
                        + applicant.fullName() + "\".")
                        : new ReviewFlag("NAME_MISMATCH", Severity.HIGH, doc.getDocType()
                        + ": name \"" + doc.getNameOnDocument() + "\" does not match \""
                        + applicant.fullName() + "\"."));
            }
        }

        // 3. Birth certificate date vs date of birth
        ApplicationDocument birthCertificate = byType.get(DocumentType.BIRTH_CERTIFICATE);
        if (birthCertificate != null) {
            if (birthCertificate.getDateOnDocument() == null) {
                flags.add(new ReviewFlag("UNVERIFIED_DATE_OF_BIRTH", Severity.LOW,
                        "No date was recorded for the birth certificate, so the date of birth could not be cross-checked."));
            } else if (!birthCertificate.getDateOnDocument().equals(applicant.getDateOfBirth())) {
                flags.add(new ReviewFlag("DATE_OF_BIRTH_MISMATCH", Severity.HIGH,
                        "Birth certificate shows " + birthCertificate.getDateOnDocument()
                                + " but the application says " + applicant.getDateOfBirth() + "."));
            }
        }

        // 4. Previous school expected for this grade
        if (application.getOffering().getGradeLevel().isRequiresPreviousReport()
                && (applicant.getPreviousSchool() == null || applicant.getPreviousSchool().isBlank())) {
            flags.add(new ReviewFlag("MISSING_PREVIOUS_SCHOOL", Severity.MEDIUM,
                    "Previous school is not recorded for a grade that requires a previous report."));
        }

        String overall = flags.isEmpty() ? "NO_ISSUES_FOUND" : "NEEDS_STAFF_REVIEW";
        return new DocumentReviewReport(application.getId(), "rule-based", overall, List.copyOf(flags), NOTICE);
    }

    /** Lower-cased, accent-stripped set of name parts, so "Okafor, Ada" equals "ada okafor". */
    static Set<String> tokens(String text) {
        if (text == null) {
            return Set.of();
        }
        String cleaned = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\s]", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(cleaned.split("\\s+")));
    }
}
