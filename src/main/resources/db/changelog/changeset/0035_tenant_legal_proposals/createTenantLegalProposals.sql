ALTER TABLE tenant_legal_draft
  ADD COLUMN origin_proposal_id BIGINT NULL,
  ADD COLUMN origin_distribution_id VARCHAR(36) NULL,
  ADD COLUMN origin_source_revision VARCHAR(64) NULL,
  ADD COLUMN origin_source_updated_at TIMESTAMP NULL,
  ADD COLUMN origin_shared_by VARCHAR(255) NULL;

CREATE TABLE tenant_legal_proposal_distribution (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  request_key VARCHAR(128) NOT NULL,
  kind VARCHAR(16) NOT NULL,
  audience VARCHAR(16) NOT NULL,
  source_draft_id BIGINT NOT NULL,
  source_draft_version BIGINT NOT NULL,
  source_updated_at TIMESTAMP NOT NULL,
  request_fingerprint VARCHAR(64) NOT NULL,
  recipient_ids TEXT NOT NULL,
  created_by VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uq_tenant_legal_proposal_distribution_request UNIQUE (request_key)
);

CREATE SEQUENCE IF NOT EXISTS sequence_tenant_legal_proposal START WITH 1 INCREMENT BY 1;
CREATE TABLE tenant_legal_proposal (
  id BIGINT NOT NULL PRIMARY KEY,
  version BIGINT NOT NULL,
  recipient_tenant_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  distribution_id VARCHAR(36) NOT NULL,
  audience VARCHAR(16) NOT NULL,
  source_draft_id BIGINT NOT NULL,
  source_draft_version BIGINT NOT NULL,
  source_updated_at TIMESTAMP NOT NULL,
  content TEXT NOT NULL,
  privacy_consent TEXT NULL,
  status VARCHAR(16) NOT NULL,
  created_by VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  decided_by VARCHAR(255) NULL,
  decided_at TIMESTAMP NULL,
  superseded_by_proposal_id BIGINT NULL,
  superseded_at TIMESTAMP NULL,
  CONSTRAINT uq_tenant_legal_proposal_source_recipient
    UNIQUE (source_draft_id, source_draft_version, recipient_tenant_id),
  CONSTRAINT fk_tenant_legal_proposal_tenant
    FOREIGN KEY (recipient_tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
  CONSTRAINT fk_tenant_legal_proposal_distribution
    FOREIGN KEY (distribution_id) REFERENCES tenant_legal_proposal_distribution(id)
);
CREATE INDEX idx_tenant_legal_proposal_recipient_history
  ON tenant_legal_proposal (recipient_tenant_id, created_at, id);
CREATE INDEX idx_tenant_legal_proposal_recipient_state
  ON tenant_legal_proposal (recipient_tenant_id, kind, status, id);

CREATE SEQUENCE IF NOT EXISTS sequence_tenant_legal_proposal_delivery START WITH 1 INCREMENT BY 1;
CREATE TABLE tenant_legal_proposal_delivery (
  id BIGINT NOT NULL PRIMARY KEY,
  distribution_id VARCHAR(36) NOT NULL,
  proposal_id BIGINT NOT NULL,
  CONSTRAINT uq_tenant_legal_proposal_delivery UNIQUE (distribution_id, proposal_id),
  CONSTRAINT fk_tenant_legal_proposal_delivery_distribution
    FOREIGN KEY (distribution_id) REFERENCES tenant_legal_proposal_distribution(id) ON DELETE CASCADE,
  CONSTRAINT fk_tenant_legal_proposal_delivery_proposal
    FOREIGN KEY (proposal_id) REFERENCES tenant_legal_proposal(id) ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS sequence_tenant_legal_draft_archive START WITH 1 INCREMENT BY 1;
CREATE TABLE tenant_legal_draft_archive (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  draft_row_id BIGINT NOT NULL,
  draft_revision VARCHAR(64) NOT NULL,
  content TEXT NOT NULL,
  privacy_consent TEXT NULL,
  draft_saved_at TIMESTAMP NOT NULL,
  origin_proposal_id BIGINT NULL,
  origin_distribution_id VARCHAR(36) NULL,
  origin_source_revision VARCHAR(64) NULL,
  origin_source_updated_at TIMESTAMP NULL,
  origin_shared_by VARCHAR(255) NULL,
  archived_by VARCHAR(255) NOT NULL,
  archived_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_tenant_legal_draft_archive_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);
CREATE INDEX idx_tenant_legal_draft_archive_history
  ON tenant_legal_draft_archive (tenant_id, kind, archived_at, id);
