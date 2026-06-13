---
name: Docker Build
description: Tool for building the Mercury Docker image from the Dockerfile at project root
type: terminal
command-prefix: docker
used-by: [devops-engineer]
---

## Purpose

Builds the Mercury Docker image from the `Dockerfile` at the project root. The image wraps the Spring Boot fat JAR produced by Maven.

## Available Commands

### Build Maven artifact first
```bash
mvn -U clean package -DskipTests
```
Produces `target/mercury-0.0.1.jar` required by the Dockerfile.

### Build Docker image
```bash
docker build -t mercury:latest .
docker build -t mercury:0.0.1 .
```

### Build with build args (for private repo credentials)
```bash
docker build \
  --build-arg REPSY_USERNAME=<user> \
  --build-arg REPSY_PASSWORD=<pass> \
  -t mercury:latest .
```

### Run container locally
```bash
docker run --env-file default.env -p 8080:8080 mercury:latest
```

### Tag and push to registry
```bash
docker tag mercury:latest <registry>/mercury:0.0.1
docker push <registry>/mercury:0.0.1
```

## Output Locations

- JAR input: `target/mercury-0.0.1.jar`
- Docker image: local Docker daemon as `mercury:latest` / `mercury:0.0.1`

## Notes

- `Dockerfile` is present at `/Users/lmata/projects/GitHub/mercury/Dockerfile`
- Build the JAR with Maven before running Docker build
- Environment variables for runtime are documented in `environment_variables.md` and `default.env`
- `PRX_KAFKA_AUTO_STARTUP` must be set to `true` in deployed container environment
- Vault and Config Server URLs must be provided via environment variables at container start
