-- Development seed: one active user per RoleType (password: password)
-- coordinator@uam.mx already exists from V5/V6
-- SYSTEM_ADMIN has no graduate_program_id (global scope)

INSERT INTO users (email, password_hash, role, active, graduate_program_id)
SELECT 'system_admin@uam.mx', '$2a$10$SMG1pcfDl1qA5njpC0LHhOk1xDimOF54/nw/MChk.6NBAERCu3ok6', 'SYSTEM_ADMIN', TRUE, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'system_admin@uam.mx');

INSERT INTO users (email, password_hash, role, active, graduate_program_id)
SELECT 'assistant@uam.mx', '$2a$10$SMG1pcfDl1qA5njpC0LHhOk1xDimOF54/nw/MChk.6NBAERCu3ok6', 'ASSISTANT', TRUE, id
FROM graduate_programs
WHERE name = 'Ciencias y Tecnologías de la Información'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, role, active, graduate_program_id)
SELECT 'professor@uam.mx', '$2a$10$SMG1pcfDl1qA5njpC0LHhOk1xDimOF54/nw/MChk.6NBAERCu3ok6', 'PROFESSOR', TRUE, id
FROM graduate_programs
WHERE name = 'Ciencias y Tecnologías de la Información'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, role, active, graduate_program_id)
SELECT 'student@uam.mx', '$2a$10$SMG1pcfDl1qA5njpC0LHhOk1xDimOF54/nw/MChk.6NBAERCu3ok6', 'STUDENT', TRUE, id
FROM graduate_programs
WHERE name = 'Ciencias y Tecnologías de la Información'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, role, active, graduate_program_id)
SELECT 'speaker@uam.mx', '$2a$10$SMG1pcfDl1qA5njpC0LHhOk1xDimOF54/nw/MChk.6NBAERCu3ok6', 'SPEAKER', TRUE, id
FROM graduate_programs
WHERE name = 'Ciencias y Tecnologías de la Información'
ON CONFLICT (email) DO NOTHING;
