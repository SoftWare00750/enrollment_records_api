package com.school.enrolment.service;

import com.school.enrolment.domain.AcademicYear;
import com.school.enrolment.domain.Applicant;
import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationDocument;
import com.school.enrolment.domain.ClassOffering;
import com.school.enrolment.domain.DocumentType;
import com.school.enrolment.domain.GradeLevel;
import com.school.enrolment.web.ApiDtos.EligibilityReport;
import com.school.enrolment.web.ApiDtos.RuleResult;
import org.springframework.stereotype.Service;

import java.time.Period;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure business rules for "is this applicant eligible for this class?". No database access and
 * no side effects, so every rule is easy to unit-test. Each rule reports pass/fail plus a
 * human-readable reason so staff can see exactly why an application failed.
 */
@Service
public class EligibilityService {

    public EligibilityReport evaluate(Application application) {
        ClassOffering offering = application.getOffering();
        GradeLevel grade = offering.getGradeLevel();
        AcademicYear year = offering.getAcademicYear();
        Applicant applicant = application.getApplicant();

        List<RuleResult> rules = new ArrayList<>();
        rules.add(ageRule(applicant, grade, year));
        rules.add(documentsRule(application, grade));
        if (grade.isRequiresPreviousReport()) {
            rules.add(previousSchoolRule(applicant, grade));
        }

        boolean eligible = rules.stream().allMatch(RuleResult::passed);
        return new EligibilityReport(eligible, List.copyOf(rules));
    }

    /** Age is measured on the first day of the academic year. */
    private RuleResult ageRule(Applicant applicant, GradeLevel grade, AcademicYear year) {
        int age = applicant.getDateOfBirth().isAfter(year.getStartDate())
                ? -1
                : Period.between(applicant.getDateOfBirth(), year.getStartDate()).getYears();
        boolean passed = age >= grade.getMinAge() && age <= grade.getMaxAge();
        String message = "Age on " + year.getStartDate() + " is " + (age < 0 ? "not yet born" : String.valueOf(age))
                + "; " + grade.getCode() + " accepts " + grade.getMinAge() + "-" + grade.getMaxAge() + ".";
        return new RuleResult("AGE_RANGE", passed, message);
    }

    private RuleResult documentsRule(Application application, GradeLevel grade) {
        Set<DocumentType> provided = application.getDocuments().stream()
                .map(ApplicationDocument::getDocType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DocumentType.class)));
        Set<DocumentType> missing = EnumSet.noneOf(DocumentType.class);
        missing.addAll(DocumentType.requiredFor(grade));
        missing.removeAll(provided);

        boolean passed = missing.isEmpty();
        String message = passed
                ? "All required documents received."
                : "Missing required documents: " + missing.stream().map(Enum::name).collect(Collectors.joining(", "))
                + ".";
        return new RuleResult("REQUIRED_DOCUMENTS", passed, message);
    }

    private RuleResult previousSchoolRule(Applicant applicant, GradeLevel grade) {
        String school = applicant.getPreviousSchool();
        boolean passed = school != null && !school.isBlank();
        String message = passed
                ? "Previous school recorded: " + school.trim() + "."
                : grade.getCode() + " requires the previous school to be recorded.";
        return new RuleResult("PREVIOUS_SCHOOL", passed, message);
    }
}
