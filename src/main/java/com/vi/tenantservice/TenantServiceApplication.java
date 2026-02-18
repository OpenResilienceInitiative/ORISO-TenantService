package com.vi.tenantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starter class for the application. */
@SpringBootApplication
public class TenantServiceApplication {

  /**
   * Global application entry point.
   *
   * @param args possible provided args
   */
  public static void main(String[] args) {
    SpringApplication.run(TenantServiceApplication.class, args);
  }
}
