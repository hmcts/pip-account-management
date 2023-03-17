CREATE TABLE IF NOT EXISTS audit_log (
    id uuid NOT NULL PRIMARY KEY,
    user_id varchar(255),
    user_email varchar(255),
    roles varchar(255),
    user_provenance varchar(255),
    action varchar(255),
    details varchar(255),
    timestamp timestamp
  );

ALTER TABLE audit_log
  ADD COLUMN IF NOT EXISTS roles varchar(255),
  ADD COLUMN IF NOT EXISTS user_provenance varchar(255);
