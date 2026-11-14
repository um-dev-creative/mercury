-- The subject of an e-mail belongs to the template (edited from Iris, one per language version), not to the caller's code.
-- It may use the same ${placeholders} as the body. NULL = the template defines none and the caller must send one.
ALTER TABLE mercury.templates ADD COLUMN IF NOT EXISTS subject varchar(500);
