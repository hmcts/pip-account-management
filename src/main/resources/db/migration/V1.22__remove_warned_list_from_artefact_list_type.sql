--
-- Remove old CROWN_WARNED_PDDA_LIST from the subscription_list_type table
--
UPDATE subscription_list_type
  SET list_type = ARRAY_REMOVE(list_type, 'CROWN_WARNED_PDDA_LIST')
  WHERE ARRAY_TO_STRING(list_type, '||') LIKE '%CROWN_WARNED_PDDA_LIST%';
