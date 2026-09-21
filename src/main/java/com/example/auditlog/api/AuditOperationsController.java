package com.example.auditlog.api;

import com.example.auditlog.domain.VerificationResult;
import com.example.auditlog.service.AuditEventService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditOperationsController {
    private final AuditEventService service;

    public AuditOperationsController(AuditEventService service) {
        this.service = service;
    }

    @GetMapping("/audit/verify")
    public VerificationResult verify(@RequestParam String tenantId, @RequestParam String streamId) {
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
}
