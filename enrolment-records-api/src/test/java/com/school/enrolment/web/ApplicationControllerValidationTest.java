package com.school.enrolment.web;

import com.school.enrolment.exception.ApiException;
import com.school.enrolment.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP-layer checks: request validation and the error format. No database needed. */
@WebMvcTest(ApplicationController.class)
class ApplicationControllerValidationTest {

    @Autowired MockMvc mvc;
    @MockBean ApplicationService service;

    @Test
    void emptyBody_returns400_withFieldErrors() throws Exception {
        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.applicant").exists())
                .andExpect(jsonPath("$.errors.guardian").exists())
                .andExpect(jsonPath("$.errors.academicYearId").exists())
                .andExpect(jsonPath("$.errors.gradeLevelId").exists());
    }

    @Test
    void invalidNestedFields_areReportedWithTheirPath() throws Exception {
        String body = """
                {"applicant":{"firstName":"","lastName":"Okafor","dateOfBirth":"2999-01-01"},
                 "guardian":{"fullName":"Ngozi Okafor","email":"not-an-email","phone":"abc"},
                 "academicYearId":1,"gradeLevelId":1}
                """;

        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['applicant.firstName']").exists())
                .andExpect(jsonPath("$.errors['applicant.dateOfBirth']").exists())
                .andExpect(jsonPath("$.errors['guardian.email']").exists())
                .andExpect(jsonPath("$.errors['guardian.phone']").exists());
    }

    @Test
    void unknownDocumentType_returns400_notA500() throws Exception {
        mvc.perform(post("/api/applications/1/documents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SELFIE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void decisionWithoutReason_returns400() throws Exception {
        mvc.perform(post("/api/applications/1/decision").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ADMITTED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
    }

    @Test
    void nonNumericId_returns400() throws Exception {
        mvc.perform(get("/api/applications/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void businessRuleFailure_isMappedToItsStatus_asProblemDetail() throws Exception {
        when(service.get(eq(7L))).thenThrow(ApiException.notFound("Application 7 not found."));

        mvc.perform(get("/api/applications/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Application 7 not found."));
    }

    @Test
    void conflict_isMappedTo409() throws Exception {
        when(service.withdraw(eq(7L), any())).thenThrow(ApiException.conflict("nope"));

        mvc.perform(post("/api/applications/7/withdraw")).andExpect(status().isConflict());
    }
}
