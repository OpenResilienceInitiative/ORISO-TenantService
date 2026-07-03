# Machine-translation metadata convention (`__meta` keys)

Legal content (e.g. the tenant DPA, `tenant.content_dpa`) is stored as a JSON map
`language -> HTML` in an existing LONGTEXT column. To mark a language as *machine translated*
we store a parallel metadata key **inside the same map** — no schema change needed:

```json
{
  "de": "<p>Original German text…</p>",
  "en": "<p>Machine translated English text…</p>",
  "en__meta": "{\"mt\":true,\"src\":\"de\",\"at\":\"2026-07-03T10:15:30Z\"}"
}
```

## Rules

- The meta key for language `<lang>` is `<lang>__meta` (see `TranslationMetaUtil`).
- The meta value is a **strict JSON object** with only these fields:
  - `mt` (boolean, required) — `true` = the language content is machine translated
  - `src` (string) — the source language the translation was made from
  - `at` (string) — ISO timestamp of the translation
- Anything else (unknown fields, wrong types, malformed JSON) is rejected with HTTP 400
  on publish.
- Meta values are **not HTML-sanitized** (they are not HTML); all other map values go through
  the OWASP sanitizer as before.
- **A manual edit clears the machine-translated tag:** when a publish changes the HTML of a
  language but merely resends the previously stored meta unchanged, that meta is removed.
  A changed/new meta together with new content (a fresh machine translation) is stored.
  A meta without content for its language is dropped.
- Consumers that only want the content can use `TranslationMetaUtil.stripMetaKeys(map)`.

## Where it applies

- `PUT /tenantadmin/{id}/dpa` (DPA publish) accepts and stores meta keys per the rules above.
- `POST /tenantadmin/translate` produces the translations; the Admin UI adds the
  `<lang>__meta` entries when publishing machine-translated languages.

## Related endpoints

- `GET /tenantadmin/translation/keys` — masked provider API keys (super admin)
- `PUT /tenantadmin/translation/keys/{provider}` — set key for `openrouter` / `mistral`
  (super admin)
- `POST /tenantadmin/translate` — translate HTML texts, typed error codes
  (`TRANSLATION_NOT_CONFIGURED` 409; `TRANSLATION_KEY_INVALID`, `TRANSLATION_NO_CREDIT`,
  `TRANSLATION_RATE_LIMITED`, `TRANSLATION_PROVIDER_UNAVAILABLE` 502)
