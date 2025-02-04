--
-- If the table already exists without the new columns, add them in
--
ALTER TABLE subscription
  ADD COLUMN IF NOT EXISTS party_names varchar(255);
