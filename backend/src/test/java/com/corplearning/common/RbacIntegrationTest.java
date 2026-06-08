package com.corplearning.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void complianceOfficerCannotConfigureRules() throws Exception {
        mockMvc.perform(post("/api/v1/rules")
                        .header(RoleContext.ROLE_HEADER, "COMPLIANCE_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleId\":\"RBAC-TEST\",\"name\":\"Test\",\"definitionJson\":\"{}\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.requiredPermission").value("RULE_CONFIGURE"))
                .andExpect(jsonPath("$.role").value("COMPLIANCE_OFFICER"));
    }

    @Test
    void lndAdminCanConfigureRules() throws Exception {
        mockMvc.perform(post("/api/v1/rules")
                        .header(RoleContext.ROLE_HEADER, "LND_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleId\":\"RBAC-ADMIN\",\"name\":\"Admin Rule\",\"definitionJson\":\"{}\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void trainerCannotExportComplianceReport() throws Exception {
        String body = "{\"periodStart\":\"2026-01-01\",\"periodEnd\":\"2026-03-31\"}";
        MvcResult created = mockMvc.perform(post("/api/v1/reports/corporate")
                        .header(RoleContext.ROLE_HEADER, "COMPLIANCE_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        long runId = objectMapper.readTree(created.getResponse().getContentAsString()).get("runId").asLong();

        mockMvc.perform(get("/api/v1/reports/" + runId + "/export?format=csv")
                        .header(RoleContext.ROLE_HEADER, "TRAINER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.requiredPermission").value("REPORT_EXPORT"));

        mockMvc.perform(get("/api/v1/reports/" + runId + "/export?format=pdf")
                        .header(RoleContext.ROLE_HEADER, "TRAINER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void complianceOfficerCanExportPdf() throws Exception {
        String body = "{\"periodStart\":\"2026-01-01\",\"periodEnd\":\"2026-03-31\"}";
        MvcResult created = mockMvc.perform(post("/api/v1/reports/corporate")
                        .header(RoleContext.ROLE_HEADER, "COMPLIANCE_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode report = objectMapper.readTree(created.getResponse().getContentAsString());
        long runId = report.get("runId").asLong();

        mockMvc.perform(get("/api/v1/reports/" + runId + "/export?format=pdf")
                        .header(RoleContext.ROLE_HEADER, "COMPLIANCE_OFFICER"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Type", "application/pdf"));
    }

    @Test
    void lndAdminCannotGenerateReport() throws Exception {
        mockMvc.perform(post("/api/v1/reports/corporate")
                        .header(RoleContext.ROLE_HEADER, "LND_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodStart\":\"2026-01-01\",\"periodEnd\":\"2026-03-31\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.requiredPermission").value("REPORT_GENERATE"));
    }
}
