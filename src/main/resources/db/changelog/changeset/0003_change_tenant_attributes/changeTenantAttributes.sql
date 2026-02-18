ALTER TABLE `tenant`
CHANGE `name` `name` varchar(40) COLLATE 'utf8_unicode_ci' NOT NULL AFTER `id`,
CHANGE `content_claim` `content_claim` varchar(40) COLLATE 'utf8_unicode_ci' NULL AFTER `content_impressum`;