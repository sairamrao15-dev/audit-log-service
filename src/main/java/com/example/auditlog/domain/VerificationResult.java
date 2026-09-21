package com.example.auditlog.domain;

public record VerificationResult(
        String tenantId,
        String streamId,
        boolean valid,
        long eventCount,
        Long firstInvalidSequence,
        String message) {

    public boolean intact() {
        return valid;
    }
}
