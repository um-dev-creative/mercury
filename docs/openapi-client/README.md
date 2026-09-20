OpenAPI client & spec generation
================================

This document describes the OpenAPI spec and how to generate client code from it.

Spec location
-------------

- YAML: src/main/resources/api/openapi.yaml

Endpoints (operationId -> path)
--------------------------------

- createCampaign -> POST /api/v1/campaigns
- getCampaignsByApplication -> GET /api/v1/campaigns?applicationId=... (header: session-token)
- getCampaignById -> GET /api/v1/campaigns/{id}
- sendEmail -> POST /api/v1/mail
- sendVerificationCode -> POST /api/v1/verification-code
- getLatestIsVerifiedStatus -> GET /api/v1/verification-code/latest-status?userId=...
- getAllChannelTypes -> GET /api/v1/channel-types
- createChannelType -> POST /api/v1/channel-types
- getEnabledChannelTypes -> GET /api/v1/channel-types/enabled
- getChannelTypeByCode -> GET /api/v1/channel-types/code/{code}
- getChannelTypeById -> GET /api/v1/channel-types/{id}
- updateChannelType -> PUT /api/v1/channel-types/{id}
- toggleChannelType -> PATCH /api/v1/channel-types/{id}/toggle?enabled=true|false

Quick client generation
-----------------------

1. Install OpenAPI Generator CLI (one of):

   - npm: npm i -g @openapitools/openapi-generator-cli
   - or download the jar and place it under TOOLS/openapi-generator-cli.jar

2. Generate a Java client example:

   openapi-generator-cli generate -i src/main/resources/api/openapi.yaml -g java -o target/openapi-client/java-client

3. Or run the provided script (project root):

   ./scripts/generate-openapi-client.sh

Notes
-----
- Controller interfaces were updated to provide explicit operationId values to stabilize generated method names.
- The YAML in src/main/resources/api/openapi.yaml is the canonical contract; update it first when changing API behavior.

