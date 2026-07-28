CREATE SEQUENCE IF NOT EXISTS `tenantservice`.`SEQUENCE_TENANT_DPA_ADMIN_SIGNATURE` START WITH 100000 INCREMENT BY 1;

-- Append-only audit trail of authenticated tenant admins signing the tenant's currently
-- published DPA version (TEN-INV-U9, ORISO-TenantService#144). Rows are never updated or
-- deleted by the application; the unique key on (tenant_id, dpa_version) is the database-level
-- guarantee that signing an already-signed version has no duplicate effect even under
-- concurrent double-submits.
CREATE TABLE IF NOT EXISTS `tenantservice`.`tenant_dpa_admin_signature` (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    dpa_version datetime NOT NULL,
    signer_user_id varchar(255) NOT NULL,
    signer_username varchar(255) NULL,
    signer_name varchar(255) NULL,
    signer_position varchar(255) NULL,
    signer_email varchar(255) NULL,
    signer_organisation varchar(255) NULL,
    lang varchar(10) NULL,
    form_data text NOT NULL,
    signed_at datetime NOT NULL,
    create_date datetime NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tenant_dpa_admin_signature_version (tenant_id, dpa_version),
    KEY idx_tenant_dpa_admin_signature_tenant (tenant_id),
    CONSTRAINT fk_tenant_dpa_admin_signature_tenant FOREIGN KEY (tenant_id)
        REFERENCES `tenantservice`.`tenant` (id) ON DELETE CASCADE
);
