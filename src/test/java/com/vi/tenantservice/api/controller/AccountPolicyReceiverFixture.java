package com.vi.tenantservice.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Real isolated Tenant app; fixture observation never replaces the policy guard or its query. */
public final class AccountPolicyReceiverFixture {
  private AccountPolicyReceiverFixture() {}

  public static void main(String[] arguments) throws Exception {
    JsonNode fixture =
        new ObjectMapper()
            .readTree(Files.readString(Path.of(required("ORISO_TASK_TOKEN_FIXTURE"))));
    if (!"FIVE_ACTORS".equals(fixture.path("identityModel").asText())
        || fixture.path("tasks").size() != 5) {
      throw new IllegalStateException("Policy receiver requires the native five-account fixture");
    }
    String issuer = fixture.path("issuer").asText();
    System.setProperty("spring.profiles.active", "testing");
    System.setProperty("server.address", "127.0.0.1");
    System.setProperty("server.port", "0");
    System.setProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", issuer);
    System.setProperty(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        issuer + "/protocol/openid-connect/certs");
    System.setProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS");
    System.setProperty("TASK_IDENTITY_REQUIRED_TASKS", "INVITE_RESERVATIONS,CONSULTANT_IMPORT");
    System.setProperty("TASK_IDENTITY_AUDIENCE", "tenantservice");
    System.setProperty("ORISO_TASK_CONTEXT_KEY", required("ORISO_POLICY_SHARED_KEY"));
    // A C policy read must neither validate nor fall back to this deliberately unusable v1 key.
    System.setProperty("ORISO_WIZARD_POLICY_CONTEXT_KEY", "intentionally-invalid-legacy-key");
    for (JsonNode actor : fixture.path("tasks")) {
      String prefix = "IDENTITY_" + actor.path("key").asText();
      System.setProperty(prefix + "_CLIENT_ID", actor.path("clientId").asText());
      System.setProperty(prefix + "_SERVICE_SUBJECT", actor.path("subject").asText());
    }
    var queries = new AtomicLong();
    Path observed = Path.of(required("ORISO_POLICY_QUERY_COUNT"));
    Files.writeString(observed, "0");
    var application = new SpringApplication(TenantServiceApplication.class);
    application.addInitializers(
        context ->
            context
                .getBeanFactory()
                .addBeanPostProcessor(
                    new BeanPostProcessor() {
                      @Override
                      public Object postProcessAfterInitialization(Object bean, String name) {
                        if (!(bean instanceof TenantServiceFacade)) return bean;
                        var proxy = new ProxyFactory(bean);
                        proxy.setProxyTargetClass(true);
                        proxy.addAdvice(
                            (MethodInterceptor)
                                invocation -> {
                                  if ("findAccountProvisioningPolicy"
                                      .equals(invocation.getMethod().getName()))
                                    Files.writeString(
                                        observed, Long.toString(queries.incrementAndGet()));
                                  return invocation.proceed();
                                });
                        return proxy.getProxy();
                      }
                    }));
    try (var context = application.run(arguments)) {
      new ResourceDatabasePopulator(
              new ClassPathResource("database/TenantServiceDatabase.sql"),
              new ClassPathResource("database/MultiTenantData.sql"))
          .execute(context.getBean(DataSource.class));
      Files.writeString(
          Path.of(required("ORISO_POLICY_RECEIVER_READY")),
          "http://127.0.0.1:" + ((WebServerApplicationContext) context).getWebServer().getPort());
      System.in.read();
    }
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank())
      throw new IllegalStateException("Policy receiver fixture configuration missing: " + name);
    return value;
  }
}
