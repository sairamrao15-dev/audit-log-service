package com.example.auditlog.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class HashChainVerifier {
    private final HashChainHasher hasher;

    public HashChainVerifier(HashChainHasher hasher) {
        this.hasher = hasher;
    }

    public VerificationResult verify(String tenantId, String streamId, List<AuditEvent> events) {
        String previousHash = null;
        long expectedSequence = 1;
        String archiveId = null;
        List<AuditEvent> archiveSegment = new ArrayList<>();
        for (AuditEvent event : events) {
            if (!tenantId.equals(event.tenantId()) || !streamId.equals(event.streamId())) {
                return invalid(tenantId, streamId, events.size(), event.sequenceNumber(),
                        "Event belongs to a different tenant or stream");
            }
            if (event.sequenceNumber() != expectedSequence) {
                return invalid(tenantId, streamId, events.size(), event.sequenceNumber(), "Sequence gap");
            }
            if (!Objects.equals(previousHash, event.previousHash())) {
                return invalid(tenantId, streamId, events.size(), event.sequenceNumber(),
                        "Previous hash link does not match");
            }
            if (event.archived()) {
                if (event.payload() != null || event.archiveId() == null || event.payloadHash() == null) {
                    return invalid(tenantId, streamId, events.size(), event.sequenceNumber(),
                            "Archived event is missing a valid archive proof");
                }
                if (archiveId == null) {
                    archiveId = event.archiveId();
                }
                if (!archiveId.equals(event.archiveId())) {
                    if (!archiveManifestMatches(archiveSegment)) {
                        return invalid(tenantId, streamId, events.size(),
                                archiveSegment.get(0).sequenceNumber(), "Archive manifest mismatch");
                    }
                    archiveSegment.clear();
                    archiveId = event.archiveId();
                }
                archiveSegment.add(event);
            } else {
                if (archiveId != null) {
                    if (!archiveManifestMatches(archiveSegment)) {
                        return invalid(tenantId, streamId, events.size(),
                                archiveSegment.get(0).sequenceNumber(), "Archive manifest mismatch");
                    }
                    archiveId = null;
                    archiveSegment.clear();
                }
                if (event.payload() == null) {
                    return invalid(tenantId, streamId, events.size(), event.sequenceNumber(),
                            "Active event has no payload");
                }
                String payloadJson = hasher.canonicalPayload(event.payload());
                String calculatedPayloadHash = hasher.payloadHash(payloadJson);
                if (!HashChainHasher.LEGACY_ALGORITHM.equals(event.hashAlgorithm())
                        && !calculatedPayloadHash.equals(event.payloadHash())) {
                    return invalid(tenantId, streamId, events.size(), event.sequenceNumber(),
                            "Payload digest mismatch");
                }
                String expectedHash = HashChainHasher.LEGACY_ALGORITHM.equals(event.hashAlgorithm())
                        ? hasher.legacyHash(event.tenantId(), event.streamId(), event.sequenceNumber(),
                        event.eventId(), event.eventType(), event.actorId(), event.resourceType(),
                        event.resourceId(), event.occurredAt().toString(), payloadJson, event.previousHash())
                        : hasher.hash(event.tenantId(), event.streamId(), event.sequenceNumber(), event.eventId(),
                        event.eventType(), event.actorId(), event.resourceType(), event.resourceId(),
                        event.occurredAt().toString(), payloadJson, event.previousHash());
                if (!expectedHash.equals(event.eventHash())) {
                    return invalid(tenantId, streamId, events.size(), event.sequenceNumber(), "Event hash mismatch");
                }
            }
            previousHash = event.eventHash();
            expectedSequence++;
        }
        if (archiveId != null && !archiveManifestMatches(archiveSegment)) {
            return invalid(tenantId, streamId, events.size(),
                    archiveSegment.get(0).sequenceNumber(), "Archive manifest mismatch");
        }
        return new VerificationResult(tenantId, streamId, true, events.size(), null, "Chain is valid");
    }

    private boolean archiveManifestMatches(List<AuditEvent> events) {
        AuditEvent first = events.get(0);
        return events.stream().allMatch(event -> Objects.equals(first.archiveManifestHash(), event.archiveManifestHash()))
                && first.archiveManifestHash().equals(hasher.archiveManifest(first.archiveId(), events));
    }

    private VerificationResult invalid(String tenantId, String streamId, int count, long sequence, String message) {
        return new VerificationResult(tenantId, streamId, false, count, sequence, message);
    }
}
