CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Extend the search_type check constraint to include the new CASE_NUMBER and CASE_NAME values
ALTER TABLE subscription
DROP CONSTRAINT IF EXISTS subscription_search_type_check,
    ADD CONSTRAINT subscription_search_type_check CHECK (search_type IN (
        'LOCATION_ID', 'CASE_ID', 'CASE_URN', 'LIST_TYPE', 'CASE_NUMBER', 'CASE_NAME'
    ));

-- Copy CASE_ID subscriptions to new CASE_NUMBER rows
INSERT INTO subscription (id, user_id, search_type, search_value, channel, created_date, case_number, case_name,
                          last_updated_date)
SELECT gen_random_uuid(),
       user_id,
       'CASE_NUMBER',
       search_value,
       channel,
       created_date,
       case_number,
       case_name,
       last_updated_date
FROM subscription
WHERE search_type = 'CASE_ID';

-- Copy CASE_URN subscriptions to new CASE_NUMBER rows, mapping the urn field to case_number
INSERT INTO subscription (id, user_id, search_type, search_value, channel, created_date, case_number, case_name,
                          last_updated_date)
SELECT gen_random_uuid(),
       user_id,
       'CASE_NUMBER',
       search_value,
       channel,
       created_date,
       urn,
       case_name,
       last_updated_date
FROM subscription
WHERE search_type = 'CASE_URN';
