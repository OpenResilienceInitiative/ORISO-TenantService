ALTER TABLE `tenantservice`.`tenant_dpa_signature` ADD COLUMN IF NOT EXISTS token_hash varchar(64) NULL;
ALTER TABLE `tenantservice`.`tenant_dpa_signature` ADD COLUMN IF NOT EXISTS token_expires_at datetime NULL;
