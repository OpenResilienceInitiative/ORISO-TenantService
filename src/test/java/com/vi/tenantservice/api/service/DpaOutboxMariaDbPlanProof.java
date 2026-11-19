package com.vi.tenantservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.service.DpaAdminNoticeOutboxService.PendingEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.jdbc.core.JdbcTemplate;

/** Measures the real Hibernate queries on the lock-ordering fixture's disposable MariaDB. */
public final class DpaOutboxMariaDbPlanProof {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final long HISTORY_START = 10_000_000L;
  private static final long QUEUE_START = HISTORY_START + 100_001;
  private static final long SIGNED_AT = 1_763_546_400_123L;
  private static final List<Long> CURRENT =
      List.of(QUEUE_START + 4, QUEUE_START + 5, QUEUE_START + 6);
  private static final Pattern SIGNATURE_ALIAS =
      Pattern.compile(
          "\\bfrom\\s+(?:`?\\w+`?\\.)?`?tenant_dpa_signature`?\\s+(?:as\\s+)?`?(\\w+)`?",
          Pattern.CASE_INSENSITIVE);

  private final JdbcTemplate jdbc;
  private final DpaAdminNoticeOutboxService outbox;
  private final long tenantId;
  private final String currentBinding;

  DpaOutboxMariaDbPlanProof(
      JdbcTemplate jdbc,
      DpaAdminNoticeOutboxService outbox,
      long tenantId,
      String reservationToken) {
    this.jdbc = jdbc;
    this.outbox = outbox;
    this.tenantId = tenantId;
    currentBinding = DpaSignToken.hash(reservationToken);
  }

  void run() throws Exception {
    String version = jdbc.queryForObject("SELECT VERSION()", String.class);
    assertThat(version).startsWith("10.11.18-MariaDB");
    var indexes =
        jdbc.queryForList(
            """
        select index_name, group_concat(column_name order by seq_in_index) as index_columns
        from information_schema.statistics
        where table_schema = database() and table_name = 'tenant_dpa_signature'
        group by index_name order by index_name
        """);
    print("DPA_OUTBOX_MARIADB_SCHEMA", Map.of("version", version, "indexes", indexes));

    seedHistory(1, 10_000);
    var small = measure(10_000);
    jdbc.update(
        "delete from tenant_dpa_signature where tenant_id = ? and id >= ?", tenantId, QUEUE_START);
    seedHistory(10_001, 100_000);
    var large = measure(100_000);
    var comparison = new LinkedHashMap<String, RowWork>();
    for (String scenario : small.keySet()) {
      var tenThousand = small.get(scenario);
      var hundredThousand = large.get(scenario);
      comparison.put(scenario, new RowWork(tenThousand, hundredThousand));
    }
    // Correct page/cursor/owner behavior is asserted. Baseline cost remains observable even when
    // it grows with historical rows, so the proof can inform an index decision without masking it.
    print("DPA_OUTBOX_MARIADB_HISTORY_COMPARISON", comparison);
  }

