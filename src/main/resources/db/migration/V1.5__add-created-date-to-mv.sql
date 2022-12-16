--
-- Add Created Date to the PI User Materialised View
--
DROP MATERIALIZED VIEW IF EXISTS sdp_mat_view_pi_user;

CREATE MATERIALIZED VIEW IF NOT EXISTS sdp_mat_view_pi_user AS
SELECT pi_user.user_id,
       pi_user.provenance_user_id,
       pi_user.user_provenance,
       pi_user.roles,
       pi_user.created_date
FROM pi_user;
