CREATE TABLE `tenantservice`.`tenant_media` (
  `id` varchar(36) NOT NULL,
  `tenant_id` bigint(21) NOT NULL,
  `content` mediumblob NOT NULL,
  `content_type` varchar(64) NOT NULL,
  `file_name` varchar(255) NULL,
  `content_size` int NOT NULL,
  `create_date` datetime NOT NULL DEFAULT (UTC_TIMESTAMP),
  PRIMARY KEY (`id`),
  KEY `idx_tenant_media_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
