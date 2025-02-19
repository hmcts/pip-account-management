--
-- Creates the materialised view for subscriptions if it doesn't exist.
-- Search Value is populated with 'location name' only, excluding case details
--
CREATE MATERIALIZED VIEW IF NOT EXISTS sdp_mat_view_subscription AS
SELECT subscription.id,
       subscription.user_id,
       subscription.search_type,
       subscription.location_name AS search_value,
       subscription.channel,
       subscription.created_date,
       subscription.last_updated_date
FROM subscription;
