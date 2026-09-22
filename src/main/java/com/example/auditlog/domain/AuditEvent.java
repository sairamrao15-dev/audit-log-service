package com.example.auditlog.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        String tenantId,
        String streamId,
        long sequenceNumber,
        UUID eventId,
        String idempotencyKey,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        Instant occurredAt,
        JsonNode payload,
        String previousHash,
        String eventHash,
        String hashAlgorithm,
        Instant createdAt,
        boolean archived,
        String archiveId,
        String archiveManifestHash,
        String payloadHash) {

    public AuditEvent(String tenantId, String streamId, long sequenceNumber, UUID eventId,
                      String idempotencyKey, String eventType, Instant occurredAt, JsonNode payload,
                      String previousHash, String eventHash, Instant createdAt) {
        this(tenantId, streamId, sequenceNumber, eventId, idempotencyKey, eventType,
                null, null, null, occurredAt, payload, previousHash, eventHash,
                "SHA-256-RAW-PAYLOAD-V1", createdAt,
                false, null, null, null);
    }

    /** The public API name for the producer timestamp. */
    public Instant timestamp() {
        return occurredAt;
    }
}
