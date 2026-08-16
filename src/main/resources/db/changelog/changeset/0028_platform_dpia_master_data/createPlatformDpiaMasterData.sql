CREATE SEQUENCE sequence_platform_dpia_master_data
INCREMENT BY 1
MINVALUE = 1
NOMAXVALUE
START WITH 1
CACHE 0;

CREATE TABLE platform_dpia_master_data (
  id BIGINT NOT NULL,
  operator_legal_name VARCHAR(255) NULL,
  operator_short_name VARCHAR(255) NULL,
  operator_address VARCHAR(512) NULL,
  operator_contact_email VARCHAR(255) NULL,
  operator_contact_phone VARCHAR(64) NULL,
  operator_dpo_name VARCHAR(255) NULL,
  operator_department VARCHAR(255) NULL,
  operator_responsible_person VARCHAR(255) NULL,
  supervisory_legal_framework VARCHAR(16) NULL,
  supervisory_authority_name VARCHAR(255) NULL,
  supervisory_authority_address VARCHAR(512) NULL,
  supervisory_authority_email VARCHAR(255) NULL,
  document_date DATE NULL,
  document_next_review_date DATE NULL,
  key_figure_tenants BIGINT NULL,
  key_figure_tenants_as_of DATE NULL,
  key_figure_counselling_centres BIGINT NULL,
  key_figure_counselling_centres_as_of DATE NULL,
  key_figure_active_counsellors BIGINT NULL,
  key_figure_active_counsellors_as_of DATE NULL,
  key_figure_registered_clients BIGINT NULL,
  key_figure_registered_clients_as_of DATE NULL,
  update_date DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP),
  PRIMARY KEY (id)
);
