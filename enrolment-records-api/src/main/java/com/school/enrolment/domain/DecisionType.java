package com.school.enrolment.domain;

public enum DecisionType {
    ADMITTED(ApplicationStatus.ADMITTED),
    WAITLISTED(ApplicationStatus.WAITLISTED),
    REJECTED(ApplicationStatus.REJECTED);

    private final ApplicationStatus resultingStatus;

    DecisionType(ApplicationStatus resultingStatus) {
        this.resultingStatus = resultingStatus;
    }

    public ApplicationStatus toStatus() {
        return resultingStatus;
    }
}
