DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_account_organization') THEN
    ALTER TABLE user_account
      ADD CONSTRAINT fk_user_account_organization
      FOREIGN KEY (organization_code) REFERENCES organization(organization_code);
  END IF;
END $$;