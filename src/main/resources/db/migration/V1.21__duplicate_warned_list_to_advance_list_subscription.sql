-- Add CROWN_ADVANCE_PDDA_LIST to subscription_list_type if it contains CROWN_WARNED_PDDA_LIST
UPDATE subscription_list_type
  SET list_type = list_type || '{CROWN_ADVANCE_PDDA_LIST}'
  WHERE ARRAY_TO_STRING(list_type, '||') LIKE '%CROWN_WARNED_PDDA_LIST%';
