package com.example.auditlog.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AppendEventCommand(
        String tenantId,
        String streamId,
        String idempotencyKey,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        Instant occurredAt,
        JsonNode payload,
        UUID eventId) {

    public AppendEventCommand(String tenantId, String streamId, String idempotencyKey,
                              String eventType, Instant occurredAt, JsonNode payload, UUID eventId) {
        this(tenantId, streamId, idempotencyKey, eventType, null, null, null,
                occurredAt, payload, eventId);
    }

    public Instant timestamp() {
        return occurredAt;
    }
}
