CREATE TABLE trimestral_plans (
    id                  BIGSERIAL PRIMARY KEY,
    graduate_program_id BIGINT       NOT NULL REFERENCES graduate_programs(id),
    survey_id           BIGINT       NOT NULL REFERENCES enrollment_surveys(id),
    term                VARCHAR(4)   NOT NULL,
    status              VARCHAR(10)  NOT NULL CHECK (status IN ('BORRADOR', 'TERMINADA')),
    outdated            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by          BIGINT       NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_trimestral_plans_program_term UNIQUE (graduate_program_id, term)
);

CREATE TABLE trimestral_plan_warnings (
    id                  BIGSERIAL PRIMARY KEY,
    trimestral_plan_id  BIGINT       NOT NULL REFERENCES trimestral_plans(id) ON DELETE CASCADE,
    code                VARCHAR(40)  NOT NULL,
    clave               VARCHAR(20),
    enrollment_id       VARCHAR(20),
    employee_number     VARCHAR(20),
    group_id            BIGINT
);

CREATE TABLE trimestral_plan_groups (
    id                  BIGSERIAL PRIMARY KEY,
    trimestral_plan_id  BIGINT       NOT NULL REFERENCES trimestral_plans(id) ON DELETE CASCADE,
    uea_id              BIGINT       NOT NULL REFERENCES ueas(id),
    posicion            SMALLINT     NOT NULL,
    clave               VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    tipo_uea            VARCHAR(20)  NOT NULL,
    grupo               VARCHAR(10),
    cupo                VARCHAR(5),
    professor_id        BIGINT       REFERENCES professors(id),
    employee_number     VARCHAR(20),
    professor_name      VARCHAR(300),
    lun_ini             VARCHAR(5),
    lun_fin             VARCHAR(5),
    lun_lab             BOOLEAN      NOT NULL DEFAULT FALSE,
    mar_ini             VARCHAR(5),
    mar_fin             VARCHAR(5),
    mar_lab             BOOLEAN      NOT NULL DEFAULT FALSE,
    mie_ini             VARCHAR(5),
    mie_fin             VARCHAR(5),
    mie_lab             BOOLEAN      NOT NULL DEFAULT FALSE,
    jue_ini             VARCHAR(5),
    jue_fin             VARCHAR(5),
    jue_lab             BOOLEAN      NOT NULL DEFAULT FALSE,
    vie_ini             VARCHAR(5),
    vie_fin             VARCHAR(5),
    vie_lab             BOOLEAN      NOT NULL DEFAULT FALSE,
    obs                 TEXT
);

CREATE TABLE trimestral_plan_group_students (
    id                  BIGSERIAL PRIMARY KEY,
    group_id            BIGINT       NOT NULL REFERENCES trimestral_plan_groups(id) ON DELETE CASCADE,
    student_id          BIGINT       NOT NULL REFERENCES students(id),
    enrollment_id       VARCHAR(20)  NOT NULL,
    full_name           VARCHAR(300) NOT NULL,
    source              VARCHAR(6)   NOT NULL CHECK (source IN ('SURVEY', 'MANUAL')),
    academic_term       VARCHAR(4),
    posicion            SMALLINT     NOT NULL
);

CREATE INDEX idx_trimestral_plans_program_term ON trimestral_plans (graduate_program_id, term);
CREATE INDEX idx_trimestral_plan_groups_plan ON trimestral_plan_groups (trimestral_plan_id, posicion);
CREATE INDEX idx_trimestral_plan_warnings_plan ON trimestral_plan_warnings (trimestral_plan_id);
CREATE INDEX idx_trimestral_plan_group_students_group ON trimestral_plan_group_students (group_id, posicion);
