# Tool: Docker Build

**Purpose:** Build and run Mercury as a Docker container.

## Dockerfile Location
`Dockerfile` — at repository root (`/Users/lmata/projects/GitHub/mercury/Dockerfile`)

## Build Commands

### Build Image
```bash
docker build -t mercury:latest .
```

### Build with Specific Tag (for release)
```bash
docker build -t mercury:1.4.0 -t mercury:latest .
```

### Build Without Cache
```bash
docker build --no-cache -t mercury:latest .
```

## Run Commands

### Run Container (local dev)
```bash
docker run --rm \
  --env-file default.env \
  -p 8080:8080 \
  mercury:latest
```

### Run with Explicit Vault Config
```bash
docker run --rm \
  -e VAULT_HOST=vault \
  -e VAULT_PORT=8200 \
  -e VAULT_TOKEN=dev-token \
  -e DB_URL=jdbc:postgresql://postgres:5432/mercury \
  -p 8080:8080 \
  mercury:latest
```

## Docker Compose (Full Local Stack)
```bash
# Start all services (PostgreSQL + MongoDB + Kafka + Vault + Mercury)
docker compose up -d

# Stop all services
docker compose down

# View Mercury logs
docker compose logs -f mercury

# Rebuild Mercury image and restart
docker compose up -d --build mercury
```

**`docker-compose.yml`** location: repo root

## Key Files
- `Dockerfile` — multi-stage build (Eclipse Temurin 21 JDK builder → JRE runtime)
- `docker-compose.yml` — full local dev stack
- `default.env` — default environment variable values for local dev
- `.github/tools/maven.tool.md` — Maven build used inside Docker multi-stage

## Image Naming Convention
```
mercury:<version>    — versioned tag matching Git release tag (e.g., mercury:1.4.0)
mercury:latest       — always points to latest release
```

## Notes
- The multi-stage Dockerfile runs `mvn -B -DskipTests clean package` in the builder stage
- Final image uses `eclipse-temurin:21-jre-alpine` for minimal footprint
- `bootstrap.yml` inside the image reads from environment variables at runtime
- All secrets injected via environment variables — never baked into the image
