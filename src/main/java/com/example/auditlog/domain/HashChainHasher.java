package com.example.auditlog.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class HashChainHasher {
    private final CanonicalJson canonicalJson;

    public HashChainHasher(ObjectMapper objectMapper) {
        this.canonicalJson = new CanonicalJson(objectMapper);
    }

    public String canonicalPayload(JsonNode payload) {
        return canonicalJson.write(payload);
    }

    public String hash(String tenantId, String streamId, long sequenceNumber, UUID eventId,
                       String eventType, String occurredAt, String payloadJson, String previousHash) {
        return hash(tenantId, streamId, sequenceNumber, eventId, eventType,
                null, null, null, occurredAt, payloadJson, previousHash);
    }

    public String hash(String tenantId, String streamId, long sequenceNumber, UUID eventId,
                       String eventType, String actorId, String resourceType, String resourceId,
                       String occurredAt, String payloadJson, String previousHash) {
        if (actorId == null && resourceType == null && resourceId == null) {
            return digest(String.join("|",
                    part(tenantId), part(streamId), part(Long.toString(sequenceNumber)), part(eventId.toString()),
                    part(eventType), part(occurredAt), part(payloadJson), part(previousHash)));
        }
        String material = String.join("|",
                part(tenantId), part(streamId), part(Long.toString(sequenceNumber)), part(eventId.toString()),
                part(eventType), part(actorId), part(resourceType), part(resourceId),
                part(occurredAt), part(payloadJson), part(previousHash));
        return digest(material);
    }

    public String payloadHash(String payloadJson) {
        return digest(payloadJson);
    }

    public String archiveManifest(String archiveId, java.util.List<AuditEvent> events) {
        String material = archiveId + "|" + events.size() + "|"
                + events.stream().map(event -> event.sequenceNumber() + ":"
                + part(event.eventId().toString()) + ":" + part(event.idempotencyKey()) + ":"
                + part(event.eventType()) + ":" + part(event.actorId()) + ":"
                + part(event.resourceType()) + ":" + part(event.resourceId()) + ":"
                + part(event.occurredAt().toString()) + ":" + part(event.createdAt().toString()) + ":"
                + part(event.previousHash()) + ":" + event.eventHash() + ":"
                + part(event.payloadHash() != null ? event.payloadHash()
                : event.payload() == null ? null : payloadHash(canonicalPayload(event.payload()))))
                .collect(java.util.stream.Collectors.joining("|"));
        return payloadHash(material);
    }

    private String digest(String material) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JRE", e);
        }
    }

    private String part(String value) {
        String normalized = value == null ? "<null>" : value;
        return normalized.length() + ":" + normalized;
    }
}
