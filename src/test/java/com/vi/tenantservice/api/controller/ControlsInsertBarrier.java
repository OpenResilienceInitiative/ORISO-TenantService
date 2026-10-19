package com.vi.tenantservice.api.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.h2.api.Trigger;

/** Test-database fixture: both real inserts must arrive before either can finish. */
public final class ControlsInsertBarrier implements Trigger {
  static volatile CyclicBarrier barrier;

  @Override
  public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
    try {
      barrier.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new SQLException(
          "Interrupted while waiting for overlapping controls inserts", interrupted);
    } catch (Exception failure) {
      throw new SQLException("Both first controls inserts must overlap", failure);
    }
  }
}
