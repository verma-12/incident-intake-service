package com.example.incident.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsIncidentAndTracksLifecycle() throws Exception {
        String createPayload = """
                {
                  "title": "Facility outage - Building 7",
                  "severity": "HIGH",
                  "reportedBy": "facility.ops@example.com",
                  "externalReferenceId": "FAC-2026-001",
                  "description": "Main breaker tripped"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.timeline[0].eventType").value("CREATED"))
                .andReturn();

        String incidentId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentId));

        mockMvc.perform(patch("/incidents/" + incidentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACKNOWLEDGED",
                                  "actor": "dispatcher@example.com",
                                  "note": "Assigned to on-call"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.timeline[?(@.eventType == 'STATUS_CHANGED')]").exists());

        mockMvc.perform(get("/incidents/" + incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline").isArray())
                .andExpect(jsonPath("$.timeline.length()").value(2));

        mockMvc.perform(get("/incidents").param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }

    @Test
    void duplicateExternalReferenceWithDifferentPayloadReturnsConflict() throws Exception {
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Security breach",
                                  "severity": "CRITICAL",
                                  "reportedBy": "sec@example.com",
                                  "externalReferenceId": "SEC-99"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult conflict = mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Different title",
                                  "severity": "CRITICAL",
                                  "reportedBy": "sec@example.com",
                                  "externalReferenceId": "SEC-99"
                                }
                                """))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode error = objectMapper.readTree(conflict.getResponse().getContentAsString());
        assertThat(error.get("code").asText()).isEqualTo("DUPLICATE_EXTERNAL_REFERENCE_CONFLICT");
        assertThat(error.get("path").asText()).isEqualTo("/incidents");
        assertThat(error.get("timestamp").asText()).isNotBlank();
    }

    @Test
    void validationErrorsUseConsistentStructure() throws Exception {
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "severity": "HIGH",
                                  "reportedBy": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void invalidTransitionReturnsUnprocessableEntity() throws Exception {
        MvcResult created = mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Weather event",
                                  "severity": "MEDIUM",
                                  "reportedBy": "ops@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String incidentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/incidents/" + incidentId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "actor": "ops@example.com"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void getUnknownIncidentReturnsNotFound() throws Exception {
        mockMvc.perform(get("/incidents/00000000-0000-0000-0000-000000000099"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INCIDENT_NOT_FOUND"));
    }
}
