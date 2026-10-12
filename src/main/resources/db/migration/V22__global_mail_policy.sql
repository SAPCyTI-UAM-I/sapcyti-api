-- V22__global_mail_policy.sql
-- SPEC-040: global mail policy — one row and its effective-change history (HU-35)
-- The fixed key is id = 1. Neither table has graduate_program_id.

CREATE TABLE IF NOT EXISTS mail_policy (
    id                 INTEGER PRIMARY KEY,
    mode               VARCHAR(16) NOT NULL,
    version            INTEGER     NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE,
    updated_by_user_id BIGINT,
    CONSTRAINT ck_mail_policy_single_row CHECK (id = 1),
    CONSTRAINT ck_mail_policy_mode CHECK (mode IN ('DISABLED', 'TEST', 'ENABLED')),
    CONSTRAINT ck_mail_policy_version CHECK (version >= 0),
    CONSTRAINT fk_mail_policy_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS mail_policy_audit_entries (
    id               BIGSERIAL PRIMARY KEY,
    previous_mode    VARCHAR(16) NOT NULL,
    new_mode         VARCHAR(16) NOT NULL,
    previous_version INTEGER     NOT NULL,
    new_version      INTEGER     NOT NULL,
    actor_user_id    BIGINT      NOT NULL,
    occurred_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_mail_policy_audit_previous_mode CHECK (previous_mode IN ('DISABLED', 'TEST', 'ENABLED')),
    CONSTRAINT ck_mail_policy_audit_new_mode CHECK (new_mode IN ('DISABLED', 'TEST', 'ENABLED')),
    CONSTRAINT ck_mail_policy_audit_previous_version CHECK (previous_version >= 0),
    CONSTRAINT ck_mail_policy_audit_new_version CHECK (new_version >= 0),
    CONSTRAINT ck_mail_policy_audit_version_step CHECK (new_version = previous_version + 1),
    CONSTRAINT ck_mail_policy_audit_mode_change CHECK (previous_mode <> new_mode),
    CONSTRAINT fk_mail_policy_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
);

INSERT INTO mail_policy (id, mode, version, updated_at, updated_by_user_id)
SELECT 1, 'DISABLED', 0, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM mail_policy WHERE id = 1);
