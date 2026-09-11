-- ORISO-TenantService#179: sign links can now be created from the public onboarding context
-- BEFORE the tenant registration completes. The PENDING signature row is bound to the tenant id
-- the invite's reservation holds, so the referenced tenant row does not exist yet - the foreign
-- key must go. Referential hygiene is preserved at the application layer: tenant deletion
-- explicitly removes the tenant's signature rows (TenantServiceFacade), and reservation-bound
-- rows are only ever created against the authoritative tenant_id_reservation ledger.
ALTER TABLE `tenantservice`.`tenant_dpa_signature` DROP FOREIGN KEY fk_tenant_dpa_signature_tenant;
