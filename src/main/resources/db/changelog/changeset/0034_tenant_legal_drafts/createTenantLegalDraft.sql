CREATE SEQUENCE IF NOT EXISTS sequence_tenant_legal_draft START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS tenant_legal_draft (
  id BIGINT NOT NULL PRIMARY KEY,
  version BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  privacy_consent TEXT NULL,
  update_date TIMESTAMP NOT NULL,
  CONSTRAINT uq_tenant_legal_draft UNIQUE (tenant_id, kind),
  CONSTRAINT fk_tenant_legal_draft_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
