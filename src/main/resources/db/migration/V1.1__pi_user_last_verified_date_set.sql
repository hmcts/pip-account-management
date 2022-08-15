--
-- Create the table if doesn't exist for test DBs
--
CREATE TABLE IF NOT EXISTS pi_user (
    user_id uuid NOT NULL PRIMARY KEY,
    email varchar(255),
    provenance_user_id varchar(255) NOT NULL,
    roles varchar(255),
    user_provenance varchar(255),
    created_date timestamp,
    last_verified_date timestamp
  );

--
-- If the table already existed without the last_verified_date column, add the column
--
ALTER TABLE pi_user
    ADD COLUMN IF NOT EXISTS last_verified_date timestamp;

--
-- Set the created date to 12th July 2pm for any users that do not have a created date
--
UPDATE pi_user
SET created_date = '2022-07-12 14:00:00'
WHERE created_date IS NULL;

--
-- Sets the last_verified_date to the created_date for verified users
--
UPDATE pi_user
SET last_verified_date = created_date
WHERE roles = 'VERIFIED';
