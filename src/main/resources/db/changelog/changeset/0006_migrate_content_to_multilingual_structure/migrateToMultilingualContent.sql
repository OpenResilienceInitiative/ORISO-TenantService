ALTER TABLE `tenant`
MODIFY `content_claim` varchar(1024) COLLATE 'utf8_unicode_ci' NULL;

UPDATE `tenantservice`.`tenant`  SET content_privacy = REPLACE( content_privacy ,'"',"'");
UPDATE `tenantservice`.`tenant`  SET content_termsandconditions = REPLACE( content_termsandconditions ,'"',"'");
UPDATE `tenantservice`.`tenant`  SET content_impressum = REPLACE( content_impressum ,'"',"'");
UPDATE `tenantservice`.`tenant`  SET content_claim = REPLACE( content_claim ,'"',"'");

UPDATE `tenantservice`.`tenant` SET content_privacy = concat('{"de": "', content_privacy, '"}');
UPDATE `tenantservice`.`tenant` SET content_termsandconditions = concat('{"de": "', content_termsandconditions, '"}');
UPDATE `tenantservice`.`tenant` SET content_impressum = concat('{"de": "', content_impressum, '"}');
UPDATE `tenantservice`.`tenant` SET content_claim = concat('{"de": "', content_claim, '"}');



