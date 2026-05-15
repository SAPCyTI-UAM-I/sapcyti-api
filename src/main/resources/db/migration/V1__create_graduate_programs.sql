-- V1__create_graduate_programs.sql
-- SPEC-004: GraduateProgram aggregate root — Program Configuration (BC-04)
-- Drivers: QA-3 (parameterization), QA-4 (multi-graduate program support)

CREATE TABLE graduate_programs (
    id       BIGSERIAL    PRIMARY KEY,
    name     VARCHAR(200) NOT NULL,
    division VARCHAR(100) NOT NULL,
    CONSTRAINT uq_graduate_program_name UNIQUE (name)
);
