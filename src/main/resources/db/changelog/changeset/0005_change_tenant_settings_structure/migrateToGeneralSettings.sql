ALTER TABLE `tenantservice`.`tenant` ADD COLUMN `settings` VARCHAR(4000) NULL AFTER `settings_topics_in_registration_enabled`;
UPDATE `tenantservice`.`tenant` SET settings = "{'feature.topicsInRegistration.enabled': 'true'}" where settings_topics_in_registration_enabled = 1;
ALTER TABLE `tenantservice`.`tenant` DROP COLUMN settings_topics_in_registration_enabled;


