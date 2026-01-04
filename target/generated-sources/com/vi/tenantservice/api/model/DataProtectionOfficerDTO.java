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
 * DataProtectionOfficerDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class DataProtectionOfficerDTO {

  private String agencyResponsibleContact;

  private String alternativeRepresentativeContact;

  private String dataProtectionOfficerContact;

  public DataProtectionOfficerDTO agencyResponsibleContact(String agencyResponsibleContact) {
    this.agencyResponsibleContact = agencyResponsibleContact;
    return this;
  }

  /**
   * Get agencyResponsibleContact
   * @return agencyResponsibleContact
  */
  
  @Schema(name = "agencyResponsibleContact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("agencyResponsibleContact")
  public String getAgencyResponsibleContact() {
    return agencyResponsibleContact;
  }

  public void setAgencyResponsibleContact(String agencyResponsibleContact) {
    this.agencyResponsibleContact = agencyResponsibleContact;
  }

  public DataProtectionOfficerDTO alternativeRepresentativeContact(String alternativeRepresentativeContact) {
    this.alternativeRepresentativeContact = alternativeRepresentativeContact;
    return this;
  }

  /**
   * Get alternativeRepresentativeContact
   * @return alternativeRepresentativeContact
  */
  
  @Schema(name = "alternativeRepresentativeContact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("alternativeRepresentativeContact")
  public String getAlternativeRepresentativeContact() {
    return alternativeRepresentativeContact;
  }

  public void setAlternativeRepresentativeContact(String alternativeRepresentativeContact) {
    this.alternativeRepresentativeContact = alternativeRepresentativeContact;
  }

  public DataProtectionOfficerDTO dataProtectionOfficerContact(String dataProtectionOfficerContact) {
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
    DataProtectionOfficerDTO dataProtectionOfficerDTO = (DataProtectionOfficerDTO) o;
    return Objects.equals(this.agencyResponsibleContact, dataProtectionOfficerDTO.agencyResponsibleContact) &&
        Objects.equals(this.alternativeRepresentativeContact, dataProtectionOfficerDTO.alternativeRepresentativeContact) &&
        Objects.equals(this.dataProtectionOfficerContact, dataProtectionOfficerDTO.dataProtectionOfficerContact);
  }

  @Override
  public int hashCode() {
    return Objects.hash(agencyResponsibleContact, alternativeRepresentativeContact, dataProtectionOfficerContact);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataProtectionOfficerDTO {\n");
    sb.append("    agencyResponsibleContact: ").append(toIndentedString(agencyResponsibleContact)).append("\n");
    sb.append("    alternativeRepresentativeContact: ").append(toIndentedString(alternativeRepresentativeContact)).append("\n");
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

