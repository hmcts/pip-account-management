DELETE
FROM subscription
WHERE user_id IN (
  SELECT tbls.user_id --THIS QUERY WILL RETURN ALL THE USERS WHICH DOES NOT EXISTS IN PI_USER TABLE
  FROM
    (
      SELECT DISTINCT user_id
      FROM subscription
    )tbls
      LEFT JOIN pi_user tblu
        ON tbls.user_id = tblu.user_id
  WHERE tblu.email IS NULL
)

DELETE
FROM subscription_list_type
WHERE user_id IN (
  SELECT tbls.user_id --THIS QUERY WILL RETURN ALL THE USERS WHICH DOES NOT EXISTS IN PI_USER TABLE
  FROM
    (
      SELECT DISTINCT user_id
      FROM subscription_list_type
    )tbls
      LEFT JOIN pi_user tblu
        ON tbls.user_id = tblu.user_id
  WHERE tblu.email IS NULL
)
