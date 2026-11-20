package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.TenantServiceApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * #346, no secret configured (as on an installation that never set it): saving a key is refused
 * with a typed error, nothing is stored, and the key does not show up in the answer or in a log.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(
    properties = {"spring.profiles.active=testing", "settings.smtp.password.encryption.secret="})
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
@ExtendWith(OutputCaptureExtension.class)
class TranslationApiKeyWithoutSecretIT extends TranslationApiKeyITSupport {

  @Test
  void savingAKey_Should_beRefusedWithATypedError_andStoreNothing(CapturedOutput output)
      throws Exception {
    var body =
        mvc.perform(
                put(KEY_URL)
                    .with(superAdmin())
                    .contentType(APPLICATION_JSON)
                    .content("{\"apiKey\":\"" + RAW_KEY + "\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("TRANSLATION_ENCRYPTION_NOT_CONFIGURED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).doesNotContain(RAW_KEY);
    assertThat(output.getAll()).doesNotContain(RAW_KEY);
    // no row at all, not merely a row without the key
    assertThat(repository.findAll()).isEmpty();
  }
}
