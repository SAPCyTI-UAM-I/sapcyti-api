CREATE TABLE annual_plans (
    id                  BIGSERIAL PRIMARY KEY,
    graduate_program_id BIGINT       NOT NULL REFERENCES graduate_programs(id),
    year                INT          NOT NULL,
    status              VARCHAR(10)  NOT NULL CHECK (status IN ('BORRADOR', 'TERMINADA', 'ARCHIVADA')),
    created_by          BIGINT       NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_annual_plans_program_year UNIQUE (graduate_program_id, year)
);

CREATE TABLE annual_plan_entries (
    id             BIGSERIAL PRIMARY KEY,
    annual_plan_id BIGINT       NOT NULL REFERENCES annual_plans(id) ON DELETE CASCADE,
    uea_id         BIGINT       NOT NULL REFERENCES ueas(id),
    posicion       SMALLINT     NOT NULL,
    clave          VARCHAR(20)  NOT NULL,
    nombre         VARCHAR(200) NOT NULL,
    modalidad      VARCHAR(10)  NOT NULL,
    grupos_i       VARCHAR(5),
    cupo_i         VARCHAR(5),
    grupos_p       VARCHAR(5),
    cupo_p         VARCHAR(5),
    grupos_o       VARCHAR(5),
    cupo_o         VARCHAR(5),
    p_fis          VARCHAR(3),
    p_mat          VARCHAR(3),
    mcmai          VARCHAR(3),
    p_quim         VARCHAR(3),
    p_iquim        VARCHAR(3),
    p_ibiom        VARCHAR(3),
    pcyti          VARCHAR(3),
    pema           VARCHAR(3),
    efmc           VARCHAR(3),
    CONSTRAINT uq_annual_plan_entries_plan_uea UNIQUE (annual_plan_id, uea_id),
    CONSTRAINT chk_annual_plan_entries_grupos_i CHECK (grupos_i IS NULL OR grupos_i = '*' OR grupos_i ~ '^[0-9]+$'),
    CONSTRAINT chk_annual_plan_entries_cupo_i CHECK (cupo_i IS NULL OR cupo_i = '*' OR cupo_i ~ '^[0-9]+$'),
    CONSTRAINT chk_annual_plan_entries_grupos_p CHECK (grupos_p IS NULL OR grupos_p = '*' OR grupos_p ~ '^[0-9]+$'),
    CONSTRAINT chk_annual_plan_entries_cupo_p CHECK (cupo_p IS NULL OR cupo_p = '*' OR cupo_p ~ '^[0-9]+$'),
    CONSTRAINT chk_annual_plan_entries_grupos_o CHECK (grupos_o IS NULL OR grupos_o = '*' OR grupos_o ~ '^[0-9]+$'),
    CONSTRAINT chk_annual_plan_entries_cupo_o CHECK (cupo_o IS NULL OR cupo_o = '*' OR cupo_o ~ '^[0-9]+$'),
    CONSTRAINT chk_annual_plan_entries_p_fis CHECK (p_fis IS NULL OR p_fis IN ('X', 'O', 'X/O')),
    CONSTRAINT chk_annual_plan_entries_p_mat CHECK (p_mat IS NULL OR p_mat IN ('X', 'O', 'X/O')),
    CONSTRAINT chk_annual_plan_entries_mcmai CHECK (mcmai IS NULL OR mcmai IN ('X', 'O', 'X/O')),
    CONSTRAINT chk_annual_plan_entries_p_quim CHECK (p_quim IS NULL OR p_quim IN ('X', 'O', 'X/O')),
    CONSTRAINT chk_annual_plan_entries_p_iquim CHECK (p_iquim IS NULL OR p_iquim IN ('X', 'O', 'X/O')),
    CONSTRAINT chk_annual_plan_entries_p_ibiom CHECK (p_ibiom IS NULL OR p_ibiom IN ('X', 'O', 'X/O')),
    CONSTRAINT chk_annual_plan_entries_pcyti CHECK (pcyti IS NULL OR pcyti IN ('X', 'O', 'X/O')),
    CONSTRAINT chk_annual_plan_entries_pema CHECK (pema IS NULL OR pema IN ('X', 'O', 'X/O')),
    CONSTRAINT chk_annual_plan_entries_efmc CHECK (efmc IS NULL OR efmc IN ('X', 'O', 'X/O'))
);

CREATE INDEX idx_annual_plans_program_year ON annual_plans (graduate_program_id, year DESC);
CREATE INDEX idx_annual_plan_entries_plan_posicion ON annual_plan_entries (annual_plan_id, posicion);
