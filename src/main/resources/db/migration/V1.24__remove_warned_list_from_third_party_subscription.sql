DELETE FROM subscription
WHERE search_type = 'LIST_TYPE' AND search_value = 'CROWN_WARNED_PDDA_LIST';

DELETE FROM api_subscription
WHERE list_type = 'CROWN_WARNED_PDDA_LIST';
