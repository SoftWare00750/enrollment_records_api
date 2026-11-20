package com.school.enrolment.domain;

import java.util.EnumSet;
import java.util.Set;

public enum DocumentType {
    BIRTH_CERTIFICATE, IMMUNISATION_RECORD, PASSPORT_PHOTO, PREVIOUS_REPORT;

    /** Documents every applicant for this grade must supply. */
    public static Set<DocumentType> requiredFor(GradeLevel grade) {
        EnumSet<DocumentType> required = EnumSet.of(BIRTH_CERTIFICATE, IMMUNISATION_RECORD, PASSPORT_PHOTO);
        if (grade.isRequiresPreviousReport()) {
            required.add(PREVIOUS_REPORT);
        }
        return required;
    }
}
