package com.vi.tenantservice.api.model;

@com.fasterxml.jackson.annotation.JsonInclude(
    com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
public record AccountProvisioningPolicy(Long id, Integer allowedNumberOfUsers) {}
