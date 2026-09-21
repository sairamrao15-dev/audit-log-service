package com.example.auditlog.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AppendEventRequest(
        @NotBlank String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        @NotNull JsonNode payload,
        Instant timestamp,
        Instant occurredAt,
        UUID eventId) {

    public AppendEventRequest(String eventType, JsonNode payload, Instant occurredAt, UUID eventId) {
        this(eventType, null, null, null, payload, null, occurredAt, eventId);
    }

    public Instant effectiveTimestamp() {
        return timestamp != null ? timestamp : occurredAt;
    }
}
