package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * NoAgencyContextDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class NoAgencyContextDTO {

  private String responsibleContact;

  private String dataProtectionOfficerContact;

  public NoAgencyContextDTO responsibleContact(String responsibleContact) {
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

  public NoAgencyContextDTO dataProtectionOfficerContact(String dataProtectionOfficerContact) {
    this.dataProtectionOfficerContact = dataProtectionOfficerContact;
    return this;
  }

  /**
   * Get dataProtectionOfficerContact
   * @return dataProtectionOfficerContact
  */
  
  @Schema(name = "dataProtectionOfficerContact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dataProtectionOfficerContact")
  public String getDataProtectionOfficerContact() {
    return dataProtectionOfficerContact;
  }

  public void setDataProtectionOfficerContact(String dataProtectionOfficerContact) {
    this.dataProtectionOfficerContact = dataProtectionOfficerContact;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NoAgencyContextDTO noAgencyContextDTO = (NoAgencyContextDTO) o;
    return Objects.equals(this.responsibleContact, noAgencyContextDTO.responsibleContact) &&
        Objects.equals(this.dataProtectionOfficerContact, noAgencyContextDTO.dataProtectionOfficerContact);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responsibleContact, dataProtectionOfficerContact);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NoAgencyContextDTO {\n");
    sb.append("    responsibleContact: ").append(toIndentedString(responsibleContact)).append("\n");
    sb.append("    dataProtectionOfficerContact: ").append(toIndentedString(dataProtectionOfficerContact)).append("\n");
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

