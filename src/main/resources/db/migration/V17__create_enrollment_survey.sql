CREATE TABLE enrollment_surveys (
    id                  BIGSERIAL PRIMARY KEY,
    graduate_program_id BIGINT       NOT NULL REFERENCES graduate_programs(id),
    term                VARCHAR(4)   NOT NULL,
    opens_at            TIMESTAMPTZ  NOT NULL,
    closes_at           TIMESTAMPTZ  NOT NULL,
    intro_message       VARCHAR(500),
    closed_manually     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by          BIGINT       NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_enrollment_surveys_program_term UNIQUE (graduate_program_id, term),
    CONSTRAINT chk_enrollment_surveys_closes_after_opens CHECK (closes_at > opens_at)
);

CREATE INDEX idx_enrollment_surveys_program ON enrollment_surveys (graduate_program_id);

CREATE TABLE survey_snapshot_ueas (
    survey_id BIGINT NOT NULL REFERENCES enrollment_surveys(id) ON DELETE CASCADE,
    uea_id    BIGINT NOT NULL REFERENCES ueas(id),
    PRIMARY KEY (survey_id, uea_id)
);

CREATE INDEX idx_survey_snapshot_ueas_survey ON survey_snapshot_ueas (survey_id);

CREATE TABLE survey_responses (
    id            BIGSERIAL PRIMARY KEY,
    survey_id     BIGINT      NOT NULL REFERENCES enrollment_surveys(id) ON DELETE CASCADE,
    student_id    BIGINT      NOT NULL REFERENCES students(id),
    academic_term VARCHAR(4)  NOT NULL,
    mode          VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_survey_responses_survey_student UNIQUE (survey_id, student_id)
);

CREATE INDEX idx_survey_responses_survey ON survey_responses (survey_id);

CREATE TABLE survey_response_ueas (
    response_id BIGINT NOT NULL REFERENCES survey_responses(id) ON DELETE CASCADE,
    uea_id      BIGINT NOT NULL REFERENCES ueas(id),
    PRIMARY KEY (response_id, uea_id)
);

CREATE INDEX idx_survey_response_ueas_response ON survey_response_ueas (response_id);
