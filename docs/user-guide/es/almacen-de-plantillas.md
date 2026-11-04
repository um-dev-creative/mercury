# Guía de usuario: dónde viven las plantillas

🌐 [Read this in English](../en/template-storage.md)

El texto de cada mensaje que envía Mercury (el correo de verificación, el mensaje de una campaña) sale de un archivo de plantilla. Mercury busca esos archivos en una sola carpeta, definida con `TEMPLATE_PATH` (por defecto `/usr/local/runme/templates`; en Docker puedes montar tu propia carpeta encima con `MERCURY_TEMPLATES_HOST_PATH`).

## Lo que debes saber

- Una plantilla es un solo archivo con letras, números, `_`, `-` y `.`, que termina en `.ftl`, `.ftlh`, `.md` o `.html`. No puede estar en una subcarpeta.
- Puedes agregar o cambiar un archivo en la carpeta y Mercury lo usa en unos 5 segundos, sin reiniciar.
- El nombre que le das a una plantilla en Mercury (su `location`) es ese nombre de archivo.
- Si la carpeta no existe al arrancar, Mercury se detiene con un error que nombra la carpeta. Créala o corrige `TEMPLATE_PATH`.
- Una plantilla que no se encuentra, o con un nombre no permitido, se omite y el mensaje usa el texto plano de la campaña.

## Usar un bucket en lugar de una carpeta

Si ejecutas varias instancias de Mercury, o no quieres las plantillas dentro del servidor, guárdalas en un bucket de Amazon S3 o de un servicio compatible con S3 (MinIO, por ejemplo): define `TEMPLATE_STORAGE=s3` y `TEMPLATE_S3_BUCKET` (y `TEMPLATE_S3_ENDPOINT` con `TEMPLATE_S3_PATH_STYLE=true` para MinIO). Sube los archivos de plantilla al bucket bajo `TEMPLATE_S3_PREFIX` antes de reiniciar Mercury. Desde entonces, las plantillas que edites desde Iris se escriben en el bucket y todas las instancias las usan en pocos segundos. La clave de acceso y el secreto son secretos: guárdalos en Vault, o déjalos vacíos para usar el rol de AWS del servidor.

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
