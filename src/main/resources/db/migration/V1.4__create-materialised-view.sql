--
-- Create the Materialized view for the PI User Table
--
CREATE MATERIALIZED VIEW IF NOT EXISTS sdp_mat_view_pi_user AS
SELECT pi_user.user_id,
       pi_user.provenance_user_id,
       pi_user.user_provenance,
       pi_user.roles
FROM pi_user;
