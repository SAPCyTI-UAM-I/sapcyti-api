-- V2__create_configuration_parameters.sql
-- SPEC-005: ConfigurationParameter — persistence and multi-tenant isolation (BC-04)
-- Drivers: QA-3 (parameterization without code changes), QA-4 (isolation per program)

CREATE TABLE configuration_parameters (
    id                   BIGSERIAL    PRIMARY KEY,
    graduate_program_id  BIGINT       NOT NULL
        REFERENCES graduate_programs(id) ON DELETE CASCADE,
    param_key            VARCHAR(100) NOT NULL,
    param_value          VARCHAR(500) NOT NULL,
    description          VARCHAR(500),
    CONSTRAINT uq_configuration_parameters_program_key
        UNIQUE (graduate_program_id, param_key)
);

CREATE INDEX idx_configuration_parameters_program
    ON configuration_parameters(graduate_program_id);
