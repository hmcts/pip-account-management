--
-- Add new column for last sign in date if it doesn't exist
--
ALTER TABLE pi_user
    ADD COLUMN IF NOT EXISTS last_signed_in_date timestamp;

--
-- Set the last_sign_in_date to the created_date for:
-- - AAD admin users
-- - All IDAM users
--
UPDATE pi_user
SET last_signed_in_date = created_date
WHERE (user_provenance = 'PI_AAD' AND roles <> 'VERIFIED')
   OR user_provenance = 'CFT_IDAM'
   OR user_provenance = 'CRIME_IDAM';
