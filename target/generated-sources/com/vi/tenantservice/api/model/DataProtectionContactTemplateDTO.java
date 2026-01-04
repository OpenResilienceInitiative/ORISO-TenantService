package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.AgencyContextDTO;
import com.vi.tenantservice.api.model.NoAgencyContextDTO;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DataProtectionContactTemplateDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class DataProtectionContactTemplateDTO {

  private AgencyContextDTO agencyContext;

  private NoAgencyContextDTO noAgencyContext;

  public DataProtectionContactTemplateDTO agencyContext(AgencyContextDTO agencyContext) {
    this.agencyContext = agencyContext;
    return this;
  }

  /**
   * Get agencyContext
   * @return agencyContext
  */
  @Valid 
  @Schema(name = "agencyContext", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("agencyContext")
  public AgencyContextDTO getAgencyContext() {
    return agencyContext;
  }

  public void setAgencyContext(AgencyContextDTO agencyContext) {
    this.agencyContext = agencyContext;
  }

  public DataProtectionContactTemplateDTO noAgencyContext(NoAgencyContextDTO noAgencyContext) {
    this.noAgencyContext = noAgencyContext;
    return this;
  }

  /**
   * Get noAgencyContext
   * @return noAgencyContext
  */
  @Valid 
  @Schema(name = "noAgencyContext", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("noAgencyContext")
  public NoAgencyContextDTO getNoAgencyContext() {
    return noAgencyContext;
  }

  public void setNoAgencyContext(NoAgencyContextDTO noAgencyContext) {
    this.noAgencyContext = noAgencyContext;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataProtectionContactTemplateDTO dataProtectionContactTemplateDTO = (DataProtectionContactTemplateDTO) o;
    return Objects.equals(this.agencyContext, dataProtectionContactTemplateDTO.agencyContext) &&
        Objects.equals(this.noAgencyContext, dataProtectionContactTemplateDTO.noAgencyContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(agencyContext, noAgencyContext);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataProtectionContactTemplateDTO {\n");
    sb.append("    agencyContext: ").append(toIndentedString(agencyContext)).append("\n");
    sb.append("    noAgencyContext: ").append(toIndentedString(noAgencyContext)).append("\n");
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

