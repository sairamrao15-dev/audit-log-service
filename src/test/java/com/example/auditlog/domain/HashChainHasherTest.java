package com.example.auditlog.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HashChainHasherTest {
    private final HashChainHasher hasher = new HashChainHasher(new ObjectMapper());

    @Test
    void canonicalizesObjectKeyOrderAndProducesSha256() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String first = hasher.canonicalPayload(mapper.readTree("""
                {"z":1,"nested":{"b":true,"a":"x"},"a":[3,2,1]}
                """));
        String second = hasher.canonicalPayload(mapper.readTree("""
                {"a":[3,2,1],"nested":{"a":"x","b":true},"z":1}
                """));

        assertThat(first).isEqualTo(second);
        assertThat(hasher.hash("t", "s", 1, UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "created", "2026-01-01T00:00:00Z", first, null)).hasSize(64);
    }
}
