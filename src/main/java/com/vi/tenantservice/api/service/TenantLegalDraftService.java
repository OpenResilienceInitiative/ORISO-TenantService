package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.exception.SettingsUpdateConflictException;
import com.vi.tenantservice.api.model.*;
import com.vi.tenantservice.api.repository.TenantLegalDraftRepository;
import com.vi.tenantservice.api.validation.InputSanitizer;
import java.time.*;
import java.util.*;
import lombok.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantLegalDraftService {
  public static final String NEW_REVISION = "new";

  private final @NonNull TenantLegalDraftRepository repository;
  private final @NonNull InputSanitizer inputSanitizer;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Transactional(readOnly = true)
  public Optional<TenantLegalDraftEntity> get(Long tenantId, TenantLegalDraftKind kind) {
    return repository.findByTenantIdAndKind(tenantId, kind);
  }

  @Transactional
  public TenantLegalDraftEntity save(
      Long tenantId, TenantLegalDraftKind kind, Map<String, String> content, String revision) {
    var existing = repository.findByTenantIdAndKind(tenantId, kind);
    String actual = existing.map(this::revision).orElse(NEW_REVISION);
    if (!actual.equals(revision)) throw new SettingsUpdateConflictException(actual, revision);
    var entity =
        existing.orElseGet(
            () -> TenantLegalDraftEntity.builder().tenantId(tenantId).kind(kind).build());
    entity.setContent(write(sanitize(content)));
    entity.setUpdateDate(LocalDateTime.now(ZoneOffset.UTC));
    try {
      return repository.saveAndFlush(entity);
    } catch (OptimisticLockingFailureException | DataIntegrityViolationException exception) {
      throw new SettingsUpdateConflictException(exception);
    }
  }

  @Transactional
  public void delete(Long tenantId, TenantLegalDraftKind kind, String revision) {
    var entity =
        repository
            .findByTenantIdAndKind(tenantId, kind)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal draft not found"));
    String actual = revision(entity);
    if (!actual.equals(revision)) throw new SettingsUpdateConflictException(actual, revision);
    try {
      repository.delete(entity);
      repository.flush();
    } catch (OptimisticLockingFailureException | DataIntegrityViolationException exception) {
      throw new SettingsUpdateConflictException(exception);
    }
  }

  public String revision(TenantLegalDraftEntity draft) {
    if (draft.getId() == null || draft.getVersion() == null) {
      throw new IllegalStateException("A persisted legal draft is required for a revision token");
    }
    return draft.getId() + ":" + draft.getVersion();
  }

  public Map<String, String> content(TenantLegalDraftEntity draft) {
    try {
      return objectMapper.readValue(
          draft.getContent(), new TypeReference<LinkedHashMap<String, String>>() {});
    } catch (Exception e) {
      throw new IllegalStateException("Could not read legal draft content", e);
    }
  }

  private String write(Map<String, String> content) {
    try {
      return objectMapper.writeValueAsString(content == null ? Map.of() : content);
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not save legal draft content", e);
    }
  }

  private Map<String, String> sanitize(Map<String, String> content) {
    if (content == null) return Map.of();
    Map<String, String> sanitized = new LinkedHashMap<>();
    content.forEach(
        (language, html) ->
            sanitized.put(
                language,
                inputSanitizer.sanitizeAllowingFormattingAndLinks(html == null ? "" : html)));
    return sanitized;
  }
}
