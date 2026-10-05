-- BC-02 Academic Management (SPEC-016 / SPEC-017)
-- professors must exist before students.advisor_id FK

CREATE TABLE professors (
    id BIGSERIAL PRIMARY KEY,
    employee_number VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    graduate_program_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    first_last_name VARCHAR(100) NOT NULL,
    second_last_name VARCHAR(100),
    nationality VARCHAR(100),
    CONSTRAINT uq_professors_employee_number UNIQUE (employee_number),
    CONSTRAINT uq_professors_user_id UNIQUE (user_id),
    CONSTRAINT fk_professors_graduate_program FOREIGN KEY (graduate_program_id) REFERENCES graduate_programs(id),
    CONSTRAINT fk_professors_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_professors_graduate_program ON professors(graduate_program_id);

-- Reserved for SPEC-016 (student registration)
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    enrollment_id VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    graduate_program_id BIGINT NOT NULL,
    advisor_id BIGINT,
    first_name VARCHAR(100) NOT NULL,
    first_last_name VARCHAR(100) NOT NULL,
    second_last_name VARCHAR(100),
    nationality VARCHAR(100),
    undergraduate_degree VARCHAR(200),
    program_type VARCHAR(50),
    admission_date DATE,
    CONSTRAINT uq_students_enrollment_id UNIQUE (enrollment_id),
    CONSTRAINT uq_students_user_id UNIQUE (user_id),
    CONSTRAINT fk_students_graduate_program FOREIGN KEY (graduate_program_id) REFERENCES graduate_programs(id),
    CONSTRAINT fk_students_advisor FOREIGN KEY (advisor_id) REFERENCES professors(id),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_students_graduate_program ON students(graduate_program_id);
