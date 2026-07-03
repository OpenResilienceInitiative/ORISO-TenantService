ALTER TABLE `tenantservice`.`tenant_dpa_signature` ADD COLUMN IF NOT EXISTS signer_email varchar(255) NULL;
ALTER TABLE `tenantservice`.`tenant_dpa_signature` ADD COLUMN IF NOT EXISTS signer_organisation varchar(255) NULL;
ALTER TABLE `tenantservice`.`tenant_dpa_signature` ADD COLUMN IF NOT EXISTS forwarded_by_user_id varchar(64) NULL;
ALTER TABLE `tenantservice`.`tenant_dpa_signature` ADD COLUMN IF NOT EXISTS source varchar(64) NULL;
