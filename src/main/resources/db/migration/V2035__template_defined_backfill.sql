-- Templates created through the management API never got a row in mercury.template_defined, the table campaigns and POST /api/v1/messages read,
-- so every send by template name failed with "not registered for this application". The service now writes it on create; this fills the gap.

-- the one frequency every definition gets: a template is sent when asked for, not on a schedule
INSERT INTO mercury.frequency_type (id, name, description, created_at, updated_at, active)
SELECT general.uuid_generate_v4(), 'ON_DEMAND', 'Sent when requested, not on a schedule', now(), now(), true
WHERE NOT EXISTS (SELECT 1 FROM mercury.frequency_type WHERE upper(name) = 'ON_DEMAND');

-- every active template without a definition gets one (user_id: the nil UUID, the creator was never recorded)
INSERT INTO mercury.template_defined (id, template_id, user_id, application_id, created_at, updated_at, expired_at, is_active, frequency_type)
SELECT general.uuid_generate_v4(), t.id, '00000000-0000-0000-0000-000000000000', t.application_id, now(), now(), NULL, true,
       (SELECT f.id FROM mercury.frequency_type f WHERE upper(f.name) = 'ON_DEMAND' AND f.active ORDER BY f.created_at LIMIT 1)
FROM mercury.templates t
WHERE COALESCE(t.active, true)
  AND t.application_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM mercury.template_defined d WHERE d.template_id = t.id);
