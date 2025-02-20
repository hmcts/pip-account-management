--
-- Create the table if doesn't exist. Only used in test DBs
--
CREATE TABLE IF NOT EXISTS subscription_list_type (
    id uuid NOT NULL PRIMARY KEY,
    list_language text[],
    list_type text[],
    user_id character varying(255) NOT NULL
);
