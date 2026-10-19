UPDATE `tenantservice`.`tenant`
SET
  `theming_primary_color` = CASE WHEN TRIM(`theming_primary_color`) = '' THEN NULL ELSE `theming_primary_color` END,
  `theming_secondary_color` = CASE WHEN TRIM(`theming_secondary_color`) = '' THEN NULL ELSE `theming_secondary_color` END,
  `theming_accent` = CASE WHEN TRIM(`theming_accent`) = '' THEN NULL ELSE `theming_accent` END,
  `theming_signal` = CASE WHEN TRIM(`theming_signal`) = '' THEN NULL ELSE `theming_signal` END
WHERE TRIM(`theming_primary_color`) = ''
   OR TRIM(`theming_secondary_color`) = ''
   OR TRIM(`theming_accent`) = ''
   OR TRIM(`theming_signal`) = '';
