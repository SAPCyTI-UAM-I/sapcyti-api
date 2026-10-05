-- SPEC-037: ProgramStatus lifecycle — replace ACTIVO with EN_INVESTIGACION
UPDATE student_programs
SET status = 'EN_INVESTIGACION'
WHERE status = 'ACTIVO';

ALTER TABLE student_programs
    ALTER COLUMN status SET DEFAULT 'EN_INVESTIGACION';
