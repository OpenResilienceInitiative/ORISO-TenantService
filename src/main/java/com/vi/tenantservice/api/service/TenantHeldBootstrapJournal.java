package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import com.vi.tenantservice.config.security.TaskContextProof;
import com.vi.tenantservice.config.security.TaskServiceIdentity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Durable witnesses are written only by the private actual-INSERT owner. */
@Component
public final class TenantHeldBootstrapJournal {
  static final ObjectMapper JSON =
      new ObjectMapper()
          .findAndRegisterModules()
          .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final JdbcTemplate sql;
  private final TaskServiceIdentity identity;
  private final Environment environment;

  public TenantHeldBootstrapJournal(
      DataSource source, TaskServiceIdentity identity, Environment environment) {
    this.sql = new JdbcTemplate(source);
    this.sql.setQueryTimeout(3);
    this.identity = identity;
    this.environment = environment;
  }

  Operation prepare(JsonNode command) {
    noAmbient();
    Binding binding = caller();
    fields(command, "claimId", "accountAttemptId", "executionId", "tenant");
    UUID claim = uuid(command.path("claimId")),
        attempt = uuid(command.path("accountAttemptId")),
        execution = uuid(command.path("executionId"));
    JsonNode tenant = command.path("tenant");
    if (!tenant.isObject()
        || !tenant.path("id").isIntegralNumber()
        || !tenant.path("id").canConvertToLong()
        || tenant.path("id").longValue() < 1
        || !tenant.path("tenantIdReservationToken").isTextual()
        || tenant.path("tenantIdReservationToken").textValue().isBlank()
        || tenant.path("tenantIdReservationToken").textValue().length() > 255) throw denied();
    try {
      MultilingualTenantDTO dto = JSON.treeToValue(tenant, MultilingualTenantDTO.class);
      String request = digest("tenant-held-bootstrap-request/v1", command);
      String capability =
          digest(
              "tenant-held-bootstrap-capability/v1",
              Map.of("tenantId", dto.getId(), "token", dto.getTenantIdReservationToken()));
      var operation =
          new Operation(
              this,
              claim,
              attempt,
              execution,
              binding,
              dto,
              request,
              capability,
              digest("tenant-held-bootstrap-input/v1", JSON.valueToTree(dto)));
      Row row = read(operation, false);
      if (row != null) operation.command = row.command.getBytes(StandardCharsets.UTF_8);
      return operation;
    } catch (org.springframework.dao.DataAccessException unavailable) {
      throw unavailable;
    } catch (Exception failure) {
      throw denied();
    }
  }

  void freeze(Operation operation, byte[] command) {
    require(operation);
    if (operation.command != null
        || command == null
        || command.length == 0
        || command.length > 1_048_576) throw denied();
    operation.command = command.clone();
  }

  void require(Operation operation) {
    if (operation == null
        || operation.owner != this
        || !operation.open
        || operation.thread != Thread.currentThread()
        || System.nanoTime() - operation.started >= 60_000_000_000L
        || !operation.binding.equals(caller())) throw denied();
    if (!equal(
        operation.inputDigest,
        digest("tenant-held-bootstrap-input/v1", JSON.valueToTree(operation.input))))
      throw denied();
  }

  Row read(Operation operation, boolean lock) {
    require(operation);
    var rows =
        sql.query(
            "SELECT * FROM tenant_bootstrap_operation WHERE claim_id=?"
                + (lock ? " FOR UPDATE" : ""),
            (rs, index) -> row(rs),
            operation.claim.toString());
    if (rows.size() > 1) throw denied();
    if (rows.isEmpty()) return null;
    Row row = rows.get(0);
    if (!row.claim.equals(operation.claim.toString())
        || !row.accountAttempt.equals(operation.accountAttempt.toString())
        || !row.execution.equals(operation.execution.toString())
        || row.tenantId != operation.input.getId()
        || !row.binding.equals(operation.binding)
        || !equal(row.requestDigest, operation.requestDigest)
        || !equal(row.capabilityDigest, operation.capabilityDigest)
        || !Set.of("SQL_CREATED", "CTS_PENDING", "CTS_VERIFIED", "COMPLETE").contains(row.state)
        || !equal(row.commandDigest, digest("tenant-held-bootstrap-command/v1", parse(row.command)))
        || row.tenantColumns < 1
        || row.ledgerColumns < 1
        || row.version < 1) throw denied();
    if (operation.command != null
        && !MessageDigest.isEqual(operation.command, row.command.getBytes(StandardCharsets.UTF_8)))
      throw denied();
    return row;
  }

