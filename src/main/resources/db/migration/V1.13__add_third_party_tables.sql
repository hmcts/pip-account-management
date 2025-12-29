CREATE TABLE IF NOT EXISTS api_user (
  user_id uuid NOT NULL PRIMARY KEY,
  name text,
  created_date timestamp
);

CREATE TABLE IF NOT EXISTS api_subscription (
  id uuid NOT NULL PRIMARY KEY,
  user_id uuid,
  CONSTRAINT fk_user_id
    FOREIGN KEY (user_id)
      REFERENCES api_user (user_id),
  list_type text,
  sensitivity text,
  created_date timestamp,
  last_updated_date timestamp
);

CREATE TABLE IF NOT EXISTS api_oauth_configuration (
  id uuid NOT NULL PRIMARY KEY,
  user_id uuid,
  CONSTRAINT fk_user_id
    FOREIGN KEY (user_id)
      REFERENCES api_user (user_id),
  destination_url text,
  token_url text,
  client_id_key text,
  client_secret_key text,
  scope_key text,
  created_date timestamp,
  last_updated_date timestamp
);
