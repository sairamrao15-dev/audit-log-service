ALTER TABLE audit_events ADD COLUMN actor_id VARCHAR(200);
ALTER TABLE audit_events ADD COLUMN resource_type VARCHAR(200);
ALTER TABLE audit_events ADD COLUMN resource_id VARCHAR(200);

CREATE INDEX ix_audit_events_actor ON audit_events (tenant_id, actor_id, occurred_at);
CREATE INDEX ix_audit_events_resource ON audit_events (tenant_id, resource_type, resource_id, occurred_at);

CREATE TABLE audit_event_archives (
    tenant_id VARCHAR(200) NOT NULL,
    stream_id VARCHAR(200) NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    actor_id VARCHAR(200),
    resource_type VARCHAR(200),
    resource_id VARCHAR(200),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_json TEXT,
    previous_hash VARCHAR(64),
    event_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archive_id VARCHAR(36) NOT NULL,
    archive_manifest_hash VARCHAR(64) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    CONSTRAINT pk_audit_event_archives PRIMARY KEY (tenant_id, stream_id, sequence_number),
    CONSTRAINT uq_audit_archive_event_id UNIQUE (event_id),
    CONSTRAINT uq_audit_archive_idempotency UNIQUE (tenant_id, stream_id, idempotency_key)
);

CREATE INDEX ix_audit_archives_lookup
    ON audit_event_archives (tenant_id, stream_id, occurred_at);
CREATE INDEX ix_audit_archives_actor
    ON audit_event_archives (tenant_id, actor_id, occurred_at);
CREATE INDEX ix_audit_archives_resource
    ON audit_event_archives (tenant_id, resource_type, resource_id, occurred_at);
