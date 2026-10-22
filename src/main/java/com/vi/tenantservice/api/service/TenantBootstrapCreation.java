package com.vi.tenantservice.api.service;

import com.vi.tenantservice.api.exception.TenantIdAllocationConflictException;
import com.vi.tenantservice.api.exception.TenantIdAllocationExhaustedException;
import com.vi.tenantservice.api.model.TenantEntity;
import com.vi.tenantservice.config.security.TaskContextProof;
import com.vi.tenantservice.config.security.TaskServiceIdentity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;
import javax.sql.DataSource;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Owns a new INSERT and the bounded, synchronous authority derived from its known commit. */
@Service
public class TenantBootstrapCreation {
  private final TenantService tenants;
  private final TaskServiceIdentity identity;
  private final Environment environment;
  private final DataSource dataSource;
  private final JdbcTemplate sql;
  private final TransactionTemplate transaction;

  @org.springframework.beans.factory.annotation.Autowired
  private com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService consultingTypes;

  @PersistenceContext private EntityManager entities;

  @org.springframework.beans.factory.annotation.Autowired
  private TenantHeldBootstrapJournal heldJournal;

  public TenantBootstrapCreation(
      TenantService tenants,
      TaskServiceIdentity identity,
      Environment environment,
      DataSource dataSource,
      PlatformTransactionManager transactionManager) {
    this.tenants = tenants;
    this.identity = identity;
    this.environment = environment;
    this.dataSource = dataSource;
    this.sql = new JdbcTemplate(dataSource);
    this.sql.setQueryTimeout(3);
    this.transaction = new TransactionTemplate(transactionManager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.transaction.setTimeout(10);
  }

  public boolean current() {
    return TaskContextProof.grouped(environment) && identity.current("CONFIG_WIZARD");
  }

  public <T> T create(
      TenantEntity input, String reservationToken, BiFunction<TenantEntity, Grant, T> callback) {
    return createInternal(input, reservationToken, null, callback);
  }

  public void requireHeldInput(TenantHeldBootstrapJournal.Operation operation,
      com.vi.tenantservice.api.model.MultilingualTenantDTO input) {
    heldJournal.require(operation);
    if (!operation.inputDigest.equals(heldJournal.digest("tenant-held-bootstrap-input/v1",
        TenantHeldBootstrapJournal.JSON.valueToTree(input)))) throw denied();
  }

  public <T> T createHeld(TenantEntity input, String reservationToken,
      TenantHeldBootstrapJournal.Operation operation, BiFunction<TenantEntity, Grant, T> callback) {
    heldJournal.require(operation);
    if (!operation.input.getId().equals(input.getId())
        || !operation.input.getTenantIdReservationToken().equals(reservationToken)) throw denied();
    return createInternal(input, reservationToken, operation, callback);
  }

  private <T> T createInternal(TenantEntity input, String reservationToken,
      TenantHeldBootstrapJournal.Operation operation, BiFunction<TenantEntity, Grant, T> callback) {
    noAmbientTransaction();
    Binding owner = caller();
    TaskContextProof.key(environment);
    boolean auto = input.getId() == null;
    int attempts = auto ? TenantIdAllocationService.MAX_AUTO_ATTEMPTS : 1;
    TenantIdAllocationConflictException conflict = null;
    for (int index = 0; index < attempts; index++) {
      Attempt attempt = new Attempt(owner, UUID.randomUUID().toString(), operation);
      TenantEntity fresh = new TenantEntity();
      BeanUtils.copyProperties(input, fresh);
      try {
        TenantEntity created =
            transaction.execute(
                status -> {
                  TransactionSynchronizationManager.registerSynchronization(
                      new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int completion) {
                          attempt.completion = completion;
                        }
                      });
                  if (fresh.getId() != null) {
                    Snapshot previous = transactionalSnapshot(fresh.getId(), true);
                    if (!previous.tenant().isEmpty()) {
                      throw new TenantIdAllocationConflictException(
                          "Tenant ID is already assigned");
                    }
                    attempt.previousLedger = previous.ledger();
                  }
                  tenants.prepareBootstrapInsert(fresh, reservationToken);
                  attempt.id = fresh.getId();
                  entities.persist(fresh);
                  entities.flush();
                  if (sql.update(
                          "UPDATE tenant SET bootstrap_attempt_id=? WHERE id=? AND bootstrap_attempt_id IS NULL",
                          attempt.marker,
                          attempt.id)
                      != 1) {
                    throw denied();
                  }
                  attempt.snapshot = transactionalSnapshot(attempt.id, false);
                  if (!attempt.marker.equals(
                          value(attempt.snapshot.tenant(), "bootstrap_attempt_id"))
                      || !"ASSIGNED".equals(value(attempt.snapshot.ledger(), "status"))) {
                    throw denied();
                  }
                  if (operation != null) heldJournal.insert(operation, attempt.marker,
                      heldFingerprint(attempt.snapshot), attempt.snapshot.tenant().size(), attempt.snapshot.ledger().size());
                  return fresh;
                });
        if (attempt.completion != TransactionSynchronization.STATUS_COMMITTED) throw denied();
        requireSnapshot(attempt);
        Grant grant = new Grant(this, attempt);
        try {
          return callback.apply(created, grant);
        } catch (RuntimeException failure) {
          if (operation == null && grant.remote != null) {
            try {
              grant.requireLive();
              grant.remote.compensate();
              grant.requireLive();
              // Exact terminal acknowledgement precedes the existing locked SQL ownership check.
              resolveBeforeSend(attempt, failure);
            } catch (RuntimeException unresolved) {
              failure.addSuppressed(
                  new AccessDeniedException("Owned remote bootstrap requires reconciliation"));
            }
          }
          throw failure;
        } finally {
          grant.active = false;
        }
      } catch (RuntimeException failure) {
        if (operation == null && !attempt.sendStarted) resolveBeforeSend(attempt, failure);
        if (failure instanceof TenantIdAllocationConflictException
            && attempt.completion == TransactionSynchronization.STATUS_ROLLED_BACK) {
          conflict = (TenantIdAllocationConflictException) failure;
          if (auto) continue;
        }
        throw failure;
      }
    }
    throw new TenantIdAllocationExhaustedException("AUTO tenant ID contention exhausted", conflict);
  }

  public <T> T resumeHeld(TenantHeldBootstrapJournal.Operation operation,
      BiFunction<TenantEntity, Grant, T> callback) {
    Attempt attempt = heldAttempt(operation);
    var row = heldJournal.read(operation, false);
    if ("COMPLETE".equals(row.state())) throw denied();
    Grant grant = new Grant(this, attempt);
    try { return callback.apply(heldEntity(attempt.snapshot), grant); }
    finally { grant.active = false; }
  }

  public <T> T readHeld(TenantHeldBootstrapJournal.Operation operation,
      java.util.function.Function<TenantEntity, T> reader) {
    Attempt attempt = heldAttempt(operation);
    T result = reader.apply(heldEntity(attempt.snapshot));
    requireSnapshot(attempt);
    return result;
  }

  public void completeHeld(TenantHeldBootstrapJournal.Operation operation, String resultDigest) {
    Attempt attempt = heldAttempt(operation);
    transaction.executeWithoutResult(status -> {
      if (!attempt.snapshot.equals(transactionalSnapshot(attempt.id, true))) throw denied();
      heldJournal.complete(operation, resultDigest);
    });
  }

  private Attempt heldAttempt(TenantHeldBootstrapJournal.Operation operation) {
    noAmbientTransaction();
    var row = heldJournal.read(operation, false);
    if (row == null) throw denied();
    var attempt = new Attempt(caller(), row.marker(), operation);
    attempt.id = row.tenantId();
    attempt.completion = TransactionSynchronization.STATUS_COMMITTED;
    attempt.snapshot = freshSnapshot(attempt.id, row.tenantColumns(), row.ledgerColumns());
    if (!row.marker().equals(value(attempt.snapshot.tenant(), "bootstrap_attempt_id"))
        || !"ASSIGNED".equals(value(attempt.snapshot.ledger(), "status"))
        || !row.snapshotDigest().equals(heldFingerprint(attempt.snapshot))) throw denied();
    requireSnapshot(attempt);
    return attempt;
  }

  private String heldFingerprint(Snapshot snapshot) {
    var rows = new TreeMap<String, Object>();
    for (var entry : Map.of("tenant", snapshot.tenant(), "ledger", snapshot.ledger()).entrySet()) {
      var columns = new TreeMap<String, Object>();
      entry.getValue().forEach((name, cell) -> {
        var value = new TreeMap<String, Object>();
        value.put("jdbcType", cell.jdbcType()); value.put("javaType", cell.javaType());
        value.put("value", cell.value() == null ? null : cell.value().toString());
        columns.put(name, value);
      });
      rows.put(entry.getKey(), columns);
    }
    return heldJournal.digest("tenant-held-bootstrap-state/v1", rows);
  }

  // Reconstruct only data from the same verified coherent native read, never stale JPA authority.
  private static TenantEntity heldEntity(Snapshot snapshot) {
    TenantEntity result = new TenantEntity();
    try {
      for (var field : TenantEntity.class.getDeclaredFields()) {
        var column = field.getAnnotation(jakarta.persistence.Column.class);
        if (column == null || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
        String name = column.name().isEmpty()
            ? field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT)
            : column.name().toLowerCase(Locale.ROOT);
        Cell cell = snapshot.tenant().get(name);
        if (cell == null) throw denied();
        Object value = cell.value();
        if (value instanceof Number number) {
          if (field.getType() == Boolean.class) value = number.intValue() != 0;
          else if (field.getType() == Integer.class) value = Math.toIntExact(number.longValue());
          else if (field.getType() == Long.class) value = number.longValue();
        }
        field.setAccessible(true); field.set(result, value);
      }
      return result;
    } catch (ReflectiveOperationException | IllegalArgumentException invalid) { throw denied(); }
  }

  private Binding caller() {
    if (!current()) throw denied();
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(principal instanceof Jwt)) throw denied();
    Jwt jwt = (Jwt) principal;
    if (jwt.getIssuer() == null || jwt.getSubject() == null || jwt.getClaimAsString("azp") == null)
      throw denied();
    return new Binding(jwt.getIssuer().toString(), jwt.getClaimAsString("azp"), jwt.getSubject());
  }

  private static void noAmbientTransaction() {
    if (TransactionSynchronizationManager.isActualTransactionActive()
        || TransactionSynchronizationManager.isSynchronizationActive()) throw denied();
  }

  private void requireSnapshot(Attempt attempt) {
    noAmbientTransaction();
    if (attempt.operation != null) {
      var row = heldJournal.read(attempt.operation, false);
      if (row == null || !row.marker().equals(attempt.marker)
          || !row.snapshotDigest().equals(heldFingerprint(attempt.snapshot))) throw denied();
    }
    if (!attempt.owner.equals(caller())
        || attempt.snapshot == null
        || !attempt.snapshot.equals(freshSnapshot(attempt))) throw denied();
  }

  private Snapshot freshSnapshot(Attempt attempt) {
    Snapshot actual = freshSnapshot(attempt.id, attempt.snapshot.tenant().size(), attempt.snapshot.ledger().size());
    if (!actual.tenant().isEmpty() && (!actual.tenant().keySet().equals(attempt.snapshot.tenant().keySet())
        || !actual.ledger().keySet().equals(attempt.snapshot.ledger().keySet()))) throw denied();
    return actual;
  }

  private Snapshot freshSnapshot(long id, int tenantColumns, int ledgerColumns) {
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.getAutoCommit()) throw denied();
      try (var statement =
          connection.prepareStatement(
              "SELECT t.*, r.* FROM tenant t LEFT JOIN tenant_id_reservation r ON r.tenant_id=t.id WHERE t.id=?")) {
        statement.setQueryTimeout(3);
        statement.setLong(1, id);
        try (ResultSet result = statement.executeQuery()) {
          if (!result.next()) return new Snapshot(Map.of(), Map.of());
          var metadata = result.getMetaData();
          if (metadata.getColumnCount() != tenantColumns + ledgerColumns) throw denied();
          var tenant = new TreeMap<String, Cell>();
          var ledger = new TreeMap<String, Cell>();
          for (int index = 1; index <= metadata.getColumnCount(); index++) {
            Object raw = result.getObject(index);
            String type = raw == null ? "null" : raw.getClass().getName();
            Object scalar = raw;
            if (raw instanceof java.sql.Clob) scalar = result.getString(index);
            else if (raw instanceof byte[]) scalar = HexFormat.of().formatHex((byte[]) raw);
            else if (raw instanceof Timestamp) scalar = ((Timestamp) raw).toLocalDateTime();
            else if (raw instanceof java.sql.Date) scalar = ((java.sql.Date) raw).toLocalDate();
            else if (raw instanceof java.sql.Time) scalar = ((java.sql.Time) raw).toLocalTime();
            (index <= tenantColumns ? tenant : ledger)
                .put(
                    metadata.getColumnName(index).toLowerCase(Locale.ROOT),
                    new Cell(metadata.getColumnType(index), type, scalar));
          }
          if (result.next()) throw denied();
          return new Snapshot(Map.copyOf(tenant), Map.copyOf(ledger));
        }
      }
    } catch (SQLException failure) {
      throw denied();
    }
  }

  private Snapshot transactionalSnapshot(long id, boolean lock) {
    return sql.execute((ConnectionCallback<Snapshot>) connection -> snapshot(connection, id, lock));
  }

  private static Snapshot snapshot(Connection connection, long id, boolean lock)
      throws SQLException {
    return new Snapshot(
        row(connection, "tenant", "id", id, lock),
        row(connection, "tenant_id_reservation", "tenant_id", id, lock));
  }

  private static Map<String, Cell> row(
      Connection connection, String table, String column, long id, boolean lock)
      throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT * FROM " + table + " WHERE " + column + "=?" + (lock ? " FOR UPDATE" : ""))) {
      statement.setQueryTimeout(3);
      statement.setLong(1, id);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) return Map.of();
        var values = new TreeMap<String, Cell>();
        var metadata = result.getMetaData();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
          Object raw = result.getObject(index);
          String type = raw == null ? "null" : raw.getClass().getName();
          Object scalar = raw;
          if (raw instanceof java.sql.Clob) scalar = result.getString(index);
          else if (raw instanceof byte[]) scalar = HexFormat.of().formatHex((byte[]) raw);
          else if (raw instanceof Timestamp) scalar = ((Timestamp) raw).toLocalDateTime();
          else if (raw instanceof java.sql.Date) scalar = ((java.sql.Date) raw).toLocalDate();
          else if (raw instanceof java.sql.Time) scalar = ((java.sql.Time) raw).toLocalTime();
          values.put(
              metadata.getColumnName(index).toLowerCase(Locale.ROOT),
              new Cell(metadata.getColumnType(index), type, scalar));
        }
        if (result.next()) throw new SQLException("Non-unique native ownership row");
        return Map.copyOf(values);
      }
    }
  }

  private void resolveBeforeSend(Attempt attempt, RuntimeException original) {
    if (attempt.snapshot == null) {
      if (attempt.id != null && attempt.completion == TransactionSynchronization.STATUS_UNKNOWN) {
        original.addSuppressed(
            new AccessDeniedException("Owned bootstrap outcome requires reconciliation"));
      }
      return;
    }
    try {
      noAmbientTransaction();
      Snapshot actual = freshSnapshot(attempt);
      if (actual.tenant().isEmpty()) {
        if (attempt.completion != TransactionSynchronization.STATUS_ROLLED_BACK) throw denied();
        return;
      }
      if (!attempt.snapshot.equals(actual)) throw denied();
      transaction.executeWithoutResult(
          status -> {
            if (!attempt.snapshot.equals(transactionalSnapshot(attempt.id, true))) throw denied();
            if (sql.update(
                    "DELETE FROM tenant WHERE id=? AND bootstrap_attempt_id=?",
                    attempt.id,
                    attempt.marker)
                != 1) throw denied();
            Object token = value(attempt.snapshot.ledger(), "token");
            int changed;
            if ("RESERVED".equals(value(attempt.previousLedger, "status"))
                && token.equals(value(attempt.previousLedger, "token"))) {
              changed =
                  sql.update(
                      "UPDATE tenant_id_reservation SET status='RESERVED', update_date=? WHERE tenant_id=? AND status='ASSIGNED' AND token=?",
                      value(attempt.previousLedger, "update_date"),
                      attempt.id,
                      token);
            } else if (attempt.previousLedger.isEmpty()) {
              changed =
                  sql.update(
                      "DELETE FROM tenant_id_reservation WHERE tenant_id=? AND status='ASSIGNED' AND token=?",
                      attempt.id,
                      token);
            } else throw denied();
            if (changed != 1) throw denied();
          });
    } catch (RuntimeException unresolved) {
      original.addSuppressed(
          new AccessDeniedException("Owned bootstrap state retained for reconciliation"));
    }
  }

  private static Object value(Map<String, Cell> row, String name) {
    Cell cell = row.get(name);
    return cell == null ? null : cell.value();
  }

  private static AccessDeniedException denied() {
    return new AccessDeniedException("Live owned tenant creation required");
  }

  private record Binding(String issuer, String client, String subject) {}

  private record Cell(int jdbcType, String javaType, Object value) {
    @Override
    public String toString() {
      return "<native scalar>";
    }
  }

  private record Snapshot(Map<String, Cell> tenant, Map<String, Cell> ledger) {
    @Override
    public String toString() {
      return "<native ownership snapshot>";
    }
  }

  private static final class Attempt {
    private final String marker;
    private final TenantHeldBootstrapJournal.Operation operation;
    private final Binding owner;
    private Long id;
    private Map<String, Cell> previousLedger = Map.of();
    private Snapshot snapshot;
    private int completion = TransactionSynchronization.STATUS_UNKNOWN;
    private boolean sendStarted;

    private Attempt(Binding owner, String marker, TenantHeldBootstrapJournal.Operation operation) {
      if (!UUID.fromString(marker).toString().equals(marker)) throw denied();
      this.owner = owner; this.marker = marker; this.operation = operation;
    }
  }

  /** Cannot be constructed from a public ID, entity, DTO or persisted marker. */
  public static final class Grant {
    private final TenantBootstrapCreation authority;
    private final Attempt attempt;
    private final Thread thread = Thread.currentThread();
    private final long started = System.nanoTime();
    private final long expires = Instant.now().getEpochSecond() + 60;
    private boolean active = true;
    private String payloadDigest;
    private com.vi.tenantservice.api.service.consultingtype.ConsultingTypeService.BootstrapSuccess
        remote;

    private Grant(TenantBootstrapCreation authority, Attempt attempt) {
      this.authority = authority;
      this.attempt = attempt;
    }

    private void requireLive() {
      if (!active
          || Thread.currentThread() != thread
          || System.nanoTime() - started >= 60_000_000_000L
          || Instant.now().getEpochSecond() >= expires) throw denied();
      authority.requireSnapshot(attempt);
    }

    public void createDefaultConsultingTypes() {
      requireLive();
      if (attempt.sendStarted || remote != null) throw denied();
      remote = authority.consultingTypes.createDefaultConsultingTypes(this);
      if (attempt.operation != null) authority.transaction.executeWithoutResult(status -> {
        if (!attempt.snapshot.equals(authority.transactionalSnapshot(attempt.id, true))) throw denied();
        authority.heldJournal.stage(attempt.operation, "CTS_VERIFIED");
      });
    }

    public byte[] frozenBootstrapCommand() {
      requireLive();
      return attempt.operation == null ? null : attempt.operation.command.clone();
    }

    public long tenantId() {
      requireLive();
      return attempt.id;
    }

    public Map<String, Object> claims() {
      requireLive();
      return Map.of(
          "tenantId",
          attempt.id,
          "azp",
          attempt.owner.client(),
          "sub",
          attempt.owner.subject(),
          "tokenIssuer",
          attempt.owner.issuer(),
          "nonce",
          attempt.marker.replace("-", ""),
          "iat",
          Instant.now().getEpochSecond(),
          "exp",
          expires);
    }

    public void bindPayload(String digest) {
      requireLive();
      if (digest == null
          || !digest.matches("[a-f0-9]{64}")
          || (payloadDigest != null && !payloadDigest.equals(digest))) throw denied();
      if (attempt.operation != null) {
        try {
          String frozen = TaskContextProof.digest(TaskContextProof.key(authority.environment),
              TenantHeldBootstrapJournal.JSON.readTree(attempt.operation.command));
          if (!digest.equals(frozen)) throw denied();
        } catch (java.io.IOException invalid) { throw denied(); }
        catch (Exception invalid) { throw denied(); }
      }
      payloadDigest = digest;
    }

    public void beginSend() {
      requireLive();
      if (payloadDigest == null) throw denied();
      if (attempt.operation != null) authority.transaction.executeWithoutResult(status -> {
        if (!attempt.snapshot.equals(authority.transactionalSnapshot(attempt.id, true))) throw denied();
        authority.heldJournal.stage(attempt.operation, "CTS_PENDING");
      });
      attempt.sendStarted = true;
    }

    @Override
    public String toString() {
      return "<scoped tenant creation grant>";
    }
  }
}
