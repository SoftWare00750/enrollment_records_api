package com.school.enrolment.service;

import com.school.enrolment.domain.Application;
import com.school.enrolment.web.ApiDtos.DocumentReviewReport;

/**
 * Optional "AI enrolment document checker" extension point. It only ever ADVISES: it flags
 * missing or inconsistent applicant information for staff review and never changes an
 * application's status. The rule-based implementation is the default; an API-backed
 * implementation can replace it without touching the rest of the system.
 */
public interface DocumentReviewer {

    DocumentReviewReport review(Application application);
}
