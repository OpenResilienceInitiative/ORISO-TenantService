CREATE SEQUENCE IF NOT EXISTS `tenantservice`.`SEQUENCE_TENANT_DPA_SIGNATURE` START WITH 100000 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS `tenantservice`.`tenant_dpa_signature` (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    dpa_version datetime NULL,
    signer_name varchar(255) NULL,
    signer_position varchar(255) NULL,
    signer_is_member tinyint(1) NULL,
    lang varchar(10) NULL,
    signature_status varchar(20) NOT NULL DEFAULT 'PENDING',
    signed_at datetime NULL,
    create_date datetime NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tenant_dpa_signature_tenant (tenant_id)
);
