<#-- SmartKin: verification code (es). Params: vc, user_name (what smartkin-backend sends); expiresInMinutes is optional. See docs/smartkin-mail-templates.md. -->
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Verifica tu correo en SmartKin</title>
    <style>
        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; color: #212529; }
        .header { background-color: #f8f9fa; padding: 16px; text-align: center; border-radius: 5px; }
        .code { display: inline-block; background-color: #e9ecef; padding: 12px 20px; font-size: 26px; letter-spacing: 5px; border-radius: 5px; }
        .safety { background-color: #fff8e1; padding: 12px 16px; border-radius: 5px; font-size: 14px; }
        .muted, .footer { font-size: 12px; color: #6c757d; }
        .footer { margin-top: 24px; text-align: center; }
    </style>
</head>
<body>
<div class="container">
    <div class="header"><h1>Verifica tu correo</h1></div>
    <p>Hola ${user_name?html},</p>
    <p>Gracias por crear tu cuenta en SmartKin, la plataforma que ayuda a las familias a cuidar su vida digital. Usa este código para confirmar tu correo:</p>
    <p style="text-align: center;"><span class="code">${vc?html}</span></p>
    <#if expiresInMinutes??>
    <p class="muted">El código es válido por ${expiresInMinutes?html} minutos.</p>
    <#else>
    <p class="muted">El código tiene una vigencia limitada. Si ya venció, pide uno nuevo desde la aplicación.</p>
    </#if>
    <p class="safety">Si no creaste una cuenta en SmartKin, ignora este mensaje: nadie puede usar tu correo sin este código. SmartKin nunca te pedirá este código por teléfono, chat ni redes sociales.</p>
    <p class="footer">Este es un correo automático, por favor no respondas a este mensaje.</p>
</div>
</body>
</html>
