# Runbook: rotación de credenciales del keystore filtrado

## Contexto

`src/main/resources/keystore.jks` estuvo committeado en `main`/`develop` desde 2026-02-14 en un repositorio público (`um-dev-creative/mercury`). Cualquier credencial protegida por ese archivo debe tratarse como comprometida. Este documento cubre:

- **Fase 1** — confirmar si el material filtrado coincide con lo que está activo hoy en algún ambiente real.
- **Fase 2** — rotar las credenciales donde la Fase 1 confirme (o no descarte) coincidencia.

La **Fase 4** (purga del historial de git con `git filter-repo`) se documenta aparte y solo debe ejecutarse **después** de completar la Fase 2.

---

## Fase 1 — Confirmar uso real

**Objetivo:** comparar la huella digital (fingerprint) del archivo filtrado contra lo que está realmente desplegado, sin asumir nada.

### Paso A — Extraer el archivo filtrado del historial de git (una sola vez)

```bash
git show main:src/main/resources/keystore.jks > leaked-keystore.jks
```

### Paso B — Sacar la huella del archivo filtrado

La contraseña por defecto que usa este proyecto en todos los fallbacks es `changeit` (ver `bootstrap.yml`: `${SSL_KEYSTORE_PASSWORD:changeit}`). Probar primero con esa.

**Windows (PowerShell, requiere JDK — `keytool` viene incluido):**
```powershell
keytool -list -v -keystore leaked-keystore.jks -storepass changeit | Select-String "SHA256|Alias|Valid"
```

**Linux / macOS:**
```bash
keytool -list -v -keystore leaked-keystore.jks -storepass changeit | grep -E "SHA256|Alias|Valid"
```

**Docker (sin JDK local, usando un contenedor descartable):**
```bash
docker run --rm -v "$(pwd)/leaked-keystore.jks:/tmp/leaked-keystore.jks:ro" \
  eclipse-temurin:21-jdk \
  keytool -list -v -keystore /tmp/leaked-keystore.jks -storepass changeit
```
> Windows PowerShell: reemplazar `$(pwd)` por `${PWD}`.

Anotar el **SHA256 del certificado** y las **fechas de validez** — eso es lo que se compara contra lo desplegado.

### Paso C — Sacar la huella de lo que está realmente activo

#### C.1 — Si hay acceso directo a Vault

**Linux/macOS:**
```bash
export VAULT_ADDR="https://tu-vault-real:8200"
vault login
vault kv get secret/mercury/<ambiente>   # ajustar el path real
```

**Windows (PowerShell), Vault CLI vía `choco install vault` o binario manual:**
```powershell
$env:VAULT_ADDR = "https://tu-vault-real:8200"
vault login
vault kv get secret/mercury/<ambiente>
```

Revisar `SSL_KEYSTORE_LOCATION`, `SSL_KEYSTORE_PASSWORD`, `SSL_KEYSTORE_TYPE`. Si `SSL_KEYSTORE_LOCATION` apunta a `classpath:...`, es una señal fuerte de que es el mismo `keystore.jks`.

#### C.2 — Servicio corriendo directo en un host, sin Vault a mano

**Linux:**
```bash
ps aux | grep mercury
lsof -p <PID> | grep -iE "jks|p12"
```

**Windows (PowerShell):**
```powershell
Get-Process -Name java | Select-Object Id, Path
```

Con la ruta real:
```bash
keytool -list -v -keystore /ruta/real/al/keystore -storepass <password-real> | grep SHA256
```

#### C.3 — Docker / Kubernetes

**Docker:**
```bash
docker exec -it <container> sh -c 'printenv | grep SSL_KEYSTORE'
docker cp <container>:/ruta/dentro/del/contenedor/keystore.jks ./deployed-keystore.jks
keytool -list -v -keystore deployed-keystore.jks -storepass <password-real> | grep SHA256
```

**Kubernetes:**
```bash
kubectl exec -it <pod> -n <namespace> -- sh -c 'printenv | grep SSL_KEYSTORE'
kubectl cp <namespace>/<pod>:/ruta/al/keystore.jks ./deployed-keystore.jks
keytool -list -v -keystore deployed-keystore.jks -storepass <password-real> | grep SHA256
```

### Paso D — Comparar y concluir

| Resultado | Conclusión |
|---|---|
| SHA256 del filtrado == SHA256 del desplegado | **Confirmado comprometido** → ejecutar Fase 2 en ese ambiente |
| SHA256 distinto | Lo activo ya es otro material — rotar igual por precaución, prioridad menor. Revisar también `prx_backbone_rest*.jks` / `prx_srv_monitor.jks` (2022) con el mismo método |
| No se encuentra el archivo en ningún ambiente activo | Era huérfano/de desarrollo local — bajar prioridad, documentar como cerrado |

---

## Fase 2 — Rotar credenciales

Ejecutar solo en los ambientes donde la Fase 1 confirmó coincidencia (o no pudo descartarla).

### Paso A — Generar el keystore nuevo

`keytool` es idéntico en Windows/Linux/macOS (viene con el JDK); solo cambia la sintaxis del shell.

**Autofirmado:**

