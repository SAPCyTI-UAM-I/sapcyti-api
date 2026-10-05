-- BC-02 Academic Management — extend PersonalData, AcademicInformation, ProfessorInformation (SPEC-016 / SPEC-017)

ALTER TABLE students
    ADD COLUMN birth_date DATE,
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN phone_extension VARCHAR(10),
    ADD COLUMN last_degree_obtained VARCHAR(200);

UPDATE students
SET birth_date = COALESCE(birth_date, '1900-01-01'),
    phone = COALESCE(phone, '0000000000'),
    last_degree_obtained = COALESCE(last_degree_obtained, undergraduate_degree)
WHERE birth_date IS NULL
   OR phone IS NULL
   OR last_degree_obtained IS NULL;

ALTER TABLE students
    ALTER COLUMN birth_date SET NOT NULL,
    ALTER COLUMN phone SET NOT NULL,
    ALTER COLUMN last_degree_obtained SET NOT NULL;

ALTER TABLE professors
    ADD COLUMN birth_date DATE,
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN phone_extension VARCHAR(10),
    ADD COLUMN commission_member BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN next_sabbatical_start DATE,
    ADD COLUMN next_sabbatical_end DATE;

UPDATE professors SET phone = '0000000000' WHERE phone IS NULL;

ALTER TABLE professors ALTER COLUMN phone SET NOT NULL;
