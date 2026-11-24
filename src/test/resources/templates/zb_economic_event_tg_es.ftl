<#-- ZeroBound Telegram: economic_event (es). -->
<#if phase == "released">📊 <b>Dato económico publicado</b><#else>🗓 <b>Evento económico próximo</b></#if>
<b>${title?html}</b> (${country?html}) · ${scheduledAt?html}
<#if phase == "released">Actual: ${actual?html} · Pronóstico: ${forecast?html} · Anterior: ${previous?html}<#else>Anterior: ${previous?html} · Pronóstico: ${forecast?html}</#if>
<i>Informativo, no es asesoría financiera.</i>
