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

    @Test
    void payloadDigestHashUsesCanonicalPayloadRepresentation() throws Exception {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        ObjectMapper mapper = new ObjectMapper();
        String canonical = hasher.canonicalPayload(mapper.readTree("{\"b\":2,\"a\":1}"));
        String first = hasher.hash("t", "s", 1, eventId, "created",
                "actor", "account", "a-1", "2026-01-01T00:00:00Z",
                canonical, null);
        String second = hasher.hash("t", "s", 1, eventId, "created",
                "actor", "account", "a-1", "2026-01-01T00:00:00Z",
                hasher.canonicalPayload(mapper.readTree("{\"a\":1,\"b\":2}")), null);

        assertThat(first).isEqualTo(second).hasSize(64);
        assertThat(hasher.payloadHash("{\"a\":1,\"b\":2}\")).hasSize(64);
    }
}
