package com.vi.tenantservice.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * #346, secret configured: the key reaches the database encrypted and no API caller or log line
 * ever sees it in clear. Uses the real cipher and the real database column.
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@Sql(scripts = {"/database/TenantServiceDatabase.sql", "/database/MultiTenantData.sql"})
@ExtendWith(OutputCaptureExtension.class)
class TranslationApiKeyAtRestIT extends TranslationApiKeyITSupport {

  @Test
  void savedKey_Should_beEncryptedInTheDatabase_andNeverReturnedOrLoggedInClear(
      CapturedOutput output) throws Exception {
    var putBody =
        mvc.perform(
                put(KEY_URL)
                    .with(superAdmin())
                    .contentType(APPLICATION_JSON)
                    .content("{\"apiKey\":\"" + RAW_KEY + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var getBody =
        mvc.perform(get("/tenantadmin/translation/keys").with(superAdmin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var controlsBody =
        mvc.perform(get("/tenantadmin/controls").with(superAdmin()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    var stored = repository.findTopByOrderByIdAsc().orElseThrow().getControls();
    assertThat(stored).contains("ENC:").doesNotContain(RAW_KEY);
    assertThat(putBody).doesNotContain(RAW_KEY);
    assertThat(getBody).doesNotContain(RAW_KEY);
    assertThat(controlsBody).doesNotContain(RAW_KEY).doesNotContain("ENC:");
    assertThat(output.getAll()).doesNotContain(RAW_KEY);
  }
}
