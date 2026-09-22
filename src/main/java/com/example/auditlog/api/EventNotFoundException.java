package com.example.auditlog.api;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(UUID eventId) {
        super("Event was not found: " + eventId);
    }
}
