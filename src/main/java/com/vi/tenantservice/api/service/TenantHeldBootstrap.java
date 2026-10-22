package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vi.tenantservice.api.model.MultilingualTenantDTO;
import org.springframework.stereotype.Service;

/** Fixed held-bootstrap operation; a public selector never constructs creation authority. */
@Service
public class TenantHeldBootstrap {
  public Result create(JsonNode command) {
    return null;
  }

  public record Result(MultilingualTenantDTO tenant, String receipt) {
    @Override public String toString() { return "HeldTenantBootstrapResult[redacted]"; }
  }
}
