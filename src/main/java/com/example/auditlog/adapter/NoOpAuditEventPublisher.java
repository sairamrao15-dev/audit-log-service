package com.example.auditlog.adapter;

import com.example.auditlog.domain.AuditEvent;

public class NoOpAuditEventPublisher implements AuditEventPublisher {
    @Override
    public void publish(AuditEvent event) {
        // Kafka publication is intentionally not part of the MVP transaction.
    }
}
