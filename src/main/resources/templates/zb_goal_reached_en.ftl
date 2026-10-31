<#-- ZeroBound: goal_reached (en). Params: see docs/zerobound-mail-templates.md. Amounts arrive already masked/formatted by zerobound. -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Goal reached</title>
    <style>
        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #f8f9fa; padding: 16px; text-align: center; border-radius: 5px; }
        .code { display: inline-block; background-color: #e9ecef; padding: 12px 20px; font-size: 24px; letter-spacing: 5px; border-radius: 5px; }
        .muted, .footer { font-size: 12px; color: #6c757d; }
    </style>
</head>
<body>
<div class="container">
    <div class="header"><h1>Goal reached</h1></div>
    <p>Congratulations! You reached your goal <strong>${goalName?html}</strong>.</p>
    <p>Target: ${target?html}.</p>
    <p class="footer">This is an automatic email, please do not reply. You can turn these notices off in your ZeroBound preferences.</p>
</div>
</body>
</html>
