package com.example.auditlog.api;

import com.example.auditlog.domain.AppendEventCommand;
import com.example.auditlog.domain.AuditEvent;
import com.example.auditlog.domain.VerificationResult;
import com.example.auditlog.service.AuditEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}")
@Validated
public class AuditEventController {
    private final AuditEventService service;

    public AuditEventController(AuditEventService service) {
        this.service = service;
    }

    @PostMapping("/streams/{streamId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEvent append(@PathVariable String tenantId, @PathVariable String streamId,
                             @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                             @Valid @RequestBody AppendEventRequest request) {
        return service.append(new AppendEventCommand(tenantId, streamId, idempotencyKey, request.eventType(),
                request.actorId(), request.resourceType(), request.resourceId(),
                request.effectiveTimestamp(), request.payload(), request.eventId()));
    }

    @PostMapping("/streams/{streamId}/events:batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AuditEvent> appendBatch(@PathVariable String tenantId, @PathVariable String streamId,
                                        @Valid @RequestBody BatchAppendRequest request) {
        return service.appendBatch(request.events().stream()
                .map(event -> new AppendEventCommand(tenantId, streamId, event.idempotencyKey(),
                        event.eventType(), event.actorId(), event.resourceType(), event.resourceId(),
                        event.effectiveTimestamp(), event.payload(), event.eventId()))
                .toList());
    }

    @GetMapping("/streams/{streamId}/events")
    public List<AuditEvent> query(@PathVariable String tenantId, @PathVariable String streamId,
                                  @RequestParam(required = false) Instant from,
                                  @RequestParam(required = false) Instant to,
                                  @RequestParam(required = false) String actorId,
                                  @RequestParam(required = false) String resourceType,
                                  @RequestParam(required = false) String resourceId,
                                  @RequestParam(required = false) String eventType,
                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(required = false) @Min(0) Integer offset,
                                  @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int limit) {
        return service.query(tenantId, streamId, actorId, resourceType, resourceId, eventType,
                from, to, limit, offset == null ? page * limit : offset);
    }

    @GetMapping("/events/{eventId}")
    public AuditEvent get(@PathVariable String tenantId, @PathVariable UUID eventId) {
        return service.find(tenantId, eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @GetMapping("/streams/{streamId}/verify")
    public VerificationResult verify(@PathVariable String tenantId, @PathVariable String streamId) {
        return service.verify(tenantId, streamId);
    }

    @GetMapping("/audit/verify")
    public VerificationResult verifyAudit(@RequestParam String tenantId, @RequestParam String streamId) {
        return service.verify(tenantId, streamId);
    }

    @GetMapping("/audit/export")
    public ExportBundle export(@RequestParam String tenantId,
                               @RequestParam(required = false) String actorId,
                               @RequestParam(required = false) String resourceId,
                               @RequestParam(defaultValue = "10000") @Min(1) @Max(100000) int limit) {
        if ((actorId == null) == (resourceId == null)) {
            throw new IllegalArgumentException("Specify exactly one of actorId or resourceId");
        }
        return service.export(tenantId, actorId, resourceId, limit);
    }

    @PostMapping("/events/{eventId}/redactions")
    @ResponseStatus(HttpStatus.CREATED)
    public AuditEvent redact(@PathVariable String tenantId, @PathVariable UUID eventId,
                             @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
                             @Valid @RequestBody RedactEventRequest request) {
        return service.redact(tenantId, eventId, idempotencyKey, request.paths());
    }
}
