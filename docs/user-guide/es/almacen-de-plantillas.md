# Guía de usuario: dónde viven las plantillas

🌐 [Read this in English](../en/template-storage.md)

El texto de cada mensaje que envía Mercury (el correo de verificación, el mensaje de una campaña) sale de un archivo de plantilla. Mercury busca esos archivos en una sola carpeta, definida con `TEMPLATE_PATH` (por defecto `/usr/local/runme/templates`; en Docker puedes montar tu propia carpeta encima con `MERCURY_TEMPLATES_HOST_PATH`).

## Una carpeta por aplicación

Cada aplicación guarda sus plantillas en su propia carpeta: `<TEMPLATE_PATH o TEMPLATE_S3_PREFIX>/<carpeta>/<archivo>`. Por ejemplo, con `TEMPLATE_S3_PREFIX=lth/mercury/templates/` las plantillas de Iris son `lth/mercury/templates/iris/iris_welcome.ftl`, y las demás están en `smartkind/`, `zerobound/` y `latinhub/`.

- El `location` de una plantilla sigue siendo el nombre del archivo (`iris_welcome.ftl`). Mercury añade la carpeta de la aplicación de la plantilla, tanto al enviar un mensaje como al editarla desde Iris, así que ambos usan el mismo archivo.
- Cada aplicación tiene su propia carpeta y la gestiona el propio Mercury: al crear la **primera plantilla de una aplicación nueva**, Mercury crea su carpeta (con el id de la aplicación como nombre) y la usa desde entonces. No hay que configurar nada cuando aparece una aplicación nueva.
- Para darle a una aplicación un nombre de carpeta más amigable, usa `PUT /api/v1/templates/applications/<id de la aplicación>/folder` con el cuerpo `{"folder": "mi-app"}` (necesitas el permiso template-manage en esa aplicación; `GET` sobre la misma ruta muestra la actual). Una carpeta pertenece a una sola aplicación. Cambiarla **no mueve** los archivos: copia antes las plantillas a la carpeta nueva.
- Las aplicaciones que ya guardan sus plantillas en una carpeta con nombre se registran una vez con el script `scripts/templates/seed-application-folders.sql` (revisa antes los ids de aplicación que trae). No hay ninguna variable de configuración para las carpetas, y una aplicación nueva no necesita nada.
- Las plantillas de Telegram de ZeroBound (`zb_<tipo>_tg_<idioma>.ftl`) se leen de `TEMPLATE_TELEGRAM_FOLDER` (por defecto `zerobound`).
- Una carpeta usa minúsculas, dígitos, `_` y `-`, hasta 64 caracteres. Solo hay un nivel de carpetas.
- Una aplicación que aún no tiene carpeta (sin plantillas creadas desde Mercury desde que existen las carpetas) sigue funcionando con sus archivos en la raíz del almacén.
- Cloudflare R2: `TEMPLATE_STORAGE=s3` más `CLOUDFLARE_R2_BUCKET_NAME` (bucket), `CLOUDFLARE_R2_ENDPOINT` (`https://<id de cuenta>.r2.cloudflarestorage.com`), `CLOUDFLARE_R2_ACCESS_KEY` y `CLOUDFLARE_R2_SECRET_KEY` (secretos, guárdalos en Vault), `TEMPLATE_S3_PREFIX=lth/mercury/templates/` (ruta base de las plantillas), `TEMPLATE_S3_REGION=auto` y `TEMPLATE_S3_PATH_STYLE=true`.

Para ver qué carpeta usa una aplicación, llama a `GET /api/v1/templates/applications/<id de la aplicación>/folder` o lee la tabla `mercury.application_template_folders` (id de la aplicación y carpeta).

## Lo que debes saber

