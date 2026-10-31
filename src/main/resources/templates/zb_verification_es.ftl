<#-- ZeroBound: verification (es). Params: see docs/zerobound-mail-templates.md. Amounts arrive already masked/formatted by zerobound. -->
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Verifica tu correo</title>
    <style>
        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #f8f9fa; padding: 16px; text-align: center; border-radius: 5px; }
        .code { display: inline-block; background-color: #e9ecef; padding: 12px 20px; font-size: 24px; letter-spacing: 5px; border-radius: 5px; }
        .muted, .footer { font-size: 12px; color: #6c757d; }
    </style>
</head>
<body>
<div class="container">
    <div class="header"><h1>Verifica tu correo</h1></div>
    <p>Usa este código para verificar tu correo en ZeroBound:</p>
    <p><span class="code">${vc?html}</span></p>
    <p class="muted">Código válido por ${expiresInMinutes?html} minutos.</p>
    <p class="footer">Este es un correo automático, no respondas a este mensaje. Si no solicitaste este código, ignora este mensaje.</p>
</div>
</body>
</html>
