-- Seed data for TenantRepositoryTest.
-- The schema is created by Hibernate (ddl-auto=create-drop) from TenantEntity,
-- so this file only inserts the single "happylife" tenant (id = 1) that the
-- repository tests look up by id and by subdomain.
INSERT INTO tenant (id, name, subdomain, licensing_allowed_users, content_impressum, content_privacy, create_date, update_date, settings)
VALUES (1, 'Happylife Gmbh', 'happylife', 5, '{"de" : "Impressum"}', '{"de" : "Privacy"}', '2021-12-28 00:00:00', '2021-12-29 00:00:00', NULL);

-- Move the id sequence past the seeded row so save_Should_saveTenant does not
-- collide with the fixed id = 1 above (Hibernate creates SEQUENCE_TENANT from the
-- entity's @SequenceGenerator).
ALTER SEQUENCE SEQUENCE_TENANT RESTART WITH 100000;
