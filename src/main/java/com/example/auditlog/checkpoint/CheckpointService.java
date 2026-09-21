package com.example.auditlog.checkpoint;

import com.example.auditlog.domain.VerificationResult;

/**
 * Explicit seam for a future external checkpoint/attestation service.
 * MVP verification is local and does not make an external trust claim.
 */
public interface CheckpointService {
    VerificationResult verifyAgainstCheckpoint(String tenantId, String streamId);
}
