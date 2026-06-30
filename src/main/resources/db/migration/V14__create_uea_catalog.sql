CREATE TABLE ueas (
    id                  BIGSERIAL PRIMARY KEY,
    graduate_program_id BIGINT       NOT NULL REFERENCES graduate_programs(id),
    clave               VARCHAR(20)  NOT NULL,
    nombre              VARCHAR(200) NOT NULL,
    tipo                VARCHAR(20)  NOT NULL,
    modalidad           VARCHAR(10)  NOT NULL DEFAULT 'MIXTA',
    horas_teoria        NUMERIC(5,1) NOT NULL,
    horas_practica      NUMERIC(5,1) NOT NULL,
    tipo_formacion      VARCHAR(20)  NOT NULL,
    creditos            INTEGER      NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ueas_program_clave UNIQUE (graduate_program_id, clave),
    CONSTRAINT chk_ueas_creditos_positive CHECK (creditos > 0),
    CONSTRAINT chk_ueas_horas_non_negative CHECK (horas_teoria >= 0 AND horas_practica >= 0)
);

CREATE INDEX idx_ueas_program_active ON ueas (graduate_program_id, active);
CREATE INDEX idx_ueas_program_clave ON ueas (graduate_program_id, clave);
