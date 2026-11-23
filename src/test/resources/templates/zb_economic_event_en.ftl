<#-- ZeroBound: economic_event (en). Params: phase (upcoming|released), title, country, scheduledAt, previous, forecast, actual; see docs/zerobound-mail-templates.md. Public macro data, no personal amounts. -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Upcoming economic event</title>
    <style>
        .container { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #f8f9fa; padding: 16px; text-align: center; border-radius: 5px; }
        .code { display: inline-block; background-color: #e9ecef; padding: 12px 20px; font-size: 24px; letter-spacing: 5px; border-radius: 5px; }
        .muted, .footer { font-size: 12px; color: #6c757d; }
    </style>
</head>
<body>
<div class="container">
    <div class="header"><h1><#if phase == "released">Economic figure released<#else>Upcoming economic event</#if></h1></div>
    <p><strong>${title?html}</strong> (${country?html})</p>
    <#if phase == "released">
    <p>Figure scheduled for ${scheduledAt?html}.</p>
    <p>Actual: ${actual?html} · Forecast: ${forecast?html} · Previous: ${previous?html}.</p>
    <#else>
    <p>Starts in less than an hour, at ${scheduledAt?html}.</p>
    <p>Previous: ${previous?html} · Forecast: ${forecast?html}.</p>
    </#if>
    <p class="muted">Public information for general purposes only; it is not financial advice.</p>
    <p class="footer">This is an automatic email, please do not reply. You can turn these notices off in your ZeroBound preferences.</p>
</div>
</body>
</html>
