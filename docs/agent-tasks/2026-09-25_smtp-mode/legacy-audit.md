# Legacy tenant SMTP mode audit

Run this read-only inventory in each environment before assigning `smtpMode` to existing tenants. It reports IDs and classification hints, not SMTP values or passwords. Do not bulk-update from the query: confirm each tenant's intended transport with its owner. A complete legacy SMTP record is a candidate for `OWN`; a partial record needs correction. A record without own SMTP can be proposed for `PLATFORM` after its tenant owner confirms the routing decision.

```sql
SELECT id,
       CASE
         WHEN settings IS NULL THEN 'NO_SETTINGS'
         WHEN JSON_VALID(settings) = 0 THEN 'INVALID_JSON'
         ELSE 'VALID_JSON'
       END AS settings_state
FROM tenant
WHERE settings IS NULL OR JSON_VALID(settings) = 0;

SELECT id,
       CASE
         WHEN JSON_UNQUOTE(JSON_EXTRACT(settings, '$.smtpMode')) IN ('PLATFORM', 'OWN')
           THEN 'EXPLICIT_MODE'
         WHEN JSON_EXTRACT(settings, '$.smtp') IS NULL
           THEN 'NO_OWN_SMTP'
         WHEN JSON_UNQUOTE(JSON_EXTRACT(settings, '$.smtp.enabled')) = 'true'
              AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(settings, '$.smtp.host')), '') IS NOT NULL
              AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(settings, '$.smtp.username')), '') IS NOT NULL
              AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(settings, '$.smtp.password')), '') IS NOT NULL
              AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(settings, '$.smtp.from')), '') IS NOT NULL
              AND CAST(JSON_UNQUOTE(JSON_EXTRACT(settings, '$.smtp.port')) AS UNSIGNED)
                  BETWEEN 1 AND 65535
           THEN 'OWN_CANDIDATE_REVIEW'
         ELSE 'PARTIAL_OR_DISABLED_REVIEW'
       END AS mode_audit_class
FROM tenant
WHERE settings IS NOT NULL AND JSON_VALID(settings) = 1;
```

Record the environment, date, tenant IDs, owner decision and change receipt outside this repository. Do not copy settings JSON, credentials or recipient addresses into issues or logs. Legacy rows retain a null mode until explicitly classified; the delivery endpoint does not send through their stored SMTP in that state.
