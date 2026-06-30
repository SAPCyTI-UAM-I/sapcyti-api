-- HU-44 research catalog seed for integration-test profile (mirrors Flyway V12)

INSERT INTO research_lines (name) VALUES ('Ciencias e Ingeniería de la Computación');
INSERT INTO research_lines (name) VALUES ('Redes de Comunicaciones');

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