  private Map<String, Double> measure(int historyRows) throws Exception {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from tenant_dpa_signature where tenant_id = ? and id < ?",
                Integer.class,
                tenantId,
                QUEUE_START))
        .isEqualTo(historyRows);
    jdbc.execute("ANALYZE TABLE tenant_dpa_signature");
    Map<String, Double> work = new LinkedHashMap<>();
    work.put("empty", poll(historyRows, "empty", 0, 50, 0, List.of(), null));
    work.put("acknowledged-key", exact(historyRows, "acknowledged-key", HISTORY_START + 2, true));
    work.put("unstaged-key", exact(historyRows, "unstaged-key", HISTORY_START + 1, false));

    for (long id : CURRENT) seedPending(id, currentBinding);
    jdbc.execute("ANALYZE TABLE tenant_dpa_signature");
    work.put("sparse", poll(historyRows, "sparse", 0, 50, CURRENT.size(), CURRENT, null));
    work.put("pending-key", exact(historyRows, "pending-key", CURRENT.getFirst(), true));

    String previousBinding = DpaSignToken.hash("previous-synthetic-reservation");
    for (long id = QUEUE_START; id < CURRENT.getFirst(); id++) seedPending(id, previousBinding);
    jdbc.execute("ANALYZE TABLE tenant_dpa_signature");
    long afterStale = CURRENT.getFirst() - 1;
    work.put(
        "stale-first-page", poll(historyRows, "stale-first-page", 0, 4, 7, List.of(), afterStale));
    work.put("after-stale", poll(historyRows, "after-stale", afterStale, 4, 7, CURRENT, null));
    work.put("stale-key", exact(historyRows, "stale-key", QUEUE_START, false));
    return work;
  }

  private Double poll(
      int history,
      String scenario,
      long afterId,
      int size,
      int staged,
      List<Long> expectedIds,
      Long next)
      throws Exception {
    var captured = capture(() -> outbox.readPending(afterId, size));
    var page = captured.result();
    assertThat(page.events()).extracting(PendingEvent::id).containsExactlyElementsOf(expectedIds);
    assertThat(page.events()).allSatisfy(event -> assertThat(event.tenantId()).isEqualTo(tenantId));
    assertThat(page.nextAfterId()).isEqualTo(next);
    return explain(
        history,
        scenario,
        captured.sql(),
        List.of(afterId, size + 1),
        Map.of(
            "stagedRows",
            staged,
            "pageLimit",
            size,
            "returnedIds",
            expectedIds,
            "hasContinuation",
            next != null));
  }

  private Double exact(int history, String scenario, long id, boolean visible) throws Exception {
    var captured = capture(() -> outbox.readExact(id));
    assertThat(captured.result().isPresent()).isEqualTo(visible);
    captured
        .result()
        .ifPresent(
            row -> {
              assertThat(row.id()).isEqualTo(id);
              assertThat(row.tenantId()).isEqualTo(tenantId);
              assertThat(row.signedAtEpochMillis()).isEqualTo(SIGNED_AT);
            });
    return explain(history, scenario, captured.sql(), List.of(id), Map.of("visible", visible));
  }

  private Double explain(
      int history, String scenario, String sql, List<?> bindings, Map<String, ?> observed)
      throws Exception {
    assertThat(sql).startsWithIgnoringCase("select");
    assertThat(sql.chars().filter(ch -> ch == '?').count())
        .as("Hibernate binding count: %s", sql)
        .isEqualTo(bindings.size());
    var alias = SIGNATURE_ALIAS.matcher(sql);
    assertThat(alias.find()).as("Hibernate signature-table alias: %s", sql).isTrue();
    JsonNode estimated = plan("EXPLAIN", sql, bindings);
    JsonNode executed = plan("ANALYZE", sql, bindings);
    var signaturePlan = table(executed, alias.group(1));
    var estimatedTable = table(estimated, alias.group(1));
    boolean counted =
        signaturePlan.path("r_rows").isNumber() && signaturePlan.path("r_loops").isNumber();
    if (bindings.size() > 1) {
      assertThat(counted).as("actual signature rows for a page scan: %s", executed).isTrue();
    }
    // MariaDB can evaluate a constant primary-key row during optimization. Preserve the estimate
    // or optimizer message where ANALYZE does not report runtime reads for that lookup.
    Double signatureRows =
        counted
            ? signaturePlan.path("r_rows").asDouble() * signaturePlan.path("r_loops").asDouble()
            : null;
    print(
        "DPA_OUTBOX_MARIADB_PLAN",
        new PlanEvidence(
            history,
            scenario,
            sql,
            bindings,
            estimatedTable.path("access_type").asText(),
            estimatedTable.path("key").asText(),
            signatureRows,
            executed.findValue("filesort") != null,
            observed,
            estimated,
            executed));
    return signatureRows;
  }

  private JsonNode plan(String command, String sql, List<?> bindings) throws Exception {
    return JSON.readTree(
        jdbc.queryForObject(command + " FORMAT=JSON " + sql, String.class, bindings.toArray()));
  }

  private static JsonNode table(JsonNode plan, String alias) {
    return plan.findValues("table").stream()
        .filter(node -> node.path("table_name").asText().equals(alias))
        .findFirst()
        .orElseGet(JSON::missingNode);
  }

  private void seedHistory(int first, int last) {
    assertThat(
            jdbc.update(
                """
        insert into tenant_dpa_signature
          (id, tenant_id, signature_status, source, create_date, signed_at, signed_at_epoch_millis,
           admin_notice_handoff_signed_at_epoch_millis, reservation_token_hash)
        select ? + seq, ?, 'SIGNED', 'FORWARDED_EXTERNAL', current_timestamp, current_timestamp, ?,
          case when mod(seq, 2) = 0 then ? else null end, ?
        from seq_1_to_100000 where seq between ? and ?
        """,
                HISTORY_START,
                tenantId,
                SIGNED_AT,
                SIGNED_AT,
                currentBinding,
                first,
                last))
        .isEqualTo(last - first + 1);
  }

  private void seedPending(long id, String binding) {
    assertThat(
            jdbc.update(
                """
        insert into tenant_dpa_signature
          (id, tenant_id, signature_status, source, create_date, signed_at, signed_at_epoch_millis,
           admin_notice_pending_since_epoch_millis, reservation_token_hash)
        values (?, ?, 'SIGNED', 'FORWARDED_EXTERNAL', current_timestamp, current_timestamp, ?, ?, ?)
        """,
                id,
                tenantId,
                SIGNED_AT,
                SIGNED_AT,
                binding))
        .isEqualTo(1);
  }

  private static <T> Captured<T> capture(Supplier<T> query) {
    var statements = new ArrayList<String>();
    SqlCapture.active.set(statements);
    try {
      T result = query.get();
      assertThat(statements).as("one Hibernate SELECT including ownership context").hasSize(1);
      return new Captured<>(result, statements.getFirst());
    } finally {
      SqlCapture.active.remove();
    }
  }

  private static void print(String label, Object value) throws Exception {
    System.out.println(label + " " + JSON.writeValueAsString(value));
  }

  public static final class SqlCapture implements StatementInspector {
    private static final ThreadLocal<List<String>> active = new ThreadLocal<>();

    @Override
    public String inspect(String sql) {
      var statements = active.get();
      if (statements != null) statements.add(sql);
      return sql;
    }
  }

  private record Captured<T>(T result, String sql) {}

  private record RowWork(Double rowsAt10kHistory, Double rowsAt100kHistory) {}

  private record PlanEvidence(
      int historyRows,
      String scenario,
      String sql,
      List<?> bindings,
      String signatureAccess,
      String signatureKey,
      Double actualSignatureRows,
      boolean filesort,
      Map<String, ?> observed,
      JsonNode explain,
      JsonNode analyze) {}
}
