-- Registers the template folder of applications whose templates ALREADY live in a named folder of the template storage
-- (<root or s3 prefix>/<folder>/<file>). It is a one-time data step for existing applications, not configuration: Mercury registers the
-- folder of every NEW application by itself when its first template is created, and PUT /api/v1/templates/applications/{id}/folder
-- changes one. An application without a row keeps its templates at the root of the storage.
--
-- If Mercury already gave an application the default folder (named like its id) because its first template was created before this script
-- ran, the row is switched to the folder named here; a row with any other folder, chosen on purpose, is left as it is.
--
-- The ids are those of the Backbone database of QA. Check them against the applications of your environment (the Applications screen
-- of Stratum) before running, and edit the list. It never overwrites a folder that was chosen on purpose, and a folder another application
-- already owns makes the script stop with an error. It does not read the `general` schema (nothing in Mercury does); run it against the Mercury schema, after V2033.
--   psql -f scripts/templates/seed-application-folders.sql

INSERT INTO mercury.application_template_folders (application_id, folder)
VALUES ('8249aa97-23fa-47cd-9c6d-4685f0c03cc3', 'iris'),        -- Iris
       ('c9291036-fd71-4f2b-9986-2c5d4e5a0f8b', 'latinhub'),    -- LatinHub
       ('137519ec-c070-4a82-b5d3-1a2b6638433b', 'lucky_le'),    -- Lucky Leaf
       ('e50b2e01-1a40-43a3-aa55-90bc9b207fa3', 'smartkind'),   -- Smart Kin
       ('c913b241-866e-4f1d-9ee4-f241b00d6475', 'zerobound')    -- Zeronbound REST
ON CONFLICT (application_id) DO UPDATE
    SET folder = EXCLUDED.folder, updated_at = CURRENT_TIMESTAMP
    WHERE mercury.application_template_folders.folder = mercury.application_template_folders.application_id::text;

-- Lucky Leaf REST (9c1573bf-b328-4ad2-b808-6040e5c15787) used the same folder as Lucky Leaf (lucky_le). A folder belongs to one
-- application, so give it its own folder, or move its templates, before registering it.

SELECT application_id, folder FROM mercury.application_template_folders ORDER BY folder;
