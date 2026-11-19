-- Templates created through the management API / Iris before the language was derived from the file name were saved with language NULL, so the
-- catalogue (GET /api/v1/messages/templates) and sends by name ignored them. Fill it from the end of the location (..._es.ftl or ...-es.ftl).
UPDATE mercury.templates
SET language = substring(location FROM '[_-]([A-Za-z]{2}(?:-[A-Za-z]{2})?)\.[A-Za-z0-9]+$')
WHERE (language IS NULL OR btrim(language) = '')
  AND location ~ '[_-][A-Za-z]{2}(?:-[A-Za-z]{2})?\.[A-Za-z0-9]+$';
