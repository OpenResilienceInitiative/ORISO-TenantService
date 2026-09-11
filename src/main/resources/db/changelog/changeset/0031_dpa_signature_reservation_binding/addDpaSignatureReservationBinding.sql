-- ORISO-TenantService#179: bind a public onboarding sign link to the RESERVATION that minted it,
-- not merely to the tenant-ID slot it points at. A tenant ID can be released and reserved again
-- within the link's 14-day lifetime; without this binding the old link would pass the
-- tenant/reservation guard and record a signature for the NEXT organisation to hold that ID.
-- Stores only the SHA-256 hash of the reservation token, never the token itself.
ALTER TABLE `tenantservice`.`tenant_dpa_signature`
    ADD COLUMN IF NOT EXISTS reservation_token_hash varchar(64) NULL;
