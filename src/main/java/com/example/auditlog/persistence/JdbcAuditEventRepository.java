package com.example.auditlog.persistence;

import com.example.auditlog.domain.AuditEvent;
import com.example.auditlog.domain.HashChainHasher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAuditEventRepository implements AuditEventRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HashChainHasher hasher;

    public JdbcAuditEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, HashChainHasher hasher) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.hasher = hasher;
    }

    @Override
    public Optional<AuditEvent> findByIdempotencyKey(String tenantId, String streamId, String idempotencyKey) {
        Optional<AuditEvent> active = findOne("""
                SELECT tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                       actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                       event_hash, created_at, false AS archived, NULL AS archive_id,
                       NULL AS archive_manifest_hash, NULL AS payload_hash
                FROM audit_events
                WHERE tenant_id = ? AND stream_id = ? AND idempotency_key = ?
                """, tenantId, streamId, idempotencyKey);
        return active.or(() -> findOne(archiveSelect() + """
                WHERE tenant_id = ? AND stream_id = ? AND idempotency_key = ?
                """, tenantId, streamId, idempotencyKey));
    }

    @Override
    public Optional<AuditEvent> findByEventId(String tenantId, UUID eventId) {
        Optional<AuditEvent> active = findOne("""
                SELECT tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                       actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                       event_hash, created_at, false AS archived, NULL AS archive_id,
                       NULL AS archive_manifest_hash, NULL AS payload_hash
                FROM audit_events
                WHERE tenant_id = ? AND event_id = ?
                """, tenantId, eventId.toString());
        return active.or(() -> findOne(archiveSelect() + """
                WHERE tenant_id = ? AND event_id = ?
                """, tenantId, eventId.toString()));
    }

    @Override
    public Optional<StreamTail> findTailForUpdate(String tenantId, String streamId) {
        try {
            StreamTail active = jdbcTemplate.query("""
                    SELECT sequence_number, event_hash FROM audit_events
                    WHERE tenant_id = ? AND stream_id = ?
                    ORDER BY sequence_number DESC LIMIT 1 FOR UPDATE
                    """, (rs, rowNum) -> new StreamTail(rs.getLong("sequence_number"), rs.getString("event_hash")),
                    tenantId, streamId).stream().findFirst().orElse(null);
            StreamTail archived = jdbcTemplate.query("""
                    SELECT sequence_number, event_hash FROM audit_event_archives
                    WHERE tenant_id = ? AND stream_id = ?
                    ORDER BY sequence_number DESC LIMIT 1
                    """, (rs, rowNum) -> new StreamTail(rs.getLong("sequence_number"), rs.getString("event_hash")),
                    tenantId, streamId).stream().findFirst().orElse(null);
            if (active == null) return Optional.ofNullable(archived);
            return archived == null || active.sequenceNumber() >= archived.sequenceNumber()
                    ? Optional.of(active) : Optional.of(archived);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void insert(AuditEvent event, String payloadJson) {
        jdbcTemplate.update("""
                INSERT INTO audit_events
                    (tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                     actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                     event_hash, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, event.tenantId(), event.streamId(), event.sequenceNumber(), event.eventId().toString(),
                event.idempotencyKey(), event.eventType(), event.actorId(), event.resourceType(),
                event.resourceId(), event.occurredAt(), payloadJson, event.previousHash(),
                event.eventHash(), event.createdAt());
    }

    @Override
    public List<AuditEvent> findStream(String tenantId, String streamId, Instant from, Instant to, int limit) {
        return findStream(tenantId, streamId, null, null, null, null, from, to, limit, 0);
    }

    @Override
    public List<AuditEvent> findStream(String tenantId, String streamId, String actorId, String resourceType,
                                       String resourceId, String eventType, Instant from, Instant to,
                                       int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT * FROM (
                    SELECT tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                           actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                           event_hash, created_at, false AS archived, NULL AS archive_id,
                           NULL AS archive_manifest_hash, NULL AS payload_hash
                    FROM audit_events
                    UNION ALL
                    SELECT tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                           actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                           event_hash, created_at, true AS archived, archive_id,
                           archive_manifest_hash, payload_hash
                    FROM audit_event_archives
                ) events
                WHERE tenant_id = ? AND stream_id = ?
                """);
        List<Object> parameters = new ArrayList<>(List.of(tenantId, streamId));
        if (actorId != null) {
            sql.append(" AND actor_id = ?");
            parameters.add(actorId);
        }
        if (resourceType != null) {
            sql.append(" AND resource_type = ?");
            parameters.add(resourceType);
        }
        if (resourceId != null) {
            sql.append(" AND resource_id = ?");
            parameters.add(resourceId);
        }
        if (eventType != null) {
            sql.append(" AND event_type = ?");
            parameters.add(eventType);
        }
        if (from != null) {
            sql.append(" AND occurred_at >= ?");
            parameters.add(from);
        }
        if (to != null) {
            sql.append(" AND occurred_at <= ?");
            parameters.add(to);
        }
        sql.append(" ORDER BY sequence_number ASC LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);
        return jdbcTemplate.query(sql, new EventRowMapper(objectMapper), parameters.toArray());
    }

    @Override
    public List<AuditEvent> findAllForVerification(String tenantId, String streamId) {
        return findStream(tenantId, streamId, null, null, null, null, null, null, Integer.MAX_VALUE, 0);
    }

    @Override
    public List<AuditEvent> findByResourceOrActor(String tenantId, String actorId, String resourceId, int limit) {
        String predicate = actorId != null ? "actor_id = ?" : "resource_id = ?";
        String value = actorId != null ? actorId : resourceId;
        String sql = """
                SELECT * FROM (
                    SELECT tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                           actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                           event_hash, created_at, false AS archived, NULL AS archive_id,
                           NULL AS archive_manifest_hash, NULL AS payload_hash
                    FROM audit_events
                    UNION ALL
                    SELECT tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                           actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                           event_hash, created_at, true AS archived, archive_id,
                           archive_manifest_hash, payload_hash
                    FROM audit_event_archives
                ) events WHERE tenant_id = ? AND """ + predicate
                + " ORDER BY occurred_at ASC, stream_id ASC, sequence_number ASC LIMIT ?";
        return jdbcTemplate.query(sql, new EventRowMapper(objectMapper), tenantId, value, limit);
    }

    @Override
    public void archive(List<AuditEvent> events, String archiveId, String archiveManifestHash) {
        for (AuditEvent event : events) {
            String payloadJson = event.payload() == null ? null : hasher.canonicalPayload(event.payload());
            jdbcTemplate.update("""
                    INSERT INTO audit_event_archives
                    (tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                     actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                     event_hash, created_at, archive_id, archive_manifest_hash, payload_hash)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, event.tenantId(), event.streamId(), event.sequenceNumber(), event.eventId().toString(),
                    event.idempotencyKey(), event.eventType(), event.actorId(), event.resourceType(),
                    event.resourceId(), event.occurredAt(), null, event.previousHash(),
                    event.eventHash(), event.createdAt(), archiveId, archiveManifestHash,
                    payloadJson == null ? null : hasher.payloadHash(payloadJson));
        }
    }

    @Override
    public void deleteActive(List<AuditEvent> events) {
        for (AuditEvent event : events) {
            jdbcTemplate.update("DELETE FROM audit_events WHERE tenant_id = ? AND stream_id = ? AND sequence_number = ?",
                    event.tenantId(), event.streamId(), event.sequenceNumber());
        }
    }

    private String archiveSelect() {
        return """
                SELECT tenant_id, stream_id, sequence_number, event_id, idempotency_key, event_type,
                       actor_id, resource_type, resource_id, occurred_at, payload_json, previous_hash,
                       event_hash, created_at, true AS archived, archive_id,
                       archive_manifest_hash, payload_hash
                FROM audit_event_archives
                """;
    }

    private Optional<AuditEvent> findOne(String sql, Object... parameters) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, new EventRowMapper(objectMapper), parameters));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private static final class EventRowMapper implements RowMapper<AuditEvent> {
        private final ObjectMapper objectMapper;

        private EventRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public AuditEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                String payloadJson = rs.getString("payload_json");
                JsonNode payload = payloadJson == null ? null : objectMapper.readTree(payloadJson);
                return new AuditEvent(
                        rs.getString("tenant_id"),
                        rs.getString("stream_id"),
                        rs.getLong("sequence_number"),
                        UUID.fromString(rs.getString("event_id")),
                        rs.getString("idempotency_key"),
                        rs.getString("event_type"),
                        rs.getString("actor_id"),
                        rs.getString("resource_type"),
                        rs.getString("resource_id"),
                        rs.getTimestamp("occurred_at").toInstant(),
                        payload,
                        rs.getString("previous_hash"),
                        rs.getString("event_hash"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getBoolean("archived"),
                        rs.getString("archive_id"),
                        rs.getString("archive_manifest_hash"),
                        rs.getString("payload_hash"));
            } catch (JsonProcessingException e) {
                throw new SQLException("Stored payload is not valid JSON", e);
            }
        }
    }
}
