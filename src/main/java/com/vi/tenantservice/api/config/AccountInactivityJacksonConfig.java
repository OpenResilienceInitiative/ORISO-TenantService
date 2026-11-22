package com.vi.tenantservice.api.config;

import com.vi.tenantservice.api.model.AccountInactivitySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;

/** Keeps fractional or string JSON values from silently changing an account lifetime. */
@Configuration
public class AccountInactivityJacksonConfig {
  @Bean
  public JacksonModule accountInactivityModule() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(AccountInactivitySettings.class, new StrictSettingsDeserializer());
    return module;
  }

  private static class StrictSettingsDeserializer
      extends ValueDeserializer<AccountInactivitySettings> {
    @Override
    public AccountInactivitySettings deserialize(
        JsonParser parser, DeserializationContext context) {
      JsonNode node = context.readTree(parser);
      if (!node.isObject()) {
        return context.reportInputMismatch(
            AccountInactivitySettings.class, "Settings must be an object");
      }
      return new AccountInactivitySettings(
          months(node, "askerMonths", context),
          months(node, "consultantMonths", context),
          months(node, "otherMonths", context),
          revision(node, context));
    }

    private Integer months(JsonNode node, String field, DeserializationContext context) {
      JsonNode value = node.get(field);
      if (value == null || value.isNull()) return null;
      if (!value.isIntegralNumber() || !value.canConvertToInt()) {
        return context.reportInputMismatch(
            AccountInactivitySettings.class, "%s must be an integer", field);
      }
      return value.intValue();
    }

    private Long revision(JsonNode node, DeserializationContext context) {
      JsonNode value = node.get("revision");
      if (value == null || value.isNull()) return null;
      if (!value.isIntegralNumber() || !value.canConvertToLong()) {
        return context.reportInputMismatch(
            AccountInactivitySettings.class, "revision must be an integer");
      }
      return value.longValue();
    }
  }
}
