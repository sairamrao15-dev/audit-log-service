CREATE TABLE audit_events (
    tenant_id VARCHAR(200) NOT NULL,
    stream_id VARCHAR(200) NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_json TEXT NOT NULL,
    previous_hash VARCHAR(64),
    event_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_events PRIMARY KEY (tenant_id, stream_id, sequence_number),
    CONSTRAINT uq_audit_event_id UNIQUE (event_id),
    CONSTRAINT uq_audit_idempotency UNIQUE (tenant_id, stream_id, idempotency_key)
);

CREATE INDEX ix_audit_events_tenant_stream_time
    ON audit_events (tenant_id, stream_id, occurred_at);
CREATE INDEX ix_audit_events_tenant_event_id
    ON audit_events (tenant_id, event_id);
