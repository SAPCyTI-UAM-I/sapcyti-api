CREATE TABLE trimestral_plans (
    id                  BIGSERIAL PRIMARY KEY,
    graduate_program_id BIGINT       NOT NULL REFERENCES graduate_programs(id),
    survey_id           BIGINT       NOT NULL REFERENCES enrollment_surveys(id),
    term                VARCHAR(4)   NOT NULL,
    status              VARCHAR(10)  NOT NULL CHECK (status IN ('BORRADOR', 'TERMINADA')),
    created_by          BIGINT       NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    exported_at         TIMESTAMPTZ,
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
    max_groups          VARCHAR(5),
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
    vie_lab             BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Professors are a child list because a group may have co-directors. NEMP/name are snapshots.
CREATE TABLE trimestral_plan_group_professors (
    id                  BIGSERIAL PRIMARY KEY,
    group_id            BIGINT       NOT NULL REFERENCES trimestral_plan_groups(id) ON DELETE CASCADE,
    professor_id        BIGINT       NOT NULL REFERENCES professors(id),
    employee_number     VARCHAR(20),
    professor_name      VARCHAR(300) NOT NULL,
    posicion            SMALLINT     NOT NULL CHECK (posicion > 0),
    CONSTRAINT uq_trimestral_group_professor UNIQUE (group_id, professor_id),
    CONSTRAINT uq_trimestral_group_professor_position UNIQUE (group_id, posicion)
);

CREATE TABLE trimestral_plan_group_students (
    id                  BIGSERIAL PRIMARY KEY,
    group_id            BIGINT       NOT NULL REFERENCES trimestral_plan_groups(id) ON DELETE CASCADE,
    student_id          BIGINT       NOT NULL REFERENCES students(id),
    enrollment_id       VARCHAR(20)  NOT NULL,
    full_name           VARCHAR(300) NOT NULL,
    source              VARCHAR(6)   NOT NULL CHECK (source IN ('SURVEY', 'MANUAL')),
    academic_term       VARCHAR(4),
    obs                 VARCHAR(255),
    posicion            SMALLINT     NOT NULL CHECK (posicion > 0),
    CONSTRAINT uq_trimestral_group_student UNIQUE (group_id, student_id),
    CONSTRAINT uq_trimestral_group_student_position UNIQUE (group_id, posicion)
);

CREATE TABLE trimestral_plan_unassigned_demand (
    id                  BIGSERIAL PRIMARY KEY,
    trimestral_plan_id  BIGINT       NOT NULL REFERENCES trimestral_plans(id) ON DELETE CASCADE,
    uea_id              BIGINT       NOT NULL REFERENCES ueas(id),
    clave               VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    student_id          BIGINT       NOT NULL REFERENCES students(id),
    enrollment_id       VARCHAR(20)  NOT NULL,
    full_name           VARCHAR(300) NOT NULL,
    academic_term       VARCHAR(4),
    reason              VARCHAR(32)  NOT NULL CHECK (reason IN (
        'UEA_NOT_OFFERED', 'GROUP_LIMIT_REACHED', 'GROUP_SUFFIX_LIMIT', 'MANUALLY_UNASSIGNED'
    )),
    posicion            SMALLINT     NOT NULL CHECK (posicion > 0),
    CONSTRAINT uq_trimestral_unassigned_pair UNIQUE (trimestral_plan_id, uea_id, student_id)
);

CREATE TABLE trimestral_plan_outdated_reasons (
    id                  BIGSERIAL PRIMARY KEY,
    trimestral_plan_id  BIGINT       NOT NULL REFERENCES trimestral_plans(id) ON DELETE CASCADE,
    reason              VARCHAR(32)  NOT NULL CHECK (reason IN ('SURVEY_REOPENED', 'ANNUAL_PLAN_CHANGED')),
    CONSTRAINT uq_trimestral_outdated_reason UNIQUE (trimestral_plan_id, reason)
);

CREATE INDEX idx_trimestral_plans_program_term ON trimestral_plans (graduate_program_id, term);
CREATE INDEX idx_trimestral_plan_groups_plan ON trimestral_plan_groups (trimestral_plan_id, posicion);
CREATE INDEX idx_trimestral_plan_warnings_plan ON trimestral_plan_warnings (trimestral_plan_id);
CREATE INDEX idx_trimestral_plan_group_students_group ON trimestral_plan_group_students (group_id, posicion);
CREATE INDEX idx_trimestral_plan_group_professors_group ON trimestral_plan_group_professors (group_id, posicion);
CREATE INDEX idx_trimestral_unassigned_plan ON trimestral_plan_unassigned_demand (trimestral_plan_id, posicion);
CREATE INDEX idx_trimestral_outdated_plan ON trimestral_plan_outdated_reasons (trimestral_plan_id);
