# Guía de usuario: dónde viven las plantillas

🌐 [Read this in English](../en/template-storage.md)

El texto de cada mensaje que envía Mercury (el correo de verificación, el mensaje de una campaña) sale de un archivo de plantilla. Mercury busca esos archivos en una sola carpeta, definida con `TEMPLATE_PATH` (por defecto `/usr/local/runme/templates`; en Docker puedes montar tu propia carpeta encima con `MERCURY_TEMPLATES_HOST_PATH`).

## Lo que debes saber

- Una plantilla es un solo archivo con letras, números, `_`, `-` y `.`, que termina en `.ftl`, `.ftlh`, `.md` o `.html`. No puede estar en una subcarpeta.
- Puedes agregar o cambiar un archivo en la carpeta y Mercury lo usa en unos 5 segundos, sin reiniciar.
- El nombre que le das a una plantilla en Mercury (su `location`) es ese nombre de archivo.
- Si la carpeta no existe al arrancar, Mercury se detiene con un error que nombra la carpeta. Créala o corrige `TEMPLATE_PATH`.
- Una plantilla que no se encuentra, o con un nombre no permitido, se omite y el mensaje usa el texto plano de la campaña.

## Ediciones seguras

Las plantillas no pueden crear objetos Java ni usar la API de Java (ver `docs/security/template-security.md`): `?new` y `?api` se rechazan.
