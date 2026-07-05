-- BC-02 Academic Management — lastDegreeObtained free text to DegreeLevel enum (SPEC-032 / HU-18)
--
-- Maps dev/test free-text values to canonical enum codes. Unmappable rows abort the migration
-- so the coordinator can review them before running in production.

UPDATE students
SET last_degree_obtained = CASE
    WHEN last_degree_obtained IN ('LICENCIATURA', 'MAESTRIA', 'DOCTORADO') THEN last_degree_obtained
    WHEN last_degree_obtained ILIKE '%doctor%' THEN 'DOCTORADO'
    WHEN last_degree_obtained ILIKE '%maestr%' THEN 'MAESTRIA'
    WHEN last_degree_obtained ILIKE '%licenciatura%' THEN 'LICENCIATURA'
    ELSE last_degree_obtained
END;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM students
        WHERE last_degree_obtained NOT IN ('LICENCIATURA', 'MAESTRIA', 'DOCTORADO')
    ) THEN
        RAISE EXCEPTION 'Unmappable last_degree_obtained values exist — review before migration';
    END IF;
END $$;

ALTER TABLE students
    ALTER COLUMN last_degree_obtained TYPE VARCHAR(20);
