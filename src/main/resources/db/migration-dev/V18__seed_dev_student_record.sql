-- Development seed: academic record for student@uam.mx (user created in V7)
-- Without a students row the STUDENT endpoints (e.g. HU-41 GET /enrollment-surveys/active)
-- return 404 "Student not found".

INSERT INTO students (enrollment_id, user_id, graduate_program_id, first_name, first_last_name,
    second_last_name, nationality, undergraduate_degree, program_type, admission_date,
    birth_date, phone, last_degree_obtained)
SELECT '2262000001', u.id, u.graduate_program_id, 'Alumno', 'De', 'Prueba',
    'Mexicana', 'Ingeniería en Computación', 'MAESTRIA', DATE '2025-09-01',
    DATE '2000-01-01', '5500000000', 'LICENCIATURA'
FROM users u
WHERE u.email = 'student@uam.mx'
  AND u.graduate_program_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM students s WHERE s.user_id = u.id);

INSERT INTO student_programs (student_id, graduate_program_id, enrollment_id, program_type,
    admission_date, status)
SELECT s.id, s.graduate_program_id, s.enrollment_id, 'MAESTRIA', DATE '2025-09-01', 'ACTIVO'
FROM students s
JOIN users u ON u.id = s.user_id
WHERE u.email = 'student@uam.mx'
  AND NOT EXISTS (SELECT 1 FROM student_programs sp WHERE sp.student_id = s.id);
