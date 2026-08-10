-- Remove all obsolete CASE_ID and CASE_URN subscriptions
DELETE FROM subscription
WHERE search_type = 'CASE_ID' OR search_type = 'CASE_URN';

-- Drop the urn and party_names column
ALTER TABLE subscription
  DROP COLUMN IF EXISTS urn,
  DROP COLUMN IF EXISTS party_names;

-- Update the search_type check constraint so CASE_ID and CASE_URN search type are not included
ALTER TABLE subscription
DROP CONSTRAINT IF EXISTS subscription_search_type_check,
  ADD CONSTRAINT subscription_search_type_check CHECK (search_type IN (
    'LOCATION_ID', 'LIST_TYPE', 'CASE_NUMBER', 'CASE_NAME'
  ));
