package com.example.auditlog.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class PayloadRedactor {
    public static final String REDACTION_MARKER = "[REDACTED]";

    public JsonNode redact(JsonNode payload, java.util.List<String> paths) {
        JsonNode copy = payload.deepCopy();
        for (String path : paths) {
            String[] parts = path.replaceFirst("^\\$\\.", "").split("\\.");
            redactAt(copy, parts, 0);
        }
        return copy;
    }

    private void redactAt(JsonNode node, String[] parts, int index) {
        if (node == null || index >= parts.length) {
            return;
        }
        String part = parts[index];
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            if (index == parts.length - 1 && object.has(part)) {
                object.put(part, REDACTION_MARKER);
            } else {
                redactAt(object.get(part), parts, index + 1);
            }
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            if ("*".equals(part)) {
                array.forEach(item -> redactAt(item, parts, index + 1));
            } else {
                try {
                    redactAt(array.get(Integer.parseInt(part)), parts, index + 1);
                } catch (NumberFormatException ignored) {
                    // An unknown path is intentionally a no-op.
                }
            }
        }
    }
}
