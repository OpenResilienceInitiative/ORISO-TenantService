package com.vi.tenantservice.api.model;

/**
 * A Träger as the platform statistics panel names it (ORISO-UserService#1666): id and name only, so
 * the Statistik-Leser learns nothing else about the tenant.
 */
public record TenantStatisticsNameDTO(Long id, String name) {}
