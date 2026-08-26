--
-- Create pi_user_archived table if it doesn't exist.
--
CREATE TABLE IF NOT EXISTS pi_user_archived (
      user_id UUID PRIMARY KEY,
      user_provenance VARCHAR(255),
      provenance_user_id VARCHAR(255),
      email VARCHAR(255),
      roles VARCHAR(255),
      last_signed_in_date TIMESTAMP,
      archived_date TIMESTAMP
);