  void insert(
      Operation operation, String marker, String snapshot, int tenantColumns, int ledgerColumns) {
    require(operation);
    requireTransaction();
    if (operation.command == null || read(operation, false) != null) throw denied();
    String command = new String(operation.command, StandardCharsets.UTF_8);
    int changed =
        sql.update(
            "INSERT INTO tenant_bootstrap_operation (claim_id,account_attempt_id,execution_id,tenant_id,bootstrap_attempt_id,owner_issuer,owner_client,owner_subject,request_digest,capability_digest,snapshot_digest,tenant_columns,ledger_columns,cts_command,cts_command_digest,state,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SQL_CREATED',1)",
            operation.claim.toString(),
            operation.accountAttempt.toString(),
            operation.execution.toString(),
            operation.input.getId(),
            marker,
            operation.binding.issuer,
            operation.binding.client,
            operation.binding.subject,
            operation.requestDigest,
            operation.capabilityDigest,
            snapshot,
            tenantColumns,
            ledgerColumns,
            command,
            digest("tenant-held-bootstrap-command/v1", parse(command)));
    if (changed != 1) throw denied();
  }

  void stage(Operation operation, String desired) {
    requireTransaction();
    Row row = read(operation, true);
    if (row == null || "COMPLETE".equals(row.state)) throw denied();
    if (row.state.equals(desired)
        || ("CTS_PENDING".equals(desired) && "CTS_VERIFIED".equals(row.state))) return;
    if (!("CTS_PENDING".equals(desired) && "SQL_CREATED".equals(row.state))
        && !("CTS_VERIFIED".equals(desired)
            && Set.of("SQL_CREATED", "CTS_PENDING").contains(row.state))) throw denied();
    if (sql.update(
            "UPDATE tenant_bootstrap_operation SET state=?,version=version+1 WHERE claim_id=? AND version=? AND state=?",
            desired,
            row.claim,
            row.version,
            row.state)
        != 1) throw denied();
  }

  void complete(Operation operation, String resultDigest) {
    requireTransaction();
    Row row = read(operation, true);
    if (row == null) throw denied();
    if ("COMPLETE".equals(row.state)) {
      if (!equal(row.resultDigest, resultDigest)) throw denied();
      return;
    }
    if (!"CTS_VERIFIED".equals(row.state)) throw denied();
    if (sql.update(
            "UPDATE tenant_bootstrap_operation SET state='COMPLETE',result_digest=?,version=version+1 WHERE claim_id=? AND version=? AND state='CTS_VERIFIED'",
            resultDigest,
            row.claim,
            row.version)
        != 1) throw denied();
  }

  String receipt(Operation operation, String resultDigest) {
    Row row = read(operation, false);
    if (row == null || !"COMPLETE".equals(row.state) || !equal(row.resultDigest, resultDigest))
      throw denied();
    var payload = new java.util.TreeMap<String, Object>();
    payload.put("v", 1);
    payload.put("tenantId", row.tenantId);
    payload.put("claimId", row.claim);
    payload.put("accountAttemptId", row.accountAttempt);
    payload.put("executionId", row.execution);
    payload.put("bootstrapAttemptId", row.marker);
    payload.put("ownerIssuer", row.binding.issuer);
    payload.put("ownerClient", row.binding.client);
    payload.put("ownerSubject", row.binding.subject);
    payload.put("requestDigest", row.requestDigest);
    payload.put("resultDigest", row.resultDigest);
    payload.put("state", "COMPLETE");
    try {
      String encoded =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(TaskContextProof.canonical(payload));
      return encoded
          + "."
          + HexFormat.of()
              .formatHex(
                  mac(
                      "tenant-held-bootstrap-receipt/v1\n",
                      encoded.getBytes(StandardCharsets.US_ASCII)));
    } catch (Exception invalid) {
      throw denied();
    }
  }

  String digest(String domain, Object value) {
    try {
      return HexFormat.of().formatHex(mac(domain + "\n", TaskContextProof.canonical(value)));
    } catch (Exception invalid) {
      throw denied();
    }
  }

