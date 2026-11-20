package com.school.enrolment;

import com.school.enrolment.domain.AcademicYear;
import com.school.enrolment.domain.Applicant;
import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationDocument;
import com.school.enrolment.domain.ApplicationStatus;
import com.school.enrolment.domain.ClassOffering;
import com.school.enrolment.domain.DocumentType;
import com.school.enrolment.domain.EligibilityStatus;
import com.school.enrolment.domain.GradeLevel;
import com.school.enrolment.domain.Guardian;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

/** Builders shared by the unit tests. */
public final class TestData {

    public static final LocalDate YEAR_START = LocalDate.of(2026, 9, 14);

    private TestData() {
    }

    public static GradeLevel primary1() {
        return new GradeLevel("PRI1", "Primary 1", 5, 7, false);
    }

    public static GradeLevel primary4() {
        return new GradeLevel("PRI4", "Primary 4", 8, 10, true);
    }

    public static ClassOffering offering(GradeLevel grade, int capacity) {
        AcademicYear year = new AcademicYear("2026/2027", YEAR_START, LocalDate.of(2027, 7, 23), true);
        ClassOffering offering = new ClassOffering(year, grade, capacity);
        ReflectionTestUtils.setField(offering, "id", 10L);
        return offering;
    }

    /** A new SUBMITTED application with no documents. */
    public static Application application(ClassOffering offering, String first, String last, LocalDate dob,
                                          String previousSchool) {
        Guardian guardian = new Guardian("Ngozi " + last, "parent@example.com", "+2348012345678");
        Applicant applicant = new Applicant(first, last, dob, previousSchool, guardian);
        Application application = new Application("ENR-2026-000001", applicant, offering);
        ReflectionTestUtils.setField(application, "id", 1L);
        return application;
    }

    /** Adds the three documents every grade requires, with matching name and date. */
    public static Application withStandardDocuments(Application application) {
        Applicant a = application.getApplicant();
        application.addDocument(new ApplicationDocument(DocumentType.BIRTH_CERTIFICATE, a.fullName(),
                a.getDateOfBirth()));
        application.addDocument(new ApplicationDocument(DocumentType.IMMUNISATION_RECORD, null, null));
        application.addDocument(new ApplicationDocument(DocumentType.PASSPORT_PHOTO, null, null));
        return application;
    }

    public static Application inState(Application application, ApplicationStatus status,
                                      EligibilityStatus eligibility) {
        if (eligibility != EligibilityStatus.PENDING) {
            application.changeEligibility(eligibility);
        }
        if (status != ApplicationStatus.SUBMITTED) {
            application.changeStatus(status);
        }
        return application;
    }
}
