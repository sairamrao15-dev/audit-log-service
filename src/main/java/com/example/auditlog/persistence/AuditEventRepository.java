package com.example.auditlog.persistence;

import com.example.auditlog.domain.AuditEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository {
    Optional<AuditEvent> findByIdempotencyKey(String tenantId, String streamId, String idempotencyKey);

    Optional<AuditEvent> findByEventId(String tenantId, UUID eventId);

    Optional<StreamTail> findTailForUpdate(String tenantId, String streamId);

    void insert(AuditEvent event, String payloadJson);

    List<AuditEvent> findStream(String tenantId, String streamId, Instant from, Instant to, int limit);

    default List<AuditEvent> findStream(String tenantId, String streamId, String actorId, String resourceType,
                                        String resourceId, String eventType, Instant from, Instant to,
                                        int limit, int offset) {
        return findStream(tenantId, streamId, from, to, limit);
    }

    default List<AuditEvent> findAllForVerification(String tenantId, String streamId) {
        return findStream(tenantId, streamId, null, null, Integer.MAX_VALUE);
    }

    default List<AuditEvent> findByResourceOrActor(String tenantId, String actorId, String resourceId, int limit) {
        throw new UnsupportedOperationException("Export is not implemented by this repository");
    }

    default void archive(List<AuditEvent> events, String archiveId, String archiveManifestHash) {
        throw new UnsupportedOperationException("Archiving is not implemented by this repository");
    }

    default void deleteActive(List<AuditEvent> events) {
        throw new UnsupportedOperationException("Archiving is not implemented by this repository");
    }

    record StreamTail(long sequenceNumber, String eventHash) {
    }
}
