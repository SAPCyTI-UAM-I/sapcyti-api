-- V5__seed_default_coordinator.sql
-- Seed an initial coordinator user for development and smoke tests.
-- Password is 'password' (BCrypt hash)

INSERT INTO graduate_programs (name, division) 
VALUES ('Ciencias y Tecnologías de la Información', 'CBI')
ON CONFLICT DO NOTHING;

INSERT INTO users (email, password_hash, role, active, graduate_program_id)
SELECT 'coordinator@uam.mx', '$2a$10$SMG1pcfDl1qA5njpC0LHhOk1xDimOF54/nw/MChk.6NBAERCu3ok6', 'COORDINATOR', TRUE, id
FROM graduate_programs 
WHERE name = 'Ciencias y Tecnologías de la Información'
ON CONFLICT (email) DO NOTHING;
