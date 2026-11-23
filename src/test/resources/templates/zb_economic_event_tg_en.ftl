<#-- ZeroBound Telegram: economic_event (en). -->
<#if phase == "released">📊 <b>Economic figure released</b><#else>🗓 <b>Upcoming economic event</b></#if>
<b>${title?html}</b> (${country?html}) · ${scheduledAt?html}
<#if phase == "released">Actual: ${actual?html} · Forecast: ${forecast?html} · Previous: ${previous?html}<#else>Previous: ${previous?html} · Forecast: ${forecast?html}</#if>
<i>For information only, not financial advice.</i>
