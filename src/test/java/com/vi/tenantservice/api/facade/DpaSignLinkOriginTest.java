package com.vi.tenantservice.api.facade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DpaSignLinkOriginTest {

  private final ApplicationContextRunner context =
      new ApplicationContextRunner().withBean(DpaSignLinkOrigin.class);

  @Test
  void missingOriginStopsStartupAndNamesSetting() {
    context.run(
        result -> {
          assertThat(result).hasFailed();
          assertThat(result.getStartupFailure()).rootCause().hasMessageContaining("APP_BASE_URL");
        });
  }

  @Test
  void invalidOriginsStopStartupAndNameSetting() {
    for (String origin :
        new String[] {
          " ",
          "/app",
          "example.org",
          "ftp://example.org",
          "http://app.example.org",
          "https://example.org/app",
          "https://example.org/%2F",
          "https://example.org?redirect=evil"
        }) {
      context
          .withPropertyValues("app.base.url=" + origin)
          .run(
              result -> {
                assertThat(result).hasFailed();
                assertThat(result.getStartupFailure())
                    .rootCause()
                    .hasMessageContaining("APP_BASE_URL");
              });
    }
  }

  @Test
  void configuredOriginProducesAbsoluteSignLink() {
    context
        .withPropertyValues("app.base.url=https://app.example.org/")
        .run(
            result -> {
              assertThat(result).hasNotFailed();
              assertThat(result.getBean(DpaSignLinkOrigin.class).build("token-1"))
                  .isEqualTo("https://app.example.org/dpa-sign/token-1");
            });
  }
}
