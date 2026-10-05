-- BC-02 Academic Management — HU-44 research catalog + line_of_knowledge (SPEC-019B)

CREATE TABLE research_lines (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE research_areas (
    id BIGSERIAL PRIMARY KEY,
    line_id BIGINT NOT NULL REFERENCES research_lines(id),
    name VARCHAR(200) NOT NULL,
    UNIQUE (line_id, name)
);

ALTER TABLE student_programs
    ADD COLUMN line_of_knowledge VARCHAR(200);

-- HU-44 seed: two lines, eleven areas
INSERT INTO research_lines (name) VALUES
    ('Ciencias e Ingeniería de la Computación'),
    ('Redes de Comunicaciones');

INSERT INTO research_areas (line_id, name)
SELECT rl.id, area.name
FROM research_lines rl
CROSS JOIN (VALUES
    ('Supercómputo (cómputo de alto rendimiento)'),
    ('Manejo de datos masivos (Big data)'),
    ('Web semántica'),
    ('Internet de las cosas'),
    ('Inteligencia artificial')
) AS area(name)
WHERE rl.name = 'Ciencias e Ingeniería de la Computación';

INSERT INTO research_areas (line_id, name)
SELECT rl.id, area.name
FROM research_lines rl
CROSS JOIN (VALUES
    ('Comunicaciones inalámbricas'),
    ('Aplicaciones de redes'),
    ('Redes definidas por software'),
    ('Codificación de red'),
    ('Encaminamiento (ruteo)'),
    ('Procesamiento digital de señales en las comunicaciones')
) AS area(name)
WHERE rl.name = 'Redes de Comunicaciones';