  private byte[] mac(String prefix, byte[] value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(TaskContextProof.key(environment), "HmacSHA256"));
    mac.update(prefix.getBytes(StandardCharsets.US_ASCII));
    return mac.doFinal(value);
  }

  private Binding caller() {
    if (!TaskContextProof.grouped(environment) || !identity.current("CONFIG_WIZARD"))
      throw denied();
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof Jwt jwt)
        || jwt.getIssuer() == null
        || jwt.getSubject() == null
        || jwt.getClaimAsString("azp") == null) throw denied();
    return new Binding(jwt.getIssuer().toString(), jwt.getClaimAsString("azp"), jwt.getSubject());
  }

  static JsonNode parse(String value) {
    try {
      return JSON.readTree(value);
    } catch (Exception invalid) {
      throw denied();
    }
  }

  static void noAmbient() {
    if (TransactionSynchronizationManager.isActualTransactionActive()
        || TransactionSynchronizationManager.isSynchronizationActive()) throw denied();
  }

  private static void requireTransaction() {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) throw denied();
  }

  private static void fields(JsonNode value, String... fields) {
    if (value == null || !value.isObject()) throw denied();
    var actual = new HashSet<String>();
    value.fieldNames().forEachRemaining(actual::add);
    if (!actual.equals(Set.of(fields))) throw denied();
  }

  private static UUID uuid(JsonNode value) {
    if (!value.isTextual()) throw denied();
    try {
      UUID id = UUID.fromString(value.textValue());
      if (!id.toString().equals(value.textValue())) throw denied();
      return id;
    } catch (RuntimeException invalid) {
      throw denied();
    }
  }

  private static boolean equal(String a, String b) {
    return a != null
        && b != null
        && MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }

  static AccessDeniedException denied() {
    return new AccessDeniedException("Exact held tenant bootstrap required");
  }

  private static Row row(ResultSet r) throws SQLException {
    return new Row(
        r.getString("claim_id"),
        r.getString("account_attempt_id"),
        r.getString("execution_id"),
        r.getLong("tenant_id"),
        r.getString("bootstrap_attempt_id"),
        new Binding(
            r.getString("owner_issuer"), r.getString("owner_client"), r.getString("owner_subject")),
        r.getString("request_digest"),
        r.getString("capability_digest"),
        r.getString("snapshot_digest"),
        r.getInt("tenant_columns"),
        r.getInt("ledger_columns"),
        r.getString("cts_command"),
        r.getString("cts_command_digest"),
        r.getString("state"),
        r.getLong("version"),
        r.getString("result_digest"));
  }

  record Binding(String issuer, String client, String subject) {}

  record Row(
      String claim,
      String accountAttempt,
      String execution,
      long tenantId,
      String marker,
      Binding binding,
      String requestDigest,
      String capabilityDigest,
      String snapshotDigest,
      int tenantColumns,
      int ledgerColumns,
      String command,
      String commandDigest,
      String state,
      long version,
      String resultDigest) {
    @Override
    public String toString() {
      return "HeldTenantOperation[redacted]";
    }
  }

  /** Constructor and all mutable lifecycle state are owned by this journal, never by a DTO. */
  public static final class Operation {
    final TenantHeldBootstrapJournal owner;
    final UUID claim, accountAttempt, execution;
    final Binding binding;
    final MultilingualTenantDTO input;
    final String requestDigest, capabilityDigest, inputDigest;
    final Thread thread = Thread.currentThread();
    final long started = System.nanoTime();
    boolean open = true;
    byte[] command;

    private Operation(
        TenantHeldBootstrapJournal owner,
        UUID claim,
        UUID accountAttempt,
        UUID execution,
        Binding binding,
        MultilingualTenantDTO input,
        String requestDigest,
        String capabilityDigest,
        String inputDigest) {
      this.owner = owner;
      this.claim = claim;
      this.accountAttempt = accountAttempt;
      this.execution = execution;
      this.binding = binding;
      this.input = input;
      this.requestDigest = requestDigest;
      this.capabilityDigest = capabilityDigest;
      this.inputDigest = inputDigest;
    }

    @Override
    public String toString() {
      return "HeldTenantOperation[redacted]";
    }
  }
}
