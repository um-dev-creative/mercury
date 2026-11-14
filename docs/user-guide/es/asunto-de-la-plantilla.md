# Asunto del correo: se define en la plantilla

El asunto de un correo es parte de la **plantilla**, así que se cambia desde Iris, en el editor de plantillas, sin pedir un
cambio de código ni un despliegue.

## Definirlo o cambiarlo
1. En Iris, abre la plantilla (cada idioma es una plantilla: `…_es`, `…_en`).
2. Completa **Asunto**. Puedes usar los datos del mensaje entre `${ }`: `Tu código es ${vc}`, `Bienvenido, ${name}`.
3. Guarda. El siguiente correo enviado con esa plantilla lo usa.

Para quitarlo, guarda el campo vacío.

## Reglas
- Hasta 500 caracteres, una sola línea.
- Cada dato usado debe enviarlo la aplicación. Si el asunto dice `${name}` y el mensaje no trae `name`, el mensaje se rechaza con un error claro en vez de salir con el asunto roto.
- Si la aplicación envía además un asunto propio, **ese gana**. Las aplicaciones que siguen esta guía no envían ninguno.
- Una plantilla sin asunto y un mensaje sin asunto se rechazan: `subject is required for email`.

## Antes de depender de esto
Completa el asunto de **cada idioma** que ofrezcas; cada uno es una plantilla distinta.
