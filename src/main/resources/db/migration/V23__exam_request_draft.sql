-- V23__exam_request_draft.sql
-- SPEC-051: exam draft, current document reference and superseded PDF mark (HU-65)
-- BIGSERIAL names the sequences exam_requests_id_seq and exam_request_documents_id_seq.
-- The adapter reserves nextval before the object put and inserts that id. This migration does not create a bucket.

CREATE TABLE IF NOT EXISTS exam_requests (
    id                   BIGSERIAL PRIMARY KEY,
    student_id           BIGINT       NOT NULL,
    exam_type            VARCHAR(16)  NOT NULL,
    graduate_program_id  BIGINT       NOT NULL,
    degree               VARCHAR(20)  NOT NULL,
    status               VARCHAR(16)  NOT NULL,
    folio                VARCHAR(17),
    version              INTEGER      NOT NULL,
    submitted_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_exam_requests_student_type UNIQUE (student_id, exam_type),
    CONSTRAINT uq_exam_requests_folio UNIQUE (folio),
    CONSTRAINT fk_exam_requests_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_exam_requests_program FOREIGN KEY (graduate_program_id) REFERENCES graduate_programs (id),
    CONSTRAINT ck_exam_requests_exam_type CHECK (exam_type IN ('GRADO', 'PREDOCTORAL')),
    CONSTRAINT ck_exam_requests_degree CHECK (degree IN ('ESPECIALIZACION', 'MAESTRIA', 'DOCTORADO')),
    CONSTRAINT ck_exam_requests_status CHECK (status IN ('BORRADOR', 'ENVIADA', 'ACTUALIZADA', 'CERRADA')),
    CONSTRAINT ck_exam_requests_version CHECK (version >= 1),
    CONSTRAINT ck_exam_requests_draft CHECK (
        (status = 'BORRADOR' AND folio IS NULL AND submitted_at IS NULL)
        OR (status <> 'BORRADOR' AND folio IS NOT NULL AND submitted_at IS NOT NULL)
    ),
    CONSTRAINT ck_exam_requests_folio CHECK (
        folio IS NULL OR folio ~ '^SEG-[0-9]{6}-[0-9]{6}$'
    )
);

CREATE TABLE IF NOT EXISTS exam_request_documents (
    id               BIGSERIAL PRIMARY KEY,
    exam_request_id  BIGINT       NOT NULL,
    document_type    VARCHAR(32)  NOT NULL,
    kind             VARCHAR(8)   NOT NULL,
    object_key       VARCHAR(512),
    sha256           VARCHAR(64),
    content_type     VARCHAR(128),
    original_name    VARCHAR(255),
    external_url     VARCHAR(2048),
    size_bytes       BIGINT,
    uploaded_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_exam_request_documents_type UNIQUE (exam_request_id, document_type),
    CONSTRAINT fk_exam_request_documents_request FOREIGN KEY (exam_request_id) REFERENCES exam_requests (id),
    CONSTRAINT ck_exam_request_documents_kind CHECK (kind IN ('PDF', 'URL')),
    CONSTRAINT ck_exam_request_documents_type CHECK (document_type IN (
        'TESIS', 'ARTICULO', 'SOL_SINODALES', 'HIST_ACAD', 'COMP_IDIOMA', 'DATOS_ALUM')),
    CONSTRAINT ck_exam_request_documents_pdf CHECK (
        kind <> 'PDF' OR (
            object_key IS NOT NULL
            AND sha256 ~ '^[0-9a-f]{64}$'
            AND content_type = 'application/pdf'
            AND original_name IS NOT NULL
            AND external_url IS NULL
            AND size_bytes >= 1
        )
    ),
    CONSTRAINT ck_exam_request_documents_url CHECK (
        kind <> 'URL' OR (
            external_url IS NOT NULL
            AND object_key IS NULL
            AND sha256 IS NULL
            AND content_type IS NULL
            AND original_name IS NULL
            AND size_bytes IS NULL
        )
    )
);

CREATE TABLE IF NOT EXISTS exam_request_superseded_objects (
    id             BIGSERIAL PRIMARY KEY,
    object_key     VARCHAR(512) NOT NULL,
    sha256         VARCHAR(64)  NOT NULL,
    superseded_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_exam_request_superseded_sha256 CHECK (sha256 ~ '^[0-9a-f]{64}$')
);
