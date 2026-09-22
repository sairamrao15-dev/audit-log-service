package com.example.auditlog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void appendsIsIdempotentAndVerifiesChain() throws Exception {
        String body = """
                {"eventType":"user.created","payload":{"z":1,"a":"value"},
                 "occurredAt":"2026-01-01T00:00:00Z"}
                """;
        String first = mockMvc.perform(post("/v1/tenants/acme/streams/users/events")
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sequenceNumber").value(1))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/v1/tenants/acme/streams/users/events")
                        .header("Idempotency-Key", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().json(first));

        mockMvc.perform(post("/v1/tenants/acme/streams/users/events")
                        .header("Idempotency-Key", "request-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"user.updated","payload":{"id":1}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sequenceNumber").value(2));

        mockMvc.perform(get("/v1/tenants/acme/streams/users/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.eventCount").value(2));

        JsonNode firstEvent = objectMapper.readTree(first);
        mockMvc.perform(get("/v1/tenants/acme/events/" + firstEvent.get("eventId").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("user.created"));
    }

    @Test
    void rejectsIdempotencyReuseWithDifferentPayload() throws Exception {
        String endpoint = "/v1/tenants/acme/streams/orders/events";
        mockMvc.perform(post(endpoint).header("Idempotency-Key", "same")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"order.created","payload":{"id":1}}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post(endpoint).header("Idempotency-Key", "same")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"order.created","payload":{"id":2}}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void filtersByEnvelopeFieldsAndExportsAResourceBundle() throws Exception {
        String endpoint = "/v1/tenants/filter/streams/orders/events";
        mockMvc.perform(post(endpoint).header("Idempotency-Key", "filter-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"order.created","actorId":"alice","resourceType":"order",
                                 "resourceId":"o-1","payload":{"secret":"x","amount":1}}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post(endpoint).header("Idempotency-Key", "filter-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"order.created","actorId":"bob","resourceType":"order",
                                 "resourceId":"o-2","payload":{"amount":2}}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get(endpoint).param("actorId", "alice").param("resourceId", "o-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].actorId").value("alice"));
        mockMvc.perform(get("/audit/export").param("tenantId", "filter").param("resourceId", "o-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("audit-log-bundle-v1"))
                .andExpect(jsonPath("$.events[0].resourceId").value("o-1"));
    }

    @Test
    void detectsDirectDatabasePayloadMutation() throws Exception {
        String endpoint = "/v1/tenants/tamper/streams/s/events";
        String response = mockMvc.perform(post(endpoint).header("Idempotency-Key", "tamper-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"eventType":"created","payload":{"value":1}}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(response).get("eventId").asText();
        jdbcTemplate.update("UPDATE audit_events SET payload_json = ? WHERE event_id = ?",
                "{\"value\":999}", eventId);

        mockMvc.perform(get("/audit/verify").param("tenantId", "tamper").param("streamId", "s"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.firstInvalidSequence").value(1));
    }

    @Test
    void redactionUsesAStableMarkerAndPreservesChain() throws Exception {
        String endpoint = "/v1/tenants/redact/streams/s/events";
        String response = mockMvc.perform(post(endpoint).header("Idempotency-Key", "redact-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"eventType":"created","payload":{"email":"a@example.test","ok":true}}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(response).get("eventId").asText();

        mockMvc.perform(post("/v1/tenants/redact/events/" + eventId + "/redactions")
                        .header("Idempotency-Key", "redact-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"paths":["email"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payload.redactedPayload.email").value("[REDACTED]"));
        mockMvc.perform(get("/audit/verify").param("tenantId", "redact").param("streamId", "s"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void archivesPayloadWithManifestAndStillVerifies() throws Exception {
        String endpoint = "/v1/tenants/archive/streams/s/events";
        mockMvc.perform(post(endpoint).header("Idempotency-Key", "archive-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"created","timestamp":"2020-01-01T00:00:00Z",
                                 "payload":{"value":1}}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/tenants/archive/streams/s/retention/archive")
                        .param("olderThan", "PT1S"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(1));
        mockMvc.perform(get("/audit/verify").param("tenantId", "archive").param("streamId", "s"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));
        jdbcTemplate.update("UPDATE audit_event_archives SET event_hash = ? WHERE tenant_id = ?",
                "bad", "archive");
        mockMvc.perform(get("/audit/verify").param("tenantId", "archive").param("streamId", "s"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false));
    }
}
