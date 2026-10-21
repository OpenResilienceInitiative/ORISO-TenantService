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
  @PersistenceContext private EntityManager entities;

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
    noAmbientTransaction();
    Binding owner = caller();
    TaskContextProof.key(environment);
    boolean auto = input.getId() == null;
    int attempts = auto ? TenantIdAllocationService.MAX_AUTO_ATTEMPTS : 1;
    TenantIdAllocationConflictException conflict = null;
    for (int index = 0; index < attempts; index++) {
      Attempt attempt = new Attempt(owner);
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
                  return fresh;
                });
        if (attempt.completion != TransactionSynchronization.STATUS_COMMITTED) throw denied();
        requireSnapshot(attempt);
        Grant grant = new Grant(this, attempt);
        try {
          return callback.apply(created, grant);
        } finally {
          grant.active = false;
        }
      } catch (RuntimeException failure) {
        if (!attempt.sendStarted) resolveBeforeSend(attempt, failure);
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
    if (!attempt.owner.equals(caller())
        || attempt.snapshot == null
        || !attempt.snapshot.equals(freshSnapshot(attempt))) throw denied();
  }

  private Snapshot freshSnapshot(Attempt attempt) {
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.getAutoCommit()) throw denied();
      try (var statement =
          connection.prepareStatement(
              "SELECT t.*, r.* FROM tenant t LEFT JOIN tenant_id_reservation r ON r.tenant_id=t.id WHERE t.id=?")) {
        statement.setQueryTimeout(3);
        statement.setLong(1, attempt.id);
        try (ResultSet result = statement.executeQuery()) {
          if (!result.next()) return new Snapshot(Map.of(), Map.of());
          int tenantColumns = attempt.snapshot.tenant().size();
          int ledgerColumns = attempt.snapshot.ledger().size();
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
          if (!tenant.keySet().equals(attempt.snapshot.tenant().keySet())
              || !ledger.keySet().equals(attempt.snapshot.ledger().keySet())
              || result.next()) throw denied();
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
    private final String marker = UUID.randomUUID().toString();
    private final Binding owner;
    private Long id;
    private Map<String, Cell> previousLedger = Map.of();
    private Snapshot snapshot;
    private int completion = TransactionSynchronization.STATUS_UNKNOWN;
    private boolean sendStarted;

    private Attempt(Binding owner) {
      this.owner = owner;
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
      payloadDigest = digest;
    }

    public void beginSend() {
      requireLive();
      if (payloadDigest == null) throw denied();
      attempt.sendStarted = true;
    }

    @Override
    public String toString() {
      return "<scoped tenant creation grant>";
    }
  }
}
