--
-- Replace the Courtel users' role to the new third party user provenance and role
--
UPDATE pi_user
SET roles = 'GENERAL_THIRD_PARTY',
    user_provenance = 'THIRD_PARTY'
WHERE roles = 'TECHNICAL';
