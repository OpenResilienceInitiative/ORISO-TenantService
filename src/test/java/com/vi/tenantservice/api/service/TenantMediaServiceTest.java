package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vi.tenantservice.api.model.TenantMediaEntity;
import com.vi.tenantservice.api.repository.TenantMediaRepository;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantMediaServiceTest {

  private static final long TENANT_ID = 5L;

  @Mock TenantMediaRepository tenantMediaRepository;

  @InjectMocks TenantMediaService tenantMediaService;

  private static byte[] pngBytes() {
    var content = new byte[64];
    byte[] magic = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, content, 0, magic.length);
    return content;
  }

  private static byte[] jpegBytes() {
    var content = new byte[64];
    byte[] magic = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    System.arraycopy(magic, 0, content, 0, magic.length);
    return content;
  }

  private static byte[] webpBytes() {
    var content = new byte[64];
    byte[] riff = {'R', 'I', 'F', 'F', 0x10, 0x10, 0x10, 0x10, 'W', 'E', 'B', 'P'};
    System.arraycopy(riff, 0, content, 0, riff.length);
    return content;
  }

  @Test
  void upload_should_storePngWithDetectedContentTypeAndTenantScope() {
    when(tenantMediaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var stored = tenantMediaService.upload(pngBytes(), "logo.png", TENANT_ID);

    var captor = ArgumentCaptor.forClass(TenantMediaEntity.class);
    verify(tenantMediaRepository).save(captor.capture());
    assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
    assertThat(captor.getValue().getContentType()).isEqualTo("image/png");
    assertThat(captor.getValue().getId()).isNotBlank();
    assertThat(stored.getContentType()).isEqualTo("image/png");
  }

  @Test
  void upload_should_detectJpegAndWebp() {
    when(tenantMediaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(tenantMediaService.upload(jpegBytes(), "photo.jpg", TENANT_ID).getContentType())
        .isEqualTo("image/jpeg");
    assertThat(tenantMediaService.upload(webpBytes(), "pic.webp", TENANT_ID).getContentType())
        .isEqualTo("image/webp");
  }

  @Test
  void upload_should_rejectUnsupportedContent_byMagicBytesNotExtension() {
    // an SVG (or any non-raster file) renamed to .png must still be rejected
    var svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes();

    assertThatThrownBy(() -> tenantMediaService.upload(svg, "image.png", TENANT_ID))
        .isInstanceOf(UnsupportedMediaContentException.class);
    verify(tenantMediaRepository, never()).save(any());
  }

  @Test
  void upload_should_rejectFilesOverTheSizeLimit() {
    var tooBig = Arrays.copyOf(pngBytes(), TenantMediaService.MAX_MEDIA_BYTES + 1);

    assertThatThrownBy(() -> tenantMediaService.upload(tooBig, "big.png", TENANT_ID))
        .isInstanceOf(MediaSizeLimitExceededException.class);
    verify(tenantMediaRepository, never()).save(any());
  }

  @Test
  void upload_should_rejectEmptyContent() {
    assertThatThrownBy(() -> tenantMediaService.upload(new byte[0], "empty.png", TENANT_ID))
        .isInstanceOf(UnsupportedMediaContentException.class);
  }

  @Test
  void findMedia_should_returnStoredEntity() {
    var entity =
        TenantMediaEntity.builder().id("abc").tenantId(TENANT_ID).contentType("image/png").build();
    when(tenantMediaRepository.findById("abc")).thenReturn(Optional.of(entity));

    assertThat(tenantMediaService.findMedia("abc")).contains(entity);
  }
}