Linux/macOS:
```bash
keytool -genkeypair -alias mercury \
  -keyalg RSA -keysize 4096 -validity 730 \
  -storetype PKCS12 \
  -keystore mercury-keystore-new.p12 \
  -storepass "$(openssl rand -base64 24)" \
  -dname "CN=mercury.tu-dominio.com, OU=Engineering, O=UMDC, L=Ciudad, ST=Estado, C=US"
```

Windows PowerShell:
```powershell
$newPass = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 32 | % {[char]$_})
keytool -genkeypair -alias mercury `
  -keyalg RSA -keysize 4096 -validity 730 `
  -storetype PKCS12 `
  -keystore mercury-keystore-new.p12 `
  -storepass $newPass `
  -dname "CN=mercury.tu-dominio.com, OU=Engineering, O=UMDC, L=Ciudad, ST=Estado, C=US"
Write-Host "Password generada: $newPass"   # guardarla en Vault, no dejarla en pantalla
```

**Certificado real emitido por una CA** (si `Issuer` != `Subject` en el `keytool -list -v` del Paso B de la Fase 1):
```bash
keytool -genkeypair -alias mercury -keyalg RSA -keysize 4096 \
  -storetype PKCS12 -keystore mercury-keystore-new.p12 -storepass "<nueva-password>" \
  -dname "CN=mercury.tu-dominio.com, O=UMDC, C=US"
keytool -certreq -alias mercury -keystore mercury-keystore-new.p12 -storepass "<nueva-password>" -file mercury.csr

# Enviar mercury.csr a la CA original, pedir un certificado nuevo, luego importar la cadena:
keytool -importcert -alias root -file ca-root.crt -keystore mercury-keystore-new.p12 -storepass "<nueva-password>"
keytool -importcert -alias intermediate -file ca-intermediate.crt -keystore mercury-keystore-new.p12 -storepass "<nueva-password>"
keytool -importcert -alias mercury -file mercury-signed.crt -keystore mercury-keystore-new.p12 -storepass "<nueva-password>"
```

### Paso B — Actualizar Vault con el material nuevo

**Linux/macOS:**
```bash
vault kv put secret/mercury/<ambiente> \
  SSL_KEYSTORE_PASSWORD="<nueva-password>" \
  SSL_KEYSTORE_TYPE="PKCS12"

base64 -i mercury-keystore-new.p12 -o mercury-keystore-new.b64
vault kv put secret/mercury/<ambiente>/keystore-file content=@mercury-keystore-new.b64
```

**Windows PowerShell:**
```powershell
vault kv put secret/mercury/<ambiente> `
  SSL_KEYSTORE_PASSWORD="<nueva-password>" `
  SSL_KEYSTORE_TYPE="PKCS12"

[Convert]::ToBase64String([IO.File]::ReadAllBytes("mercury-keystore-new.p12")) | Out-File mercury-keystore-new.b64
vault kv put secret/mercury/<ambiente>/keystore-file content=@mercury-keystore-new.b64
```

> Ajustar `secret/mercury/<ambiente>` al path real del `config-server` de este proyecto (referenciado en `docker-compose.yml` vía `SPRING_CLOUD_CONFIG_LABEL`).

### Paso C — Desplegar el archivo nuevo

**Linux (systemd):**
```bash
sudo cp mercury-keystore-new.p12 /opt/mercury/config/keystore.p12
sudo chown mercury:mercury /opt/mercury/config/keystore.p12
sudo chmod 600 /opt/mercury/config/keystore.p12
sudo systemctl restart mercury
```

**Windows (servicio de Windows):**
```powershell
Copy-Item .\mercury-keystore-new.p12 "C:\mercury\config\keystore.p12" -Force
Restart-Service -Name "Mercury"
```

**Docker (contenedor suelto, volumen montado):**
```bash
cp mercury-keystore-new.p12 /ruta/host/montada/keystore.p12
# docker restart NO relee variables de entorno nuevas.
# Si la password cambió, hay que recrear el contenedor:
docker compose up -d --force-recreate mercury
```

**Docker Swarm:**
```bash
docker secret create mercury_keystore_v2 mercury-keystore-new.p12
docker service update \
  --secret-rm mercury_keystore_v1 \
  --secret-add source=mercury_keystore_v2,target=keystore.p12 \
  mercury_stack_mercury
```

**Kubernetes:**
```bash
kubectl create secret generic mercury-keystore-v2 \
  --from-file=keystore.p12=mercury-keystore-new.p12 -n <namespace>
kubectl set env deployment/mercury SSL_KEYSTORE_PASSWORD="<nueva-password>" -n <namespace>
kubectl rollout restart deployment/mercury -n <namespace>
```

### Paso D — Verificar

```bash
openssl s_client -connect <host>:<puerto> -showcerts </dev/null 2>/dev/null | openssl x509 -noout -fingerprint -sha256
```
Confirmar que el SHA256 coincide con el keystore nuevo y no con el filtrado (Fase 1, Paso B).

### Paso E — Revocar el certificado viejo (solo si era de una CA real)

Contactar a la CA emisora con el número de serie del certificado viejo (`keytool -list -v`) y pedir revocación por CRL/OCSP. Si era autofirmado, simplemente no reutilizarlo.

---

## Siguiente paso

Una vez confirmada la rotación activa en los ambientes que correspondía, continuar con la **Fase 4** (purga del historial de git con `git filter-repo` + force-push coordinado con el equipo) — documentada por separado.
