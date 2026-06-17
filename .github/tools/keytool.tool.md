# Tool: Keytool (Java Keystore Management)

**Purpose:** Manage Mercury's TLS keystores (`mercury.jks`) and truststores (`umdc-truststore.jks`).

## Mercury Keystores

| File | Purpose | Password Property |
|---|---|---|
| `mercury.jks` | Application TLS identity (private key + certificate) | `${prx.ssl.keystore.password}` |
| `umdc-truststore.jks` | Trusted CA certificates | `${prx.ssl.truststore.password}` |

Passwords are resolved from Vault via `bootstrap.yml` — never hardcoded.

## Common Keytool Commands

### List Keystore Contents
```bash
keytool -list -keystore mercury.jks \
        -storepass "${SSL_KEYSTORE_PASSWORD}" \
        -v
```

### List Truststore Contents
```bash
keytool -list -keystore umdc-truststore.jks \
        -storepass "${SSL_TRUSTSTORE_PASSWORD}" \
        -v
```

### Generate New Key Pair (self-signed, dev only)
```bash
keytool -genkeypair \
        -alias mercury \
        -keyalg RSA \
        -keysize 2048 \
        -validity 365 \
        -keystore mercury.jks \
        -storepass "${SSL_KEYSTORE_PASSWORD}" \
        -dname "CN=mercury,OU=Engineering,O=UMDC,L=City,ST=State,C=US"
```

### Export Certificate from Keystore
```bash
keytool -exportcert \
        -alias mercury \
        -keystore mercury.jks \
        -storepass "${SSL_KEYSTORE_PASSWORD}" \
        -file mercury.crt \
        -rfc
```

### Import Certificate into Truststore
```bash
keytool -importcert \
        -alias <ca-alias> \
        -file ca.crt \
        -keystore umdc-truststore.jks \
        -storepass "${SSL_TRUSTSTORE_PASSWORD}" \
        -noprompt
```

### Delete Entry from Keystore
```bash
keytool -delete \
        -alias <old-alias> \
        -keystore mercury.jks \
        -storepass "${SSL_KEYSTORE_PASSWORD}"
```

### Check Certificate Expiry
```bash
keytool -list -keystore mercury.jks \
        -storepass "${SSL_KEYSTORE_PASSWORD}" \
        -v | grep "Valid from"
```

## bootstrap.yml SSL Configuration Pattern
```yaml
server:
  ssl:
    key-store: classpath:mercury.jks
    key-store-password: ${prx.ssl.keystore.password}
    key-store-type: JKS
    trust-store: classpath:umdc-truststore.jks
    trust-store-password: ${prx.ssl.truststore.password}
    trust-store-type: JKS
```

## Security Rules
- NEVER commit keystore passwords in source code or config files
- NEVER commit `.jks` files with weak passwords
- ALWAYS use `${prx.ssl.*}` Vault-backed properties for passwords
- Certificate rotation: generate new pair, update Vault secret, restart Mercury
- Dev keystores (self-signed) must NOT be used in production

## Notes
- Keystores are Java KeyStore (JKS) format
- Located in `src/main/resources/` (classpath) or external path in bootstrap.yml
- Production keystores should use CA-signed certificates
- Run `keytool` commands from the directory containing the `.jks` file
