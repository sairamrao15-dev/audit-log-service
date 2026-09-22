package com.example.auditlog.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

final class CanonicalJson {
    private final ObjectMapper objectMapper;

    CanonicalJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String write(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(sort(value));
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload cannot be serialized", e);
        }
    }

    private JsonNode sort(JsonNode value) {
        if (value == null || value.isValueNode()) {
            return value;
        }
        if (value.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            value.forEach(item -> array.add(sort(item)));
            return array;
        }
        ObjectNode object = objectMapper.createObjectNode();
        ArrayList<String> names = new ArrayList<>();
        Iterator<String> fields = value.fieldNames();
        fields.forEachRemaining(names::add);
        Collections.sort(names);
        for (String name : names) {
            object.set(name, sort(value.get(name)));
        }
        return object;
    }
}
