<#-- SmartKin: verification code (en). Params: vc, user_name (what smartkin-backend sends); expiresInMinutes is optional. See docs/smartkin-mail-templates.md. -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Verify your email on SmartKin</title>
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
    <div class="header"><h1>Verify your email</h1></div>
    <p>Hello ${user_name?html},</p>
    <p>Thank you for creating your SmartKin account, the platform that helps families look after their digital life. Use this code to confirm your email:</p>
    <p style="text-align: center;"><span class="code">${vc?html}</span></p>
    <#if expiresInMinutes??>
    <p class="muted">The code is valid for ${expiresInMinutes?html} minutes.</p>
    <#else>
    <p class="muted">The code is valid for a limited time. If it has expired, ask for a new one from the app.</p>
    </#if>
    <p class="safety">If you did not create a SmartKin account, ignore this message: nobody can use your email without this code. SmartKin will never ask you for this code by phone, chat or social media.</p>
    <p class="footer">This is an automated email, please do not reply to it.</p>
</div>
</body>
</html>
