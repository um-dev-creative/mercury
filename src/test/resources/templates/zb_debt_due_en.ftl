<#-- ZeroBound: debt_due (en). Params: see docs/zerobound-mail-templates.md. Amounts arrive already masked/formatted by zerobound. -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Debt due</title>
    <style>
        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #f8f9fa; padding: 16px; text-align: center; border-radius: 5px; }
        .code { display: inline-block; background-color: #e9ecef; padding: 12px 20px; font-size: 24px; letter-spacing: 5px; border-radius: 5px; }
        .muted, .footer { font-size: 12px; color: #6c757d; }
    </style>
</head>
<body>
<div class="container">
    <div class="header"><h1>Debt due</h1></div>
    <p>Your debt <strong>${debtName?html}</strong> is due on <strong>${dueDate?html}</strong>.</p>
    <p>Payment amount: ${amount?html}.</p>
    <p class="footer">This is an automatic email, please do not reply. You can turn these notices off in your ZeroBound preferences.</p>
</div>
</body>
</html>
