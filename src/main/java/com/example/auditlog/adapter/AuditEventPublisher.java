package com.example.auditlog.adapter;

import com.example.auditlog.domain.AuditEvent;

/**
 * Boundary for a future durable Kafka publication. Appending to PostgreSQL does not depend on Kafka.
 */
public interface AuditEventPublisher {
    void publish(AuditEvent event);
}
