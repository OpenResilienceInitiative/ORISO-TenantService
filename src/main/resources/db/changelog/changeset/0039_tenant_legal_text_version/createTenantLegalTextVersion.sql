CREATE SEQUENCE IF NOT EXISTS sequence_tenant_legal_text_version START WITH 1 INCREMENT BY 1;
-- tenant_id 0 is the platform operator. No foreign key: the history of a deleted Träger stays
-- answerable, and the platform row is not guaranteed to exist in every environment.
CREATE TABLE IF NOT EXISTS tenant_legal_text_version (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  content LONGTEXT NOT NULL,
  published_at TIMESTAMP NOT NULL,
  published_by VARCHAR(255) NULL,
  superseded_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS idx_tenant_legal_text_version_history
  ON tenant_legal_text_version (tenant_id, kind, published_at, id);
CREATE INDEX IF NOT EXISTS idx_tenant_legal_text_version_current
  ON tenant_legal_text_version (tenant_id, kind, superseded_at);
