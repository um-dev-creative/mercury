-- Folder of the templates of each application in the template storage (<storage root or s3 prefix>/<folder>/<file>).
-- Mercury owns this table: it never reads or writes the `general` schema (backbone-rest's), so there is no foreign key to
-- general.application. A row is created by Mercury the first time a template is created for an application that has none
-- (folder = the application id), by `PUT /api/v1/templates/applications/{applicationId}/folder`, or by the optional import of
-- `umdc.templates.application-folders` at startup. An application without a row keeps its templates at the root of the storage.

CREATE TABLE IF NOT EXISTS mercury.application_template_folders (
    application_id uuid        PRIMARY KEY,
    folder         varchar(64) NOT NULL,
    created_at     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT application_template_folders_folder_uk UNIQUE (folder)
);
