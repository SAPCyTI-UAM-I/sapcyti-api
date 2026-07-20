-- HU-56 / SPEC-034: trimestre de ingreso (nullable — no historical backfill)
ALTER TABLE students
    ADD COLUMN admission_term VARCHAR(4);
