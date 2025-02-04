--
-- Create the table if doesn't exist. Only used in test DBs
--
CREATE TABLE IF NOT EXISTS subscription (
    id uuid NOT NULL PRIMARY KEY,
    case_name varchar(255),
    case_number varchar(255),
    channel varchar(255),
    court_name varchar(255)
    created_date timestamp,
    location_name varchar(255),
    search_type varchar(255),
    search_value varchar(255),
    urn varchar(255),
    user_id varchar(255),
    list_type text[],
    last_updated_date timestamp,
    party_names varchar(255)
);
