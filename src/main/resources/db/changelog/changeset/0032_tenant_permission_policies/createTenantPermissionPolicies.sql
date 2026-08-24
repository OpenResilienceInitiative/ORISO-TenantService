CREATE SEQUENCE IF NOT EXISTS sequence_tenant_permission_policy
INCREMENT BY 1
MINVALUE = 1
NOMAXVALUE
START WITH 1
CACHE 0;

CREATE TABLE IF NOT EXISTS tenant_permission_policy (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  policies LONGTEXT NOT NULL,
  case_handover_policies LONGTEXT NULL,
  update_date DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP),
  PRIMARY KEY (id),
  CONSTRAINT uq_tenant_permission_policy_tenant UNIQUE (tenant_id),
  CONSTRAINT fk_tenant_permission_policy_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);
