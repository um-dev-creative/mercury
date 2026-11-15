-- ZeroBound (and the other applications) send a 6-digit verification code, but the database check chk_verification_code_length rejected it
-- ("new row for relation verification_codes violates check constraint chk_verification_code_length"), so no code was ever stored. The column is
-- varchar(9): the check now accepts 4 to 9 characters. NOT VALID: rows that already exist are not re-checked (the migration cannot fail on them),
-- every new row is. Re-runnable.
ALTER TABLE mercury.verification_codes DROP CONSTRAINT IF EXISTS chk_verification_code_length;
ALTER TABLE mercury.verification_codes
    ADD CONSTRAINT chk_verification_code_length CHECK (char_length(verification_code) BETWEEN 4 AND 9) NOT VALID;
