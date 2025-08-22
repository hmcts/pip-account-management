--
-- Remove 'CIC_DAILY_HEARING_LIST' from subscription_list_type
-- as the list type is being removed.
--
UPDATE subscription_list_type
SET list_type = ARRAY_REMOVE(list_type, 'CIC_DAILY_HEARING_LIST')
WHERE 'CIC_DAILY_HEARING_LIST' = ANY(list_type);
