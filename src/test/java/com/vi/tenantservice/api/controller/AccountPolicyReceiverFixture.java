package com.vi.tenantservice.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.TenantServiceApplication;
import com.vi.tenantservice.api.facade.TenantServiceFacade;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.aopalliance.intercept.MethodInterceptor;
import org.hibernate.SessionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ContentCachingResponseWrapper;

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
    System.setProperty(
        "TASK_IDENTITY_REQUIRED_TASKS", "INVITE_RESERVATIONS,CONSULTANT_IMPORT,RUNTIME_POLICY");
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
    Path mode = Path.of(required("ORISO_POLICY_TENANT_MODE"));
    Files.writeString(mode, "live");
    var application = new SpringApplication(TenantServiceApplication.class, SqlObservation.class);
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
                                      .equals(invocation.getMethod().getName())) {
                                    Files.writeString(
                                        observed, Long.toString(queries.incrementAndGet()));
                                    if ("outage".equals(Files.readString(mode)))
                                      throw new ResponseStatusException(
                                          HttpStatus.SERVICE_UNAVAILABLE,
                                          "Synthetic policy projection outage");
                                  }
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
      var controls =
          context.getBean(com.vi.tenantservice.api.service.TenantAdminControlsService.class);
      var prior = controls.getAccountInactivitySettings();
      controls.updateAccountInactivitySettings(
          new com.vi.tenantservice.api.model.AccountInactivitySettings(
              7, 11, 18, prior.getRevision()));
      var statistics =
          context.getBean(EntityManagerFactory.class).unwrap(SessionFactory.class).getStatistics();
      Files.writeString(
          Path.of(required("ORISO_POLICY_SQL_COUNT")),
          Long.toString(statistics.getQueryExecutionCount()));
      Files.writeString(
          Path.of(required("ORISO_POLICY_RECEIVER_READY")),
          "http://127.0.0.1:" + ((WebServerApplicationContext) context).getWebServer().getPort());
      System.in.read();
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  public static class SqlObservation {
    @Bean
    FilterRegistrationBean<OncePerRequestFilter> policySqlObserver(EntityManagerFactory factory) {
      var statistics = factory.unwrap(SessionFactory.class).getStatistics();
      statistics.setStatisticsEnabled(true);
      var filter =
          new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
              var observedResponse = new ContentCachingResponseWrapper(response);
              boolean completed = false;
              try {
                chain.doFilter(request, observedResponse);
                completed = true;
              } finally {
                Files.writeString(
                    Path.of(required("ORISO_POLICY_SQL_COUNT")),
                    Long.toString(statistics.getQueryExecutionCount()));
                if (completed) observedResponse.copyBodyToResponse();
              }
            }
          };
      var registration = new FilterRegistrationBean<OncePerRequestFilter>(filter);
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
      return registration;
    }
  }

  private static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank())
      throw new IllegalStateException("Policy receiver fixture configuration missing: " + name);
    return value;
  }
}
