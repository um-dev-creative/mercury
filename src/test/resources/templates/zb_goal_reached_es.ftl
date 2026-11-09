<#-- ZeroBound: goal_reached (es). Params: see docs/zerobound-mail-templates.md. Amounts arrive already masked/formatted by zerobound. -->
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Meta alcanzada</title>
    <style>
        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #f8f9fa; padding: 16px; text-align: center; border-radius: 5px; }
        .code { display: inline-block; background-color: #e9ecef; padding: 12px 20px; font-size: 24px; letter-spacing: 5px; border-radius: 5px; }
        .muted, .footer { font-size: 12px; color: #6c757d; }
    </style>
</head>
<body>
<div class="container">
    <div class="header"><h1>Meta alcanzada</h1></div>
    <p>¡Felicidades! Alcanzaste tu meta <strong>${goalName?html}</strong>.</p>
    <p>Objetivo: ${target?html}.</p>
    <p class="footer">Este es un correo automático, no respondas a este mensaje. Puedes desactivar estos avisos en tus preferencias de ZeroBound.</p>
</div>
</body>
</html>
