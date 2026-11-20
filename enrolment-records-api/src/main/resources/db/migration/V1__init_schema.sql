-- School Enrolment Records API : normalised schema (3NF)
-- Reference data ---------------------------------------------------------

CREATE TABLE academic_years (
    id                 BIGSERIAL PRIMARY KEY,
    label              VARCHAR(20) NOT NULL UNIQUE,
    start_date         DATE        NOT NULL,   -- also the cut-off date for age checks
    end_date           DATE        NOT NULL,
    applications_open  BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_year_dates CHECK (end_date > start_date)
);

CREATE TABLE grade_levels (
    id                        BIGSERIAL PRIMARY KEY,
    code                      VARCHAR(20)  NOT NULL UNIQUE,
    name                      VARCHAR(100) NOT NULL,
    min_age                   INTEGER      NOT NULL,
    max_age                   INTEGER      NOT NULL,
    requires_previous_report  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_age_range CHECK (min_age >= 0 AND max_age >= min_age)
);

-- A grade offered in a given academic year, with its seat capacity.
CREATE TABLE class_offerings (
    id                BIGSERIAL PRIMARY KEY,
    academic_year_id  BIGINT  NOT NULL REFERENCES academic_years (id),
    grade_level_id    BIGINT  NOT NULL REFERENCES grade_levels (id),
    capacity          INTEGER NOT NULL,
    CONSTRAINT uq_offering UNIQUE (academic_year_id, grade_level_id),
    CONSTRAINT chk_capacity CHECK (capacity > 0)
);

-- People --------------------------------------------------------------------

CREATE TABLE guardians (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    email       VARCHAR(254) NOT NULL UNIQUE,   -- stored lower-case by the service
    phone       VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE applicants (
    id               BIGSERIAL PRIMARY KEY,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    date_of_birth    DATE         NOT NULL,
    previous_school  VARCHAR(200),
    guardian_id      BIGINT       NOT NULL REFERENCES guardians (id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_applicants_identity ON applicants (lower(first_name), lower(last_name), date_of_birth);
CREATE INDEX idx_applicants_guardian ON applicants (guardian_id);

-- Applications --------------------------------------------------------------

CREATE SEQUENCE application_reference_seq START 1;

CREATE TABLE applications (
    id                  BIGSERIAL PRIMARY KEY,
    reference_no        VARCHAR(30) NOT NULL UNIQUE,
    applicant_id        BIGINT      NOT NULL REFERENCES applicants (id),
    offering_id         BIGINT      NOT NULL REFERENCES class_offerings (id),
    status              VARCHAR(20) NOT NULL,
    eligibility_status  VARCHAR(20) NOT NULL,
    submitted_at        TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    version             BIGINT      NOT NULL DEFAULT 0,   -- optimistic locking
    CONSTRAINT chk_app_status CHECK (status IN
        ('SUBMITTED','UNDER_REVIEW','WAITLISTED','ADMITTED','REJECTED','WITHDRAWN')),
    CONSTRAINT chk_app_eligibility CHECK (eligibility_status IN ('PENDING','ELIGIBLE','INELIGIBLE'))
);

-- One live application per applicant per class offering (re-applying is allowed
-- once the earlier one was rejected or withdrawn).
CREATE UNIQUE INDEX uq_active_application
    ON applications (applicant_id, offering_id)
    WHERE status NOT IN ('REJECTED','WITHDRAWN');
CREATE INDEX idx_applications_offering_status ON applications (offering_id, status);
CREATE INDEX idx_applications_status          ON applications (status);

CREATE TABLE application_documents (
    id                BIGSERIAL PRIMARY KEY,
    application_id    BIGINT      NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    doc_type          VARCHAR(30) NOT NULL,
    name_on_document  VARCHAR(200),
    date_on_document  DATE,
    received_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_document_type UNIQUE (application_id, doc_type),
    CONSTRAINT chk_doc_type CHECK (doc_type IN
        ('BIRTH_CERTIFICATE','IMMUNISATION_RECORD','PASSPORT_PHOTO','PREVIOUS_REPORT'))
);

-- Decision history: every admit / waitlist / reject, kept even if later superseded.
CREATE TABLE admission_decisions (
    id              BIGSERIAL PRIMARY KEY,
    application_id  BIGINT       NOT NULL REFERENCES applications (id),
    decision        VARCHAR(20)  NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    decided_by      VARCHAR(100) NOT NULL,
    decided_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_decision CHECK (decision IN ('ADMITTED','REJECTED','WAITLISTED'))
);
CREATE INDEX idx_decisions_application ON admission_decisions (application_id);

-- Audit history ---------------------------------------------------------------

CREATE TABLE audit_log (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    BIGINT       NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    from_value   VARCHAR(50),
    to_value     VARCHAR(50),
    actor        VARCHAR(100) NOT NULL,
    details      VARCHAR(1000),
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id, id);

-- The audit trail is append-only: the database itself refuses edits and deletes.
CREATE FUNCTION audit_log_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only (% not permitted)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_append_only();
