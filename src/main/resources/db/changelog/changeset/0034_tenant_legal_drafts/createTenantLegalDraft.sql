CREATE SEQUENCE IF NOT EXISTS sequence_tenant_legal_draft START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS tenant_legal_draft (
  id BIGINT NOT NULL PRIMARY KEY,
  version BIGINT NOT NULL,
  owner_key BIGINT NOT NULL,
  tenant_id BIGINT NULL,
  kind VARCHAR(16) NOT NULL,
  content LONGTEXT NOT NULL,
  privacy_consent LONGTEXT NULL,
  update_date TIMESTAMP NOT NULL,
  CONSTRAINT uq_tenant_legal_draft UNIQUE (owner_key, kind),
  CONSTRAINT chk_tenant_legal_draft_owner CHECK (owner_key = COALESCE(tenant_id, 0)),
  CONSTRAINT fk_tenant_legal_draft_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
