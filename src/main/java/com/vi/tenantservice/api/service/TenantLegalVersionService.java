package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.api.model.TenantLegalDraftKind;
import com.vi.tenantservice.api.model.TenantLegalTextVersionEntity;
import com.vi.tenantservice.api.repository.TenantLegalTextVersionRepository;
import com.vi.tenantservice.config.security.AuthorisationService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publication history of the Träger and platform imprint and privacy policy (ORISO-Admin#270),
 * mirroring AgencyService's {@code LegalTextVersionService}. On the tenant a save is the publish,
 * so a version is written whenever a save changes the stored wording, and only then.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantLegalVersionService {

  private final @NonNull TenantLegalTextVersionRepository repository;
  private final @NonNull AuthorisationService authorisationService;

  /** The published wordings before a save, captured before the converter mutates the entity. */
  public record PublishedLegalTexts(String impressum, String privacy) {
    public static final PublishedLegalTexts NONE = new PublishedLegalTexts(null, null);

    public static PublishedLegalTexts of(TenantEntity tenant) {
      return new PublishedLegalTexts(tenant.getContentImpressum(), tenant.getContentPrivacy());
    }
  }

  /** Runs the tenant save and its history writes in one transaction, so neither lands alone. */
  @Transactional
  public <T extends TenantEntity> T saveRecordingPublications(
      PublishedLegalTexts before, Supplier<T> save) {
    T saved = save.get();
    record(
        saved.getId(),
        TenantLegalDraftKind.IMPRINT,
        before.impressum(),
        saved.getContentImpressum());
    record(
        saved.getId(), TenantLegalDraftKind.PRIVACY, before.privacy(), saved.getContentPrivacy());
    return saved;
  }

  @Transactional(readOnly = true)
  public List<TenantLegalTextVersionEntity> list(Long tenantId, TenantLegalDraftKind kind) {
    return repository.findByTenantIdAndKindOrderByPublishedAtDescIdDesc(tenantId, kind);
  }

  private void record(Long tenantId, TenantLegalDraftKind kind, String before, String after) {
    if (tenantId == null || Objects.equals(before, after)) {
      return;
    }
    List<TenantLegalTextVersionEntity> open = repository.findLockedOpen(tenantId, kind);
    if (isBlank(after)) {
      // A withdrawn text must not stay "in force" in the history.
      if (!open.isEmpty()) {
        LocalDateTime now = now();
        open.forEach(version -> version.setSupersededAt(now));
        repository.saveAll(open);
      }
      return;
    }
    if (open.stream().anyMatch(version -> after.equals(version.getContent()))) {
      return;
    }
    LocalDateTime now = now();
    open.forEach(version -> version.setSupersededAt(now));
    if (!open.isEmpty()) repository.saveAll(open);
    repository.save(
        TenantLegalTextVersionEntity.builder()
            .tenantId(tenantId)
            .kind(kind)
            .content(after)
            .publishedAt(now)
            .publishedBy(publisher())
            .build());
  }

  private String publisher() {
    try {
      return authorisationService.getUserId();
    } catch (RuntimeException e) {
      log.debug("No authenticated publisher for a legal text version", e);
      return null;
    }
  }

  /** Second precision: MariaDB TIMESTAMP drops the rest, and the wire format has none. */
  private static LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
  }

  /** The converter stores an absent language map as "{}" or "null"; neither is a wording. */
  private static boolean isBlank(String content) {
    if (content == null) return true;
    String trimmed = content.trim();
    return trimmed.isEmpty() || "{}".equals(trimmed) || "null".equals(trimmed);
  }
}
