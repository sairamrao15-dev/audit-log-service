package com.example.auditlog.api;

import com.example.auditlog.domain.AuditEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExportBundle(
        String format,
        String tenantId,
        String actorId,
        String resourceId,
        Instant exportedAt,
        List<AuditEvent> events,
        Map<String, StreamChainMetadata> chains) {

    public record StreamChainMetadata(long firstSequence, long lastSequence,
                                      String previousHash, String terminalHash,
                                      boolean completeChain) {
    }
}
