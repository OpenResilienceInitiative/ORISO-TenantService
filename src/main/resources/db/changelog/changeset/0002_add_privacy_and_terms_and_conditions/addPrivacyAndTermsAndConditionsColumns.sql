ALTER TABLE `tenantservice`.`tenant`
ADD COLUMN `content_privacy` longtext NULL AFTER `content_claim`;

ALTER TABLE `tenantservice`.`tenant`
ADD COLUMN `content_termsandconditions` longtext NULL AFTER `content_privacy`;
