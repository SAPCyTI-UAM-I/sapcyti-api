-- V4__create_audit_schema.sql
-- SPEC-014: Audit Event Capture (BC-05)

CREATE TABLE audit_events (
    id                  BIGSERIAL PRIMARY KEY,
    timestamp           TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id            BIGINT, -- Nullable for anonymous events
    actor_role          VARCHAR(50) NOT NULL,
    action              VARCHAR(100) NOT NULL,
    entity_type         VARCHAR(100),
    entity_id           BIGINT,
    details             VARCHAR(2000),
    severity_level      VARCHAR(20) NOT NULL,
    ip_address          VARCHAR(45),
    graduate_program_id BIGINT -- Nullable for system-level events
);

CREATE INDEX idx_audit_events_actor ON audit_events(actor_id);
CREATE INDEX idx_audit_events_program ON audit_events(graduate_program_id);
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp);
