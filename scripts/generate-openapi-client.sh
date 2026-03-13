#!/usr/bin/env bash
# Generate OpenAPI client/spec artifacts from the project's OpenAPI YAML
# Prerequisites: openapi-generator-cli installed or placed at TOOLS/openapi-generator-cli.jar

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OPENAPI_YAML="$ROOT_DIR/src/main/resources/api/openapi.yaml"
OUT_DIR="$ROOT_DIR/target/openapi-client"

mkdir -p "$OUT_DIR"

if command -v openapi-generator-cli >/dev/null 2>&1; then
  echo "Using openapi-generator-cli from PATH"
  openapi-generator-cli generate -i "$OPENAPI_YAML" -g openapi -o "$OUT_DIR/spec" || true
else
  if [ -f "$ROOT_DIR/TOOLS/openapi-generator-cli.jar" ]; then
    echo "Using local TOOLS/openapi-generator-cli.jar"
    java -jar "$ROOT_DIR/TOOLS/openapi-generator-cli.jar" generate -i "$OPENAPI_YAML" -g openapi -o "$OUT_DIR/spec" || true
  else
    echo "openapi-generator-cli not found. Install it (npm i -g @openapitools/openapi-generator-cli) or place jar in TOOLS/"
    exit 2
  fi
fi

cp "$OPENAPI_YAML" "$OUT_DIR/openapi.yaml"
python3 - <<PY
import sys, yaml, json
with open('$OPENAPI_YAML') as f:
    o = yaml.safe_load(f)
with open('$OUT_DIR/openapi.json','w') as jf:
    json.dump(o, jf, indent=2)
print('Wrote JSON and copied YAML to', '$OUT_DIR')
PY

echo "OpenAPI client/spec generation complete. See $OUT_DIR"

