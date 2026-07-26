package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.model.TenantMediaEntity;
import com.vi.tenantservice.api.repository.TenantMediaRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Stores and serves tenant-owned editor images. Content type is derived from magic bytes only — the
 * client-supplied file name and content type are untrusted. SVG is deliberately unsupported (script
 * content). Fail-closed: anything unrecognised is rejected.
 */
@Service
@RequiredArgsConstructor
public class TenantMediaService {

  public static final int MAX_MEDIA_BYTES = 2 * 1024 * 1024;

  private final @NonNull TenantMediaRepository tenantMediaRepository;

  public TenantMediaEntity upload(byte[] content, String fileName, Long tenantId) {
    if (content == null || content.length > MAX_MEDIA_BYTES) {
      throw new MediaSizeLimitExceededException(
          "media exceeds the limit of " + MAX_MEDIA_BYTES + " bytes");
    }
    var contentType = detectContentType(content);
    var entity =
        TenantMediaEntity.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .content(content)
            .contentType(contentType)
            .fileName(fileName)
            .contentSize(content.length)
            .createDate(LocalDateTime.now(ZoneOffset.UTC))
            .build();
    return tenantMediaRepository.save(entity);
  }

  public Optional<TenantMediaEntity> findMedia(String id) {
    return tenantMediaRepository.findById(id);
  }

  private String detectContentType(byte[] content) {
    if (matches(content, 0, (byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G')) {
      return "image/png";
    }
    if (matches(content, 0, (byte) 0xFF, (byte) 0xD8, (byte) 0xFF)) {
      return "image/jpeg";
    }
    if (matches(content, 0, (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F')
        && matches(content, 8, (byte) 'W', (byte) 'E', (byte) 'B', (byte) 'P')) {
      return "image/webp";
    }
    throw new UnsupportedMediaContentException("only PNG, JPEG and WebP images are supported");
  }

  private boolean matches(byte[] content, int offset, byte... magic) {
    if (content.length < offset + magic.length) {
      return false;
    }
    for (int i = 0; i < magic.length; i++) {
      if (content[offset + i] != magic[i]) {
        return false;
      }
    }
    return true;
  }
}
