CREATE TABLE tenantservice.`tenant` (
  `id` bigint(21) NOT NULL,
  `name` varchar(36) COLLATE utf8_unicode_ci NOT NULL,
  `subdomain` varchar(255) NOT NULL,
  `licensing_allowed_users` int COLLATE utf8_unicode_ci,
  `theming_logo` longtext,
  `theming_favicon` longtext,
  `theming_primary_color` varchar(15),
  `theming_secondary_color` varchar(15),
  `content_impressum` longtext,
  `content_claim` varchar(1024),
  `create_date` datetime NOT NULL DEFAULT (UTC_TIMESTAMP),
  `update_date` datetime NOT NULL DEFAULT (UTC_TIMESTAMP),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

ALTER TABLE tenantservice.`tenant`
ADD CONSTRAINT unique_subdomain UNIQUE (subdomain);

CREATE SEQUENCE tenantservice.sequence_tenant
INCREMENT BY 1
MINVALUE = 0
NOMAXVALUE
START WITH 0
CACHE 0;
