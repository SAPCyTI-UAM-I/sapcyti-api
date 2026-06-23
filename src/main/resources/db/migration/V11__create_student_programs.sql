-- BC-02 Academic Management — StudentProgram aggregate (SPEC-019)

CREATE TABLE student_programs (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    graduate_program_id BIGINT NOT NULL,
    enrollment_id VARCHAR(20) NOT NULL,
    program_type VARCHAR(20) NOT NULL,
    admission_date DATE NOT NULL,
    graduation_date DATE,
    research_area VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    withdrawal_reason VARCHAR(500),
    tutor_id BIGINT,
    CONSTRAINT uq_student_programs_student_type UNIQUE (student_id, program_type),
    CONSTRAINT fk_sp_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_sp_graduate_program FOREIGN KEY (graduate_program_id) REFERENCES graduate_programs(id),
    CONSTRAINT fk_sp_tutor FOREIGN KEY (tutor_id) REFERENCES professors(id)
);

CREATE TABLE student_program_advisors (
    student_program_id BIGINT NOT NULL,
    professor_id BIGINT NOT NULL,
    position INT NOT NULL,
    PRIMARY KEY (student_program_id, professor_id),
    CONSTRAINT fk_spa_program FOREIGN KEY (student_program_id) REFERENCES student_programs(id) ON DELETE CASCADE,
    CONSTRAINT fk_spa_professor FOREIGN KEY (professor_id) REFERENCES professors(id)
);

CREATE INDEX idx_student_programs_student ON student_programs(student_id);
CREATE INDEX idx_student_programs_graduate_program ON student_programs(graduate_program_id);

INSERT INTO student_programs (student_id, graduate_program_id, enrollment_id, program_type,
    admission_date, status, tutor_id)
SELECT s.id, s.graduate_program_id, s.enrollment_id, COALESCE(s.program_type, 'MAESTRIA'),
    COALESCE(s.admission_date, CURRENT_DATE), 'ACTIVO', s.advisor_id
FROM students s
WHERE NOT EXISTS (
    SELECT 1 FROM student_programs sp WHERE sp.student_id = s.id
);
