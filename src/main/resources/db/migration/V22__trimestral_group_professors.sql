-- HU-57: research groups have co-directors → a group may carry several professors.
-- Mirrors the group_students child table; employee_number/professor_name are snapshots.
CREATE TABLE trimestral_plan_group_professors (
    id                  BIGSERIAL PRIMARY KEY,
    group_id            BIGINT       NOT NULL REFERENCES trimestral_plan_groups(id) ON DELETE CASCADE,
    professor_id        BIGINT       NOT NULL,
    employee_number     VARCHAR(20),
    professor_name      VARCHAR(300),
    posicion            SMALLINT     NOT NULL
);

CREATE INDEX idx_trimestral_plan_group_professors_group
    ON trimestral_plan_group_professors (group_id, posicion);

-- Preserve existing single-professor assignments so servers with data keep them.
INSERT INTO trimestral_plan_group_professors
    (group_id, professor_id, employee_number, professor_name, posicion)
SELECT id, professor_id, employee_number, professor_name, 1
FROM trimestral_plan_groups
WHERE professor_id IS NOT NULL;

ALTER TABLE trimestral_plan_groups
    DROP COLUMN professor_id,
    DROP COLUMN employee_number,
    DROP COLUMN professor_name;
