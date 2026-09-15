package com.vi.tenantservice.api.service.systememail;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vi.tenantservice.config.security.AuthorisationService;
import com.vi.tenantservice.config.security.JwtAuthConverterProperties;
import com.vi.tenantservice.config.security.WebSecurityConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(SystemEmailDeliveryControllerTest.Config.class)
@WebAppConfiguration
class SystemEmailDeliveryControllerTest {
  private static final java.security.KeyPair KEYS = keys();

  private static java.security.KeyPair keys() {
    try {
      var generator = java.security.KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private String token(String variant) throws Exception {
    var now = java.time.Instant.now();
    var claims =
        new com.nimbusds.jwt.JWTClaimsSet.Builder()
            .issuer(
                variant.equals("wrong-issuer")
                    ? "https://wrong.example.org"
                    : "https://issuer.example.org")
            .subject(variant.equals("other") ? "other-id" : "service-id")
            .issueTime(java.util.Date.from(now.minusSeconds(300)))
            .expirationTime(
                java.util.Date.from(now.plusSeconds(variant.equals("expired") ? -180 : 120)))
            .claim("azp", variant.equals("other-client") ? "other" : "app")
            .claim(
                "realm_access",
                Map.of("roles", List.of(variant.equals("admin") ? "tenant-admin" : "technical")))
            .build();
    var signed =
        new com.nimbusds.jwt.SignedJWT(
            new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.RS256), claims);
    signed.sign(
        new com.nimbusds.jose.crypto.RSASSASigner(
            (java.security.interfaces.RSAPrivateKey)
                (variant.equals("invalid") ? keys() : KEYS).getPrivate()));
    return signed.serialize();
  }

  @Configuration
  @EnableWebMvc
  @Import({WebSecurityConfig.class, AuthorisationService.class, JwtAuthConverterProperties.class})
  static class Config {
    @Bean
    SystemEmailDeliveryService delivery() {
      return mock(SystemEmailDeliveryService.class);
    }

    @Bean
    SystemEmailDeliveryController controller(SystemEmailDeliveryService service) {
      return new SystemEmailDeliveryController(service);
    }

    @Bean(name = "systemEmailServiceIdentity")
    SystemEmailServiceIdentity identity() {
      return new SystemEmailServiceIdentity("service-id", "app");
    }

    @Bean
    JwtDecoder decoder() {
      var decoder =
          NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) KEYS.getPublic())
              .build();
      decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("https://issuer.example.org"));
      return decoder;
    }
  }

  @Autowired WebApplicationContext context;
  @Autowired SystemEmailDeliveryService delivery;
  MockMvc mvc;
  final String body =
      "{\"purpose\":\"EMAIL_ADDRESS_CHANGED\",\"recipient\":\"recipient@example.org\",\"subject\":\"Subject\",\"html\":\"<p>Body</p>\",\"text\":\"Body\",\"correlationId\":\"123e4567-e89b-12d3-a456-426614174000\"}";

  @BeforeEach
  void setup() {
    reset(delivery);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void anonymousAndInvalidTokensCannotReachDelivery() throws Exception {
    mvc.perform(
            post("/tenant/40/internal/system-email-deliveries")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/tenant/40/internal/system-email-deliveries")
                .header("Authorization", "Bearer " + token("invalid"))
                .contentType("application/json")
                .content(body))
        .andExpect(status().isUnauthorized());
    for (String variant : List.of("wrong-issuer", "expired")) {
      mvc.perform(
              post("/tenant/40/internal/system-email-deliveries")
                  .header("Authorization", "Bearer " + token(variant))
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isUnauthorized());
    }
    verifyNoInteractions(delivery);
  }

  @Test
  void adminWrongSubjectAndWrongClientCannotReachDelivery() throws Exception {
    for (String token : List.of("admin", "other", "other-client"))
      mvc.perform(
              post("/tenant/40/internal/system-email-deliveries")
                  .header("Authorization", "Bearer " + token(token))
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isForbidden());
    verifyNoInteractions(delivery);
  }

  @Test
  void boundIdentityCanDeliverOnlyExplicitPathTenantAndNoStore() throws Exception {
    when(delivery.deliver(eq(40L), any())).thenReturn(true);
    mvc.perform(
            post("/tenant/40/internal/system-email-deliveries")
                .header("Authorization", "Bearer " + token("valid"))
                .header("tenantId", "1")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    verify(delivery).deliver(eq(40L), any());
  }

  @Test
  void invalidRecipientPurposeAndTransportOverridesRejected() throws Exception {
    for (String invalid :
        List.of(
            body.replace("recipient@example.org", "one@example.org,two@example.org"),
            body.replace("EMAIL_ADDRESS_CHANGED", "FREE_FORM"),
            body.replace("Subject", "S".repeat(257)),
            body.replace("\"purpose\"", "\"host\":\"override.example.org\",\"purpose\"")))
      mvc.perform(
              post("/tenant/40/internal/system-email-deliveries")
                  .header("Authorization", "Bearer " + token("valid"))
                  .contentType("application/json")
                  .content(invalid))
          .andExpect(status().isBadRequest())
          .andExpect(content().string(""))
          .andExpect(header().string("Cache-Control", "no-store"));
    verifyNoInteractions(delivery);
  }
}
