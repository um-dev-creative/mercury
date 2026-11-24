<#-- ZeroBound: economic_event (es). Params: phase (upcoming|released), title, country, scheduledAt, previous, forecast, actual; see docs/zerobound-mail-templates.md. Public macro data, no personal amounts. -->
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Evento económico próximo</title>
    <style>
        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #f8f9fa; padding: 16px; text-align: center; border-radius: 5px; }
        .code { display: inline-block; background-color: #e9ecef; padding: 12px 20px; font-size: 24px; letter-spacing: 5px; border-radius: 5px; }
        .muted, .footer { font-size: 12px; color: #6c757d; }
    </style>
</head>
<body>
<div class="container">
    <div class="header"><h1><#if phase == "released">Dato económico publicado<#else>Evento económico próximo</#if></h1></div>
    <p><strong>${title?html}</strong> (${country?html})</p>
    <#if phase == "released">
    <p>Dato programado para ${scheduledAt?html}.</p>
    <p>Actual: ${actual?html} · Pronóstico: ${forecast?html} · Anterior: ${previous?html}.</p>
    <#else>
    <p>Empieza en menos de una hora, a las ${scheduledAt?html}.</p>
    <p>Anterior: ${previous?html} · Pronóstico: ${forecast?html}.</p>
    </#if>
    <p class="muted">Información pública con fines informativos; no constituye asesoría financiera.</p>
    <p class="footer">Este es un correo automático, no respondas a este mensaje. Puedes desactivar estos avisos en tus preferencias de ZeroBound.</p>
</div>
</body>
</html>
