package com.example.auditlog.service;

import com.example.auditlog.domain.AppendEventCommand;
import com.example.auditlog.domain.AuditEvent;
import com.example.auditlog.domain.HashChainHasher;
import com.example.auditlog.domain.HashChainVerifier;
import com.example.auditlog.domain.VerificationResult;
import com.example.auditlog.domain.PayloadRedactor;
import com.example.auditlog.api.ExportBundle;
import com.example.auditlog.persistence.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class AuditEventService {
    private final AuditEventRepository repository;
    private final HashChainHasher hasher;
    private final HashChainVerifier verifier;
    private final PayloadRedactor redactor;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditEventService(AuditEventRepository repository, HashChainHasher hasher,
                             HashChainVerifier verifier, PayloadRedactor redactor, ObjectMapper objectMapper) {
        this.repository = repository;
        this.hasher = hasher;
        this.verifier = verifier;
        this.redactor = redactor;
        this.objectMapper = objectMapper;
    }

    public AuditEventService(AuditEventRepository repository, HashChainHasher hasher,
                             HashChainVerifier verifier) {
        this(repository, hasher, verifier, new PayloadRedactor(), new ObjectMapper());
    }

    @Transactional
    public AuditEvent append(AppendEventCommand command) {
        return appendInTransaction(command);
    }

    @Transactional
    public List<AuditEvent> appendBatch(List<AppendEventCommand> commands) {
        return commands.stream().map(this::appendInTransaction).toList();
    }

    public List<AuditEvent> query(String tenantId, String streamId, Instant from, Instant to, int limit) {
        return repository.findStream(tenantId, streamId, from, to, limit);
    }

    public List<AuditEvent> query(String tenantId, String streamId, String actorId, String resourceType,
                                  String resourceId, String eventType, Instant from, Instant to,
                                  int limit, int offset) {
        return repository.findStream(tenantId, streamId, actorId, resourceType, resourceId, eventType,
                from, to, limit, Math.max(0, offset));
    }

    public Optional<AuditEvent> find(String tenantId, UUID eventId) {
        return repository.findByEventId(tenantId, eventId);
    }

    public VerificationResult verify(String tenantId, String streamId) {
        return verifier.verify(tenantId, streamId, repository.findAllForVerification(tenantId, streamId));
    }

    public ExportBundle export(String tenantId, String actorId, String resourceId, int limit) {
        List<AuditEvent> events = repository.findByResourceOrActor(tenantId, actorId, resourceId, limit);
        Map<String, ExportBundle.StreamChainMetadata> chains = new LinkedHashMap<>();
        events.stream().collect(Collectors.groupingBy(AuditEvent::streamId, LinkedHashMap::new, Collectors.toList()))
                .forEach((stream, streamEvents) -> {
                    AuditEvent first = streamEvents.get(0);
                    AuditEvent last = streamEvents.get(streamEvents.size() - 1);
                    boolean complete = first.sequenceNumber() == 1;
                    for (int i = 1; i < streamEvents.size(); i++) {
                        complete &= streamEvents.get(i).sequenceNumber()
                                == streamEvents.get(i - 1).sequenceNumber() + 1;
                    }
                    chains.put(stream, new ExportBundle.StreamChainMetadata(first.sequenceNumber(),
                            last.sequenceNumber(), first.previousHash(), last.eventHash(), complete));
                });
        return new ExportBundle("audit-log-bundle-v1", tenantId, actorId, resourceId,
                Instant.now(), events, chains);
    }

    @Transactional
    public AuditEvent redact(String tenantId, UUID eventId, String idempotencyKey,
                             List<String> paths) {
        AuditEvent original = find(tenantId, eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        if (original.payload() == null) {
            throw new IllegalArgumentException("Archived event payload is no longer available for redaction");
        }
        com.fasterxml.jackson.databind.node.ObjectNode payload = objectMapper.createObjectNode();
        payload.put("targetEventId", eventId.toString());
        payload.put("originalEventHash", original.eventHash());
        payload.set("paths", objectMapper.valueToTree(paths));
        payload.set("redactedPayload", redactor.redact(original.payload(), paths));
        return append(new AppendEventCommand(tenantId, original.streamId(), idempotencyKey,
                "audit.redaction", original.actorId(), original.resourceType(), original.resourceId(),
                Instant.now(), payload, null));
    }

    @Transactional
    public int archiveExpired(String tenantId, String streamId, Duration retention) {
        Instant cutoff = Instant.now().minus(retention);
        List<AuditEvent> candidates = repository.findStream(tenantId, streamId, null, null, null, null,
                null, cutoff, Integer.MAX_VALUE, 0).stream().filter(event -> !event.archived()).toList();
        if (!candidates.isEmpty()) {
            List<AuditEvent> prefix = new java.util.ArrayList<>();
            long expected = candidates.get(0).sequenceNumber();
            for (AuditEvent event : candidates) {
                if (event.sequenceNumber() != expected) break;
                prefix.add(event);
                expected++;
            }
            candidates = prefix;
        }
        if (candidates.isEmpty()) {
            return 0;
        }
        String archiveId = UUID.randomUUID().toString();
        String manifest = hasher.archiveManifest(archiveId, candidates);
        repository.archive(candidates, archiveId, manifest);
        repository.deleteActive(candidates);
        return candidates.size();
    }

    private AuditEvent appendInTransaction(AppendEventCommand command) {
        String payloadJson = hasher.canonicalPayload(command.payload());
        Optional<AuditEvent> existing = repository.findByIdempotencyKey(
                command.tenantId(), command.streamId(), command.idempotencyKey());
        if (existing.isPresent()) {
            AuditEvent event = existing.get();
            if (!event.eventType().equals(command.eventType())
                    || !java.util.Objects.equals(event.actorId(), command.actorId())
                    || !java.util.Objects.equals(event.resourceType(), command.resourceType())
                    || !java.util.Objects.equals(event.resourceId(), command.resourceId())
                    || (event.archived()
                    ? !hasher.payloadHash(payloadJson).equals(event.payloadHash())
                    : !hasher.canonicalPayload(event.payload()).equals(payloadJson))) {
                throw new IdempotencyConflictException(command.idempotencyKey());
            }
            return event;
        }

        Optional<AuditEvent> priorByEventId = command.eventId() == null
                ? Optional.empty() : repository.findByEventId(command.tenantId(), command.eventId());
        if (priorByEventId.isPresent()) {
            return priorByEventId.get();
        }

        AuditEventRepository.StreamTail tail = repository.findTailForUpdate(
                command.tenantId(), command.streamId()).orElse(null);
        long sequence = tail == null ? 1 : tail.sequenceNumber() + 1;
        String previousHash = tail == null ? null : tail.eventHash();
        UUID eventId = command.eventId() == null ? UUID.randomUUID() : command.eventId();
        // PostgreSQL timestamptz stores microsecond precision; hash the persisted value.
        Instant occurredAt = (command.occurredAt() == null ? Instant.now() : command.occurredAt())
                .truncatedTo(ChronoUnit.MICROS);
        String hash = hasher.hash(command.tenantId(), command.streamId(), sequence, eventId,
                command.eventType(), command.actorId(), command.resourceType(), command.resourceId(),
                occurredAt.toString(), payloadJson, previousHash);
        AuditEvent event = new AuditEvent(command.tenantId(), command.streamId(), sequence, eventId,
                command.idempotencyKey(), command.eventType(), command.actorId(), command.resourceType(),
                command.resourceId(), occurredAt, command.payload(), previousHash, hash,
                Instant.now().truncatedTo(ChronoUnit.MICROS), false, null, null, null);
        try {
            repository.insert(event, payloadJson);
            return event;
        } catch (DuplicateKeyException e) {
            // A concurrent request may have won the idempotency race. Make retries deterministic.
            return repository.findByIdempotencyKey(command.tenantId(), command.streamId(), command.idempotencyKey())
                    .orElseThrow(() -> e);
        }
    }
}
