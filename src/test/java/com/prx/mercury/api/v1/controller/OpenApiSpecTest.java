package com.prx.mercury.api.v1.controller;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import org.yaml.snakeyaml.Yaml;
import io.swagger.v3.oas.annotations.Operation;

class OpenApiSpecTest {

    @Test
    void openApiYamlContainsOperationIds() {
        InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("api/openapi.yaml");
        assertNotNull(is, "openapi.yaml must be on classpath");
        Yaml yaml = new Yaml();
        Map<?,?> doc = yaml.load(is);
        assertNotNull(doc.get("paths"), "paths must be present in openapi.yaml");
        Map<?,?> paths = (Map<?,?>) doc.get("paths");
        // check a handful of operationIds
        boolean foundCreate = false;
        boolean foundGetById = false;
        boolean foundGetByApp = false;
        boolean foundSendEmail = false;

        for (Object pathObj : paths.values()) {
            Map<?,?> methods = (Map<?,?>) pathObj;
            for (Object methodObj : methods.values()) {
                Map<?,?> method = (Map<?,?>) methodObj;
                Object opId = method.get("operationId");
                if ("createCampaign".equals(opId)) foundCreate = true;
                if ("getCampaignById".equals(opId)) foundGetById = true;
                if ("getCampaignsByApplication".equals(opId)) foundGetByApp = true;
                if ("sendEmail".equals(opId)) foundSendEmail = true;
            }
        }
        assertTrue(foundCreate, "createCampaign operationId must exist in YAML");
        assertTrue(foundGetById, "getCampaignById operationId must exist in YAML");
        assertTrue(foundGetByApp, "getCampaignsByApplication operationId must exist in YAML");
        assertTrue(foundSendEmail, "sendEmail operationId must exist in YAML");
    }

    @Test
    void interfacesContainOperationAnnotations() throws Exception {
        // reflectively inspect CampaignApi methods
        var cls = CampaignApi.class;
        var m1 = cls.getMethod("createCampaign", com.prx.mercury.api.v1.to.CreateCampaignRequest.class);
        Operation op = m1.getAnnotation(Operation.class);
        assertNotNull(op);
        assertEquals("createCampaign", op.operationId());

        var m2 = cls.getMethod("getById", java.util.UUID.class);
        Operation op2 = m2.getAnnotation(Operation.class);
        assertNotNull(op2);
        assertEquals("getCampaignById", op2.operationId());
    }
}

