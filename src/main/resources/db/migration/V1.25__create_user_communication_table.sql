CREATE TABLE IF NOT EXISTS user_communication (
  id uuid NOT NULL PRIMARY KEY,
  audience_type text,
  message text,
  welsh_message text,
  user_provenances text[],
  user_roles text[],
  sub_jurisdictions text[],
  venues integer[],
  created_date timestamp,
  last_updated_date timestamp
);
