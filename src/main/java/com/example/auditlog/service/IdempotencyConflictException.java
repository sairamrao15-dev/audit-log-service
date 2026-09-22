package com.example.auditlog.service;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key has already been used with a different event: " + idempotencyKey);
    }
}
