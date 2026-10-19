package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BrandingAssetDecoderTest {

  private final BrandingAssetDecoder decoder = new BrandingAssetDecoder();

  @Test
  void decode_shouldExposeDataUriAsHttpResponsePayload() {
    var decoded = decoder.decode("data:image/png;base64,aWNvbg==").orElseThrow();

    assertThat(decoded.contentType()).isEqualTo("image/png");
    assertThat(new String(decoded.bytes(), StandardCharsets.UTF_8)).isEqualTo("icon");
  }

  @Test
  void decode_shouldSupportLegacyRawBase64AsPng() {
    var decoded = decoder.decode("aWNvbg==").orElseThrow();

    assertThat(decoded.contentType()).isEqualTo("image/png");
    assertThat(new String(decoded.bytes(), StandardCharsets.UTF_8)).isEqualTo("icon");
  }

  @Test
  void decode_shouldRejectRemoteUrlsAndUnsupportedMediaTypes() {
    assertThat(decoder.decode("https://cdn.example.org/logo.png")).isEmpty();
    assertThat(decoder.decode("data:image/svg+xml;base64,PHN2Zz4=")).isEmpty();
  }

  @Test
  void decode_shouldRejectPayloadsLargerThanTheDecodedSizeCap() {
    var overCap = java.util.Base64.getEncoder().encodeToString(new byte[1024 * 1024 + 3]);

    assertThat(decoder.decode("data:image/png;base64," + overCap)).isEmpty();
  }
}
