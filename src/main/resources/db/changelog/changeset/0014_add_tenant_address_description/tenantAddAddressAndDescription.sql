ALTER TABLE `tenantservice`.`tenant` ADD COLUMN IF NOT EXISTS address VARCHAR(255) NULL;
ALTER TABLE `tenantservice`.`tenant` ADD COLUMN IF NOT EXISTS description TEXT NULL;
