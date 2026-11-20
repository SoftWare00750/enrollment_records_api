package com.school.enrolment.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests against a real PostgreSQL (Testcontainers) with the real Flyway schema and
 * the demo seed data. Skipped automatically when Docker is not available.
 *
 * Seed ids (fresh container): academic year 1 = 2026/2027 (open), grade 1 = PRI1 (2 seats),
 * grade 2 = PRI4 (needs previous report).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Testcontainers(disabledWithoutDocker = true)
class EnrolmentFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    // ------------------------------------------------------------------ helpers

    private static String applicationJson(String first, String last, String dob, String email,
                                          boolean withImmunisation) {
        String immunisation = withImmunisation ? "{\"type\":\"IMMUNISATION_RECORD\"}," : "";
        return """
                {"applicant":{"firstName":"%s","lastName":"%s","dateOfBirth":"%s"},
                 "guardian":{"fullName":"Parent of %s","email":"%s","phone":"+2348012345678"},
                 "academicYearId":1,"gradeLevelId":1,
                 "documents":[
                   {"type":"BIRTH_CERTIFICATE","nameOnDocument":"%s %s","dateOnDocument":"%s"},
                   %s
                   {"type":"PASSPORT_PHOTO"}]}
                """.formatted(first, last, dob, first, email, first, last, dob, immunisation);
    }

    private long register(String first, String last, String dob, String email, boolean withImmunisation)
            throws Exception {
        String response = mvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Actor", "test.registrar")
                        .content(applicationJson(first, last, dob, email, withImmunisation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.eligibilityStatus").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void checkEligibility(long id, String expected) throws Exception {
        mvc.perform(post("/api/applications/{id}/eligibility-check", id).header("X-Actor", "test.registrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibilityStatus").value(expected));
    }

    private org.springframework.test.web.servlet.ResultActions decide(long id, String decision) throws Exception {
        return mvc.perform(post("/api/applications/{id}/decision", id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Actor", "test.registrar")
                .content("{\"decision\":\"" + decision + "\",\"reason\":\"Integration test\"}"));
    }

    // -------------------------------------------------------------------- tests

    @Test
    void capacityIsEnforced_waitlistWorks_andAuditTrailIsComplete() throws Exception {
        long ada = register("Ada", "Okafor", "2020-03-10", "ada.parent@example.com", true);
        long bola = register("Bola", "Adeyemi", "2020-05-02", "bola.parent@example.com", true);
        long chidi = register("Chidi", "Eze", "2020-08-19", "chidi.parent@example.com", true);

        checkEligibility(ada, "ELIGIBLE");
        checkEligibility(bola, "ELIGIBLE");
        checkEligibility(chidi, "ELIGIBLE");

        decide(ada, "ADMITTED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ADMITTED"));
        decide(bola, "ADMITTED").andExpect(status().isOk());
        decide(chidi, "ADMITTED").andExpect(status().isConflict());          // PRI1 has only 2 seats
        decide(chidi, "WAITLISTED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITLISTED"));

        mvc.perform(get("/api/applications/{id}/audit", ada))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", contains(
                        "SUBMITTED", "ELIGIBILITY_CHECKED", "STATUS_CHANGED", "DECISION_ADMITTED")))
                .andExpect(jsonPath("$[0].actor").value("test.registrar"));

        mvc.perform(get("/api/applications/{id}/decisions", chidi))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].decision").value("WAITLISTED"));

        mvc.perform(get("/api/applications").param("status", "ADMITTED").param("gradeLevelId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void ineligibleApplicant_becomesEligible_afterTheMissingDocumentIsAdded() throws Exception {
        long id = register("Emeka", "Obi", "2020-01-01", "emeka.parent@example.com", false);

        checkEligibility(id, "INELIGIBLE");
        decide(id, "ADMITTED").andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/applications/{id}/documents", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"IMMUNISATION_RECORD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eligibilityStatus").value("PENDING"));

        checkEligibility(id, "ELIGIBLE");
    }

    @Test
    void applicantOutsideTheAgeRange_isIneligible_andCanBeRejected() throws Exception {
        long id = register("Dayo", "Bello", "2012-01-01", "dayo.parent@example.com", true);

        mvc.perform(post("/api/applications/{id}/eligibility-check", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibilityStatus").value("INELIGIBLE"))
                .andExpect(jsonPath("$.rules[?(@.rule=='AGE_RANGE')].passed").value(false));

        decide(id, "REJECTED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        decide(id, "ADMITTED").andExpect(status().isUnprocessableEntity());   // terminal
    }

    @Test
    void duplicateOpenApplication_isRejectedWith409() throws Exception {
        register("Femi", "Lawal", "2020-02-02", "femi.parent@example.com", true);

        mvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("Femi", "Lawal", "2020-02-02", "femi.parent@example.com", true)))
                .andExpect(status().isConflict());
    }

    @Test
    void closedAcademicYear_doesNotAcceptApplications() throws Exception {
        String body = """
                {"applicant":{"firstName":"Gina","lastName":"Musa","dateOfBirth":"2019-04-04"},
                 "guardian":{"fullName":"Parent of Gina","email":"gina.parent@example.com","phone":"+2348012345678"},
                 "academicYearId":2,"gradeLevelId":1,"documents":[]}
                """;

        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void auditLog_isAppendOnly_theDatabaseRefusesUpdatesAndDeletes() throws Exception {
        long id = register("Hauwa", "Garba", "2020-06-06", "hauwa.parent@example.com", true);
        mvc.perform(get("/api/applications/{id}/audit", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        assertThatThrownBy(() -> jdbc.update("update audit_log set actor = 'tampered' where entity_id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from audit_log where entity_id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }
}
