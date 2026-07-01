CREATE SEQUENCE IF NOT EXISTS `tenantservice`.`SEQUENCE_TENANT_DPA_VERSION` START WITH 100000 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS `tenantservice`.`tenant_dpa_version` (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    content longtext NULL,
    activation_date datetime NOT NULL,
    create_date datetime NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tenant_dpa_version_tenant (tenant_id),
    CONSTRAINT fk_tenant_dpa_version_tenant FOREIGN KEY (tenant_id)
        REFERENCES `tenantservice`.`tenant` (id) ON DELETE CASCADE
);
