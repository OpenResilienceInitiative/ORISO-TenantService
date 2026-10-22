package com.vi.tenantservice.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TaskServiceIdentityTest {
  @Test
  void differentActorsCannotShareClientOrSubjectEvenForOptionalBindings() {
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    groupedBindings("CONFIG_WIZARD")
                        .withProperty("IDENTITY_MAIL_CLIENT_ID", "backend-setup")
                        .withProperty("IDENTITY_MAIL_SERVICE_SUBJECT", "mail-subject")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    groupedBindings("CONFIG_WIZARD")
                        .withProperty("IDENTITY_ACCOUNT_CREATE_CLIENT_ID", "backend-account-create")
                        .withProperty("IDENTITY_ACCOUNT_CREATE_SERVICE_SUBJECT", "setup-subject")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void groupedModeRequiresActorBindingsAndCannotFallBackToOldTaskKeys() {
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    bindings()
                        .withProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS")
                        .withProperty("TASK_IDENTITY_REQUIRED_TASKS", "CONFIG_WIZARD")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    groupedBindings("CONFIG_WIZARD")
                        .withProperty("IDENTITY_RUNTIME_CLIENT_ID", "backend-runtime")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new TaskServiceIdentity(groupedBindings("UNKNOWN_PURPOSE")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    groupedBindings("CONFIG_WIZARD").withProperty("TASK_IDENTITY_AUDIENCE", "")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void completeGroupedSetupCannotAddHumanDefaultOrNativeAdministratorGrants() {
    var identity = new TaskServiceIdentity(groupedBindings("CONFIG_WIZARD"));
    for (String grant :
        List.of(
            "technical",
            "tenant-admin",
            "default-roles-online-beratung",
            "manage-users",
            "manage-realm",
            "view-users",
            "query-users",
            "realm-admin")) {
      assertThat(
              identity.allows(
                  groupedCaller(
                      List.of(
                          "config-wizard", "invitation-reservations", "consultant-import", grant)),
                  "CONFIG_WIZARD"))
          .isFalse();
    }
  }

  @Test
  void configuredAccountActorWithHumanGrantsIsStillClassifiedAsMachineToken() {
    var identity =
        new TaskServiceIdentity(
            groupedBindings("CONFIG_WIZARD")
                .withProperty("IDENTITY_ACCOUNT_ADMIN_CLIENT_ID", "backend-account-admin")
                .withProperty("IDENTITY_ACCOUNT_ADMIN_SERVICE_SUBJECT", "account-admin-subject"));
    var jwt =
        Jwt.withTokenValue("fixture")
            .header("alg", "none")
            .subject("account-admin-subject")
            .claim("azp", "backend-account-admin")
            .audience(List.of("receiver"))
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("realm_access", Map.of("roles", List.of("tenant-admin")))
            .build();

    assertThat(identity.isTaskToken(jwt)).isTrue();
    assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "CONFIG_WIZARD"))
        .isFalse();
  }

  @Test
  void groupedActorCannotInheritDefaultClientRoles() {
    var identity = new TaskServiceIdentity(groupedBindings("CONFIG_WIZARD"));
    var actor =
        groupedCaller(List.of("config-wizard", "invitation-reservations", "consultant-import"));
    var jwt =
        Jwt.withTokenValue("fixture")
            .headers(headers -> headers.putAll(actor.getToken().getHeaders()))
            .claims(claims -> claims.putAll(actor.getToken().getClaims()))
            .claim("resource_access", Map.of("account", Map.of("roles", List.of("manage-account"))))
            .build();

    assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "CONFIG_WIZARD"))
        .isFalse();
  }

  @Test
  void groupedSetupRequiresItsCompleteRoleUnion() {
    var identity = new TaskServiceIdentity(groupedBindings("CONFIG_WIZARD"));
    var incomplete = groupedCaller(List.of("config-wizard", "invitation-reservations"));

    assertThat(identity.allows(incomplete, "CONFIG_WIZARD")).isFalse();
  }

  @Test
  void misspelledOrBlankIdentityModelCannotFallBackToLegacyBindings() {
    for (String model : List.of("", " FIVE_ACTORS", "five_actors", "FOUR_ACTORS")) {
      assertThatThrownBy(
              () -> new TaskServiceIdentity(bindings().withProperty("TASK_IDENTITY_MODEL", model)))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void groupedActorCannotActivateAnUndeclaredReceiverPurpose() {
    var identity = new TaskServiceIdentity(groupedBindings("CONFIG_WIZARD"));
    var caller =
        groupedCaller(List.of("config-wizard", "invitation-reservations", "consultant-import"));

    assertThat(identity.allows(caller, "CONFIG_WIZARD")).isTrue();
    assertThat(identity.allows(caller, "INVITE_RESERVATIONS")).isFalse();
  }

  private MockEnvironment groupedBindings(String purposes) {
    return new MockEnvironment()
        .withProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS")
        .withProperty("TASK_IDENTITY_REQUIRED_TASKS", purposes)
        .withProperty("TASK_IDENTITY_AUDIENCE", "tenantservice")
        .withProperty("IDENTITY_SETUP_CLIENT_ID", "backend-setup")
        .withProperty("IDENTITY_SETUP_SERVICE_SUBJECT", "setup-subject");
  }

  private JwtAuthenticationToken groupedCaller(List<String> roles) {
    return new JwtAuthenticationToken(
        Jwt.withTokenValue("fixture")
            .header("alg", "none")
            .subject("setup-subject")
            .claim("azp", "backend-setup")
            .audience(List.of("tenantservice", "agencyservice", "consultingtypeservice", "userservice"))
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("realm_access", Map.of("roles", roles))
            .build(),
        List.of());
  }

  @Test
  void groupedSetupUsesOneBindingForDeclaredWizardAndReservationPurposes() {
    var identity =
        new TaskServiceIdentity(
            new MockEnvironment()
                .withProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS")
                .withProperty("TASK_IDENTITY_REQUIRED_TASKS", "CONFIG_WIZARD,INVITE_RESERVATIONS")
                .withProperty("TASK_IDENTITY_AUDIENCE", "tenantservice")
                .withProperty("IDENTITY_SETUP_CLIENT_ID", "backend-setup")
                .withProperty("IDENTITY_SETUP_SERVICE_SUBJECT", "setup-subject"));
    var actor =
        Jwt.withTokenValue("fixture")
            .header("alg", "none")
            .subject("setup-subject")
            .claim("azp", "backend-setup")
            .audience(List.of("tenantservice", "agencyservice", "consultingtypeservice", "userservice"))
            .expiresAt(Instant.now().plusSeconds(60))
            .claim(
                "realm_access",
                Map.of(
                    "roles",
                    List.of("config-wizard", "invitation-reservations", "consultant-import")))
            .build();
    var caller = new JwtAuthenticationToken(actor, List.of());

    assertThat(identity.allows(caller, "CONFIG_WIZARD")).isTrue();
    assertThat(identity.allows(caller, "INVITE_RESERVATIONS")).isTrue();
    assertThat(identity.allows(caller, "RUNTIME_POLICY")).isFalse();
  }

  private MockEnvironment bindings() {
    return new MockEnvironment()
        .withProperty("TASK_IDENTITY_AUDIENCE", "receiver")
        .withProperty("IDENTITY_CONFIG_WIZARD_CLIENT_ID", "backend-config-wizard")
        .withProperty("IDENTITY_CONFIG_WIZARD_SERVICE_SUBJECT", "wizard-subject");
  }

  @Test
  void requiredMissingAndDuplicatedBindingsFailStartup() {
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    new MockEnvironment()
                        .withProperty("TASK_IDENTITY_REQUIRED_TASKS", "CONFIG_WIZARD")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    bindings()
                        .withProperty(
                            "IDENTITY_NOTIFICATION_DISPATCH_CLIENT_ID", "backend-config-wizard")
                        .withProperty(
                            "IDENTITY_NOTIFICATION_DISPATCH_SERVICE_SUBJECT", "dispatch-subject")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new TaskServiceIdentity(
                    bindings()
                        .withProperty(
                            "IDENTITY_NOTIFICATION_DISPATCH_CLIENT_ID",
                            "backend-notification-dispatch")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void boundTaskCannotInheritHumanOrNativeAdministration() {
    var identity = new TaskServiceIdentity(bindings());
    for (String forbidden :
        List.of(
            "technical",
            "tenant-admin",
            "agency-admin",
            "realm-admin",
            "manage-users",
            "impersonation",
            "*")) {
      var jwt = token(List.of("config-wizard", forbidden));
      assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "CONFIG_WIZARD"))
          .isFalse();
      assertThat(identity.isTaskToken(jwt)).isTrue();
    }
    var jwt = token(List.of("config-wizard"));
    assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "CONFIG_WIZARD"))
        .isTrue();
    assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "INVITE_RESERVATIONS"))
        .isFalse();
  }

  private Jwt token(List<String> roles) {
    return Jwt.withTokenValue("fixture")
        .header("alg", "none")
        .subject("wizard-subject")
        .claim("azp", "backend-config-wizard")
        .audience(List.of("receiver"))
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("realm_access", Map.of("roles", roles))
        .build();
  }

  @Test
  void canonicalActorAudiencesAllowOnlyTheirExistingReceiverPurposes() {
    for (var vector : audienceVectors()) {
      for (String purpose : vector.purposes()) {
        var identity = new TaskServiceIdentity(audienceBindings(vector, purpose));
        boolean routed = true;
        assertThat(identity.allows(audienceCaller(vector, vector.audiences()), purpose))
            .isEqualTo(routed);
        var reordered = new java.util.ArrayList<>(vector.audiences());
        java.util.Collections.reverse(reordered);
        assertThat(identity.allows(audienceCaller(vector, reordered), purpose))
            .isEqualTo(routed);
      }
    }
  }

  @Test
  void everyCanonicalAudienceIsRequiredEvenWhenTheLocalReceiverRemains() {
    var checks = new java.util.ArrayList<org.junit.jupiter.api.function.Executable>();
    for (var vector : audienceVectors()) {
      for (int index = 0; index < vector.audiences().size(); index++) {
        var missing = new java.util.ArrayList<>(vector.audiences());
        missing.remove(index);
        var identity = new TaskServiceIdentity(audienceBindings(vector, vector.purposes().get(0)));
        checks.add(() -> assertThat(identity.allows(
            audienceCaller(vector, missing), vector.purposes().get(0))).isFalse());
      }
    }
    org.junit.jupiter.api.Assertions.assertAll(checks);
  }

  @Test
  void extraDuplicateAndWrongCaseAudiencesCannotBroadenAnActor() {
    var checks = new java.util.ArrayList<org.junit.jupiter.api.function.Executable>();
    for (var vector : audienceVectors()) {
      var extra = new java.util.ArrayList<>(vector.audiences());
      extra.add("unexpected-receiver");
      var duplicate = new java.util.ArrayList<>(vector.audiences());
      duplicate.add(vector.audiences().get(0));
      var differentCase = new java.util.ArrayList<>(vector.audiences());
      differentCase.set(0, differentCase.get(0).toUpperCase(java.util.Locale.ROOT));
      var broadened = new java.util.ArrayList<>(vector.audiences());
      broadened.add("tenantservice");
      for (var invalid : List.of(extra, duplicate, differentCase, broadened)) {
        var identity = new TaskServiceIdentity(audienceBindings(vector, vector.purposes().get(0)));
        checks.add(() -> assertThat(identity.allows(
            audienceCaller(vector, invalid), vector.purposes().get(0))).isFalse());
      }
    }
    org.junit.jupiter.api.Assertions.assertAll(checks);
  }

  @Test
  void malformedAudienceClaimsDenyWithoutConversionOrAnException() {
    var vector = audienceVectors().get(0);
    var identity = new TaskServiceIdentity(audienceBindings(vector, "CONFIG_WIZARD"));
    var malformed = new java.util.ArrayList<Object>();
    malformed.add(null);
    malformed.add("tenantservice");
    malformed.add(7);
    malformed.add(Map.of("aud", "tenantservice"));
    malformed.add(List.of(7));
    malformed.add(java.util.Arrays.asList("tenantservice", null));
    malformed.add(List.of());
    var checks = new java.util.ArrayList<org.junit.jupiter.api.function.Executable>();
    for (Object invalid : malformed) {
      checks.add(() -> assertThat(org.junit.jupiter.api.Assertions.assertDoesNotThrow(
          () -> identity.allows(audienceCaller(vector, invalid), "CONFIG_WIZARD"))).isFalse());
    }
    org.junit.jupiter.api.Assertions.assertAll(checks);
  }

  @Test
  void defaultLegacyAudienceMatchingRetainsItsExistingContainsBehavior() {
    var identity = new TaskServiceIdentity(bindings());
    var original = token(List.of("config-wizard"));
    var jwt = Jwt.withTokenValue("fixture")
        .headers(headers -> headers.putAll(original.getHeaders()))
        .claims(claims -> claims.putAll(original.getClaims()))
        .audience(List.of("receiver", "legacy-other-receiver"))
        .build();
    assertThat(identity.allows(new JwtAuthenticationToken(jwt, List.of()), "CONFIG_WIZARD"))
        .isTrue();
  }

  private MockEnvironment audienceBindings(AudienceVector vector, String purpose) {
    return new MockEnvironment()
        .withProperty("TASK_IDENTITY_MODEL", "FIVE_ACTORS")
        .withProperty("TASK_IDENTITY_REQUIRED_TASKS", purpose)
        .withProperty("TASK_IDENTITY_AUDIENCE", "tenantservice")
        .withProperty("IDENTITY_" + vector.actor() + "_CLIENT_ID", vector.client())
        .withProperty("IDENTITY_" + vector.actor() + "_SERVICE_SUBJECT", vector.subject());
  }

  private JwtAuthenticationToken audienceCaller(AudienceVector vector, Object audience) {
    var jwt = Jwt.withTokenValue("fixture")
        .header("alg", "none")
        .subject(vector.subject())
        .claim("azp", vector.client())
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("realm_access", Map.of("roles", vector.roles()))
        .claims(claims -> {
          if (audience == null) claims.remove("aud");
          else claims.put("aud", audience);
        })
        .build();
    return new JwtAuthenticationToken(jwt, List.of());
  }

  private List<AudienceVector> audienceVectors() {
    return List.of(
        new AudienceVector("SETUP", "backend-setup", "setup-subject",
            List.of("config-wizard", "invitation-reservations", "consultant-import"),
            List.of("tenantservice", "agencyservice", "consultingtypeservice", "userservice"),
            List.of("CONFIG_WIZARD", "INVITE_RESERVATIONS", "CONSULTANT_IMPORT")),
        new AudienceVector("MAIL", "backend-mail", "mail-subject",
            List.of("smtp-sync", "system-email-delivery", "notification-dispatch", "notifications-technical"),
            List.of("tenantservice", "agencyservice", "consultingtypeservice", "userservice", "oriso-task-commands"),
            List.of("SMTP_SYNC", "SYSTEM_EMAIL_DELIVERY", "NOTIFICATION_DISPATCH")),
        new AudienceVector("RUNTIME", "backend-runtime", "runtime-subject",
            List.of("runtime-policy", "matrix-agency", "matrix-agency-provision", "appointment-sync", "appointment-participant-cleanup"),
            List.of("tenantservice", "agencyservice", "appointmentservice"),
            List.of("RUNTIME_POLICY", "MATRIX_AGENCY")));
  }

  private record AudienceVector(String actor, String client, String subject,
      List<String> roles, List<String> audiences, List<String> purposes) {}
}
