package com.prx.mercury.api.v1.controller;

import com.umdc.mercury.api.v1.controller.CampaignApi;
import com.umdc.mercury.api.v1.to.CreateCampaignRequest;
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
        boolean foundAccessToken = false;
        boolean foundGenerateTokenSession = false;

        for (Object pathObj : paths.values()) {
            Map<?,?> methods = (Map<?,?>) pathObj;
            for (Object methodObj : methods.values()) {
                Map<?,?> method = (Map<?,?>) methodObj;
                Object opId = method.get("operationId");
                if ("createCampaign".equals(opId)) foundCreate = true;
                if ("getCampaignById".equals(opId)) foundGetById = true;
                if ("getCampaignsByApplication".equals(opId)) foundGetByApp = true;
                if ("sendEmail".equals(opId)) foundSendEmail = true;
                if ("accessToken".equals(opId)) foundAccessToken = true;
                if ("generateTokenSession".equals(opId)) foundGenerateTokenSession = true;
            }
        }
        assertTrue(foundCreate, "createCampaign operationId must exist in YAML");
        assertTrue(foundGetById, "getCampaignById operationId must exist in YAML");
        assertTrue(foundGetByApp, "getCampaignsByApplication operationId must exist in YAML");
        assertTrue(foundSendEmail, "sendEmail operationId must exist in YAML");
        // accessToken (POST /api/v1/auth/token) and generateTokenSession
        // (POST /api/v1/auth/session-token) are implemented by security-oauth's AuthAPi,
        // not by a controller in this repo — but they're real endpoints this app exposes
        // at runtime, and generateTokenSession is the ONLY operation that mints a
        // session-token carrying a verified uid claim (see
        // docs/architecture/session-token-authorization.md). Keep them documented here.
        assertTrue(foundAccessToken, "accessToken operationId must exist in YAML");
        assertTrue(foundGenerateTokenSession, "generateTokenSession operationId must exist in YAML");
    }

    @Test
    void interfacesContainOperationAnnotations() throws Exception {
        // reflectively inspect CampaignApi methods
        var cls = CampaignApi.class;
        var m1 = cls.getMethod("createCampaign", CreateCampaignRequest.class);
        Operation op = m1.getAnnotation(Operation.class);
        assertNotNull(op);
        assertEquals("createCampaign", op.operationId());

        var m2 = cls.getMethod("getById", java.util.UUID.class);
        Operation op2 = m2.getAnnotation(Operation.class);
        assertNotNull(op2);
        assertEquals("getCampaignById", op2.operationId());
    }
}

