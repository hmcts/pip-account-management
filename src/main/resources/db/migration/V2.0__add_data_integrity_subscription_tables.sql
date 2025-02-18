ALTER TABLE subscription
  ADD CONSTRAINT fk_user_id FOREIGN KEY (user_id)
    REFERENCES pi_user (user_id);


ALTER TABLE subscription_list_type
  ADD CONSTRAINT fk_user_id FOREIGN KEY (user_id)
    REFERENCES pi_user (user_id);
