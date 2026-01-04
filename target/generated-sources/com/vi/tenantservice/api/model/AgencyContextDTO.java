package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.DataProtectionOfficerDTO;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AgencyContextDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class AgencyContextDTO {

  private String responsibleContact;

  private DataProtectionOfficerDTO dataProtectionOfficer;

  public AgencyContextDTO responsibleContact(String responsibleContact) {
    this.responsibleContact = responsibleContact;
    return this;
  }

  /**
   * Get responsibleContact
   * @return responsibleContact
  */
  
  @Schema(name = "responsibleContact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("responsibleContact")
  public String getResponsibleContact() {
    return responsibleContact;
  }

  public void setResponsibleContact(String responsibleContact) {
    this.responsibleContact = responsibleContact;
  }

  public AgencyContextDTO dataProtectionOfficer(DataProtectionOfficerDTO dataProtectionOfficer) {
    this.dataProtectionOfficer = dataProtectionOfficer;
    return this;
  }

  /**
   * Get dataProtectionOfficer
   * @return dataProtectionOfficer
  */
  @Valid 
  @Schema(name = "dataProtectionOfficer", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dataProtectionOfficer")
  public DataProtectionOfficerDTO getDataProtectionOfficer() {
    return dataProtectionOfficer;
  }

  public void setDataProtectionOfficer(DataProtectionOfficerDTO dataProtectionOfficer) {
    this.dataProtectionOfficer = dataProtectionOfficer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AgencyContextDTO agencyContextDTO = (AgencyContextDTO) o;
    return Objects.equals(this.responsibleContact, agencyContextDTO.responsibleContact) &&
        Objects.equals(this.dataProtectionOfficer, agencyContextDTO.dataProtectionOfficer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responsibleContact, dataProtectionOfficer);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AgencyContextDTO {\n");
    sb.append("    responsibleContact: ").append(toIndentedString(responsibleContact)).append("\n");
    sb.append("    dataProtectionOfficer: ").append(toIndentedString(dataProtectionOfficer)).append("\n");
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

