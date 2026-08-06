CREATE TABLE `tenantservice`.`tenant_id_reservation` (
  `tenant_id` bigint(21) NOT NULL,
  `status` varchar(16) NOT NULL,
  `token` varchar(36) NOT NULL,
  `reserved_by` varchar(255) NULL,
  `create_date` datetime NOT NULL DEFAULT (UTC_TIMESTAMP),
  `update_date` datetime NOT NULL DEFAULT (UTC_TIMESTAMP),
  PRIMARY KEY (`tenant_id`),
  UNIQUE KEY `uq_tenant_id_reservation_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
