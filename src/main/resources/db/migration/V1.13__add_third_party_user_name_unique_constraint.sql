ALTER TABLE api_user
  DROP CONSTRAINT IF EXISTS unique_name_constraint,
  ADD CONSTRAINT unique_name_constraint UNIQUE (name);
