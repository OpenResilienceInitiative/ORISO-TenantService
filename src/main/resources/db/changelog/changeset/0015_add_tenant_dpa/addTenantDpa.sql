ALTER TABLE `tenantservice`.`tenant` ADD COLUMN IF NOT EXISTS content_dpa LONGTEXT NULL;
ALTER TABLE `tenantservice`.`tenant` ADD COLUMN IF NOT EXISTS dpa_activation_date DATETIME NULL;
