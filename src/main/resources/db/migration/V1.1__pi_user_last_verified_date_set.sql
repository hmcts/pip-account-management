--  This script sets the last_verified_date as either the
--  created date if it is not null or
--  the current timestamp when the script is run
UPDATE pi_user
SET last_verified_date =
      CASE WHEN created_date IS NOT NULL THEN created_date
           ELSE CURRENT_TIMESTAMP
        END;
