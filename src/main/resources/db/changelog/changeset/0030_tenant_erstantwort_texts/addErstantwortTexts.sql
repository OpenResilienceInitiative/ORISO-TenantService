ALTER TABLE `tenantservice`.`tenant`
  ADD COLUMN `content_erstantwort_greeting` TEXT NULL,
  ADD COLUMN `content_erstantwort_who_reads_along` TEXT NULL,
  ADD COLUMN `content_erstantwort_emergency_addition` TEXT NULL,
  ADD COLUMN `content_erstantwort_free_notice` TEXT NULL,
  ADD COLUMN `content_erstantwort_closing` TEXT NULL,
  ADD COLUMN `erstantwort_response_deadline_days` INT NULL;
