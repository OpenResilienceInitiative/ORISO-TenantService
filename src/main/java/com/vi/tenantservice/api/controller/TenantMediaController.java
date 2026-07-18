package com.vi.tenantservice.api.controller;

import com.vi.tenantservice.api.service.TenantMediaService;
import com.vi.tenantservice.generated.api.controller.MediaApi;
import io.swagger.annotations.Api;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves stored tenant media publicly: legal-text images are read by anonymous visitors
 * (imprint/privacy pages render without login). Ids are UUIDs, content is immutable, so aggressive
 * caching is safe.
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "tenant-controller")
public class TenantMediaController implements MediaApi {

  private final @NonNull TenantMediaService tenantMediaService;

  @Override
  public ResponseEntity<Resource> getTenantMedia(String mediaId) {
    return tenantMediaService
        .findMedia(mediaId)
        .map(
            media ->
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(media.getContentType()))
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                    .body((Resource) new ByteArrayResource(media.getContent())))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
