ALTER TABLE audit_events ADD COLUMN payload_hash VARCHAR(64);
ALTER TABLE audit_events ADD COLUMN hash_algorithm VARCHAR(64) NOT NULL DEFAULT 'SHA-256-RAW-PAYLOAD-V1';

ALTER TABLE audit_event_archives ADD COLUMN hash_algorithm VARCHAR(64) NOT NULL DEFAULT 'SHA-256-RAW-PAYLOAD-V1';

CREATE INDEX ix_audit_events_hash_algorithm
    ON audit_events (tenant_id, stream_id, hash_algorithm);
