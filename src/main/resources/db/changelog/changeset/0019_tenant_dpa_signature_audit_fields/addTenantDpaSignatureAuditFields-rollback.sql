ALTER TABLE `tenantservice`.`tenant_dpa_signature` DROP COLUMN IF EXISTS source;
ALTER TABLE `tenantservice`.`tenant_dpa_signature` DROP COLUMN IF EXISTS forwarded_by_user_id;
ALTER TABLE `tenantservice`.`tenant_dpa_signature` DROP COLUMN IF EXISTS signer_organisation;
ALTER TABLE `tenantservice`.`tenant_dpa_signature` DROP COLUMN IF EXISTS signer_email;
