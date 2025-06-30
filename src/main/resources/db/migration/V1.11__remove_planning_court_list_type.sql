--
-- Remove 'PLANNING_COURT_DAILY_CAUSE_LIST' from subscription_list_type
-- as the list type is being removed.
--
UPDATE subscription_list_type
SET list_type = ARRAY_REMOVE(list_type, 'PLANNING_COURT_DAILY_CAUSE_LIST')
WHERE 'PLANNING_COURT_DAILY_CAUSE_LIST' = ANY(list_type);
