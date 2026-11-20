package com.school.enrolment.domain;

public enum ApplicationStatus {
    SUBMITTED, UNDER_REVIEW, WAITLISTED, ADMITTED, REJECTED, WITHDRAWN;

    /** Can staff still run eligibility checks / record a decision? */
    public boolean isDecidable() {
        return this == SUBMITTED || this == UNDER_REVIEW || this == WAITLISTED;
    }

    /** REJECTED and WITHDRAWN are terminal; an ADMITTED place can still be given up. */
    public boolean isWithdrawable() {
        return this != REJECTED && this != WITHDRAWN;
    }
}