- Una plantilla es un solo archivo con letras, números, `_`, `-` y `.`, que termina en `.ftl`, `.ftlh`, `.md` o `.html`. No puede estar en una subcarpeta.
- Puedes agregar o cambiar un archivo en la carpeta y Mercury lo usa en unos 5 segundos, sin reiniciar.
- El nombre que le das a una plantilla en Mercury (su `location`) es ese nombre de archivo.
- **La carpeta debe poder escribirla Mercury** (con `TEMPLATE_STORAGE=local`): al guardar una plantilla editada o creada desde Iris se escribe el archivo, y se crea la subcarpeta de la aplicación, ahí. Un montaje de solo lectura (por ejemplo un volumen de Docker con `:ro`) hace que cada guardado responda `500` con `Read-only file system`; monta la carpeta con escritura, da permiso de escritura al usuario del contenedor, o guarda las plantillas en un bucket.
- Si la carpeta no existe al arrancar, Mercury se detiene con un error que nombra la carpeta. Créala o corrige `TEMPLATE_PATH`.
- Una plantilla que no se encuentra, o con un nombre no permitido, se omite y el mensaje usa el texto plano de la campaña.

## Usar un bucket en lugar de una carpeta

Si ejecutas varias instancias de Mercury, o no quieres las plantillas dentro del servidor, guárdalas en un bucket de Amazon S3 o de un servicio compatible con S3 (MinIO, por ejemplo): define `TEMPLATE_STORAGE=s3` y `CLOUDFLARE_R2_BUCKET_NAME` (y `CLOUDFLARE_R2_ENDPOINT` con `TEMPLATE_S3_PATH_STYLE=true` para MinIO). Sube los archivos de plantilla al bucket bajo `TEMPLATE_S3_PREFIX` antes de reiniciar Mercury. Desde entonces, las plantillas que edites desde Iris se escriben en el bucket y todas las instancias las usan en pocos segundos. La clave de acceso y el secreto son secretos: guárdalos en Vault, o déjalos vacíos para usar el rol de AWS del servidor.

## Cómo escribir una plantilla

Una plantilla es **FreeMarker/HTML** (`.ftl`, `.ftlh`, `.html`) o **Markdown** (`.md`).

- En ambas, `${nombre}` se sustituye por el valor del parámetro `nombre`; `<#if nombre??>…</#if>` muestra una parte solo si se envió; `<#list items as item>…</#list>` repite una parte.
- Una plantilla Markdown se escribe en Markdown (`# Título`, `**negrita**`, `- lista`, `[texto](https://…)`) y Mercury la convierte en HTML al enviar el mensaje. El HTML escrito dentro de Markdown se muestra como texto, no se ejecuta; usa una plantilla `.html` si necesitas HTML.
- Una plantilla con un error no se puede guardar; el error indica la línea y la columna.
- Si dos personas editan la misma plantilla, el segundo guardado se rechaza hasta que recargue, para que nadie pierda el trabajo del otro.
- Cada guardado se conserva como una versión, así que puedes volver atrás. Restaurar una versión antigua la guarda como una nueva; no se borra nada (se conservan las últimas 50).
- Las plantillas de las que dependen otros sistemas (nombres que empiezan por `zb_`, `sk_`, `iris_`, `ds_`) piden una confirmación explícita para guardar, y se conserva el original para poder restaurarlo siempre.

## Ediciones seguras

Las plantillas no pueden crear objetos Java ni usar la API de Java (ver `docs/security/template-security.md`): `?new` y `?api` se rechazan.

## Listar plantillas

`GET /api/v1/templates` lista las plantillas de una aplicación, con filtros opcionales por tipo, severidad, estado activo o texto. Si responde `500 An unexpected error occurred` y el log dice `Could not resolve attribute 'application'`, la imagen en ejecución es anterior a la corrección; despliega la versión actual. No hay nada que configurar.


> **Mercury ya no incluye plantillas.** La carpeta `src/main/resources/templates` no existe y las imágenes no contienen plantillas. Dónde viven lo decide `TEMPLATE_STORAGE`: `s3` (o `r2`, `cloud`) = el bucket de Cloudflare R2; `local` = la carpeta `TEMPLATE_PATH`; cualquier otro valor es la ruta de la propia carpeta local (se crea si no existe). Sube los archivos de plantilla al almacenamiento elegido antes de iniciar Mercury.
