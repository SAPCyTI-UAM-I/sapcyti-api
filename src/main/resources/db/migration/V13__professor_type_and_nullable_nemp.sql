-- BC-02 Academic Management — professor type and nullable employee number (SPEC-017B / HU-23, HU-24, HU-45)

ALTER TABLE professors
    ADD COLUMN professor_type VARCHAR(20) NOT NULL DEFAULT 'INTERNO';

ALTER TABLE professors
    ALTER COLUMN employee_number DROP NOT NULL;

ALTER TABLE professors DROP CONSTRAINT IF EXISTS uq_professors_employee_number;

CREATE UNIQUE INDEX uq_professors_active_interno_nemp
    ON professors (graduate_program_id, employee_number)
    WHERE professor_type = 'INTERNO' AND employee_number IS NOT NULL;
