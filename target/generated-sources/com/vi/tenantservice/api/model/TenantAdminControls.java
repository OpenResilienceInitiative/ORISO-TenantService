package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.TenantAdminAllowedPermissionToggles;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TenantAdminControls
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-11T15:41:30.641955926Z[Etc/UTC]")
public class TenantAdminControls {

  private Boolean permissionsPageEnabled;

  private TenantAdminAllowedPermissionToggles allowedPermissionToggles;

  public TenantAdminControls permissionsPageEnabled(Boolean permissionsPageEnabled) {
    this.permissionsPageEnabled = permissionsPageEnabled;
    return this;
  }

  /**
   * Get permissionsPageEnabled
   * @return permissionsPageEnabled
  */
  
  @Schema(name = "permissionsPageEnabled", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("permissionsPageEnabled")
  public Boolean getPermissionsPageEnabled() {
    return permissionsPageEnabled;
  }

  public void setPermissionsPageEnabled(Boolean permissionsPageEnabled) {
    this.permissionsPageEnabled = permissionsPageEnabled;
  }

  public TenantAdminControls allowedPermissionToggles(TenantAdminAllowedPermissionToggles allowedPermissionToggles) {
    this.allowedPermissionToggles = allowedPermissionToggles;
    return this;
  }

  /**
   * Get allowedPermissionToggles
   * @return allowedPermissionToggles
  */
  @Valid 
  @Schema(name = "allowedPermissionToggles", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("allowedPermissionToggles")
  public TenantAdminAllowedPermissionToggles getAllowedPermissionToggles() {
    return allowedPermissionToggles;
  }

  public void setAllowedPermissionToggles(TenantAdminAllowedPermissionToggles allowedPermissionToggles) {
    this.allowedPermissionToggles = allowedPermissionToggles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TenantAdminControls tenantAdminControls = (TenantAdminControls) o;
    return Objects.equals(this.permissionsPageEnabled, tenantAdminControls.permissionsPageEnabled) &&
        Objects.equals(this.allowedPermissionToggles, tenantAdminControls.allowedPermissionToggles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permissionsPageEnabled, allowedPermissionToggles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TenantAdminControls {\n");
    sb.append("    permissionsPageEnabled: ").append(toIndentedString(permissionsPageEnabled)).append("\n");
    sb.append("    allowedPermissionToggles: ").append(toIndentedString(allowedPermissionToggles)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

