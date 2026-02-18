ALTER TABLE `tenantservice`.`tenant`
    MODIFY COLUMN `id` bigint(21) NOT NULL DEFAULT (next value for `tenantservice`.`sequence_tenant`);
