# Local/dev Vault server config for docker-compose (see docker-compose.yml's
# vault-server service). File-backed storage persists into the named volume
# vault-data (mounted at /vault/file), so unsealing survives container restarts
# but not `docker compose down -v`. TLS is intentionally off here — this is a
# throwaway dev instance on an internal compose network, not a path to a real
# environment's Vault, which terminates TLS itself.
storage "file" {
  path = "/vault/file"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = true
}

api_addr = "http://0.0.0.0:8200"
ui       = true
