package com.example.auditlog.api;

import com.example.auditlog.service.AuditEventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/streams/{streamId}/retention")
public class RetentionController {
    private final AuditEventService service;
    private final Duration configuredRetention;

    public RetentionController(AuditEventService service,
                               @Value("${audit.retention.archive-after:365d}") Duration configuredRetention) {
        this.service = service;
        this.configuredRetention = configuredRetention;
    }

    @PostMapping("/archive")
    public Map<String, Object> archive(@PathVariable String tenantId, @PathVariable String streamId,
                                       @RequestParam(required = false) Duration olderThan) {
        Duration retention = olderThan == null ? configuredRetention : olderThan;
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("Retention must be positive");
        }
        return Map.of("archived", service.archiveExpired(tenantId, streamId, retention),
                "retention", retention.toString());
    }
}
