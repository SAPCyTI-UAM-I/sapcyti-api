-- Development seed: test user for password recovery (HU-02 / SPEC-015)
-- Login: valentecardenas30@gmail.com / password

INSERT INTO users (email, password_hash, role, active, graduate_program_id)
SELECT 'valentecardenas30@gmail.com', '$2a$10$SMG1pcfDl1qA5njpC0LHhOk1xDimOF54/nw/MChk.6NBAERCu3ok6', 'STUDENT', TRUE, id
FROM graduate_programs
WHERE name = 'Ciencias y Tecnologías de la Información'
ON CONFLICT (email) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    active = EXCLUDED.active,
    graduate_program_id = EXCLUDED.graduate_program_id;
