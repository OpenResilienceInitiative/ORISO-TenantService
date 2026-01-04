package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.Licensing;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * BasicTenantLicensingDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class BasicTenantLicensingDTO {

  private Long id = null;

  private String name;

  private String subdomain;

  private String createDate;

  private String updateDate;

  private Licensing licensing;

  /**
   * Default constructor
   * @deprecated Use {@link BasicTenantLicensingDTO#BasicTenantLicensingDTO(Long, String, String)}
   */
  @Deprecated
  public BasicTenantLicensingDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BasicTenantLicensingDTO(Long id, String name, String subdomain) {
    this.id = id;
    this.name = name;
    this.subdomain = subdomain;
  }

  public BasicTenantLicensingDTO id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  @NotNull 
  @Schema(name = "id", example = "12132", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public BasicTenantLicensingDTO name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  @NotNull @Size(max = 40) 
  @Schema(name = "name", example = "Company name AG", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BasicTenantLicensingDTO subdomain(String subdomain) {
    this.subdomain = subdomain;
    return this;
  }

  /**
   * Get subdomain
   * @return subdomain
  */
  @NotNull @Size(max = 100) 
  @Schema(name = "subdomain", example = "companyname", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("subdomain")
  public String getSubdomain() {
    return subdomain;
  }

  public void setSubdomain(String subdomain) {
    this.subdomain = subdomain;
  }

  public BasicTenantLicensingDTO createDate(String createDate) {
    this.createDate = createDate;
    return this;
  }

  /**
   * Get createDate
   * @return createDate
  */
  
  @Schema(name = "createDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createDate")
  public String getCreateDate() {
    return createDate;
  }

  public void setCreateDate(String createDate) {
    this.createDate = createDate;
  }

  public BasicTenantLicensingDTO updateDate(String updateDate) {
    this.updateDate = updateDate;
    return this;
  }

  /**
   * Get updateDate
   * @return updateDate
  */
  
  @Schema(name = "updateDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("updateDate")
  public String getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(String updateDate) {
    this.updateDate = updateDate;
  }

  public BasicTenantLicensingDTO licensing(Licensing licensing) {
    this.licensing = licensing;
    return this;
  }

  /**
   * Get licensing
   * @return licensing
  */
  @Valid 
  @Schema(name = "licensing", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("licensing")
  public Licensing getLicensing() {
    return licensing;
  }

  public void setLicensing(Licensing licensing) {
    this.licensing = licensing;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BasicTenantLicensingDTO basicTenantLicensingDTO = (BasicTenantLicensingDTO) o;
    return Objects.equals(this.id, basicTenantLicensingDTO.id) &&
        Objects.equals(this.name, basicTenantLicensingDTO.name) &&
        Objects.equals(this.subdomain, basicTenantLicensingDTO.subdomain) &&
        Objects.equals(this.createDate, basicTenantLicensingDTO.createDate) &&
        Objects.equals(this.updateDate, basicTenantLicensingDTO.updateDate) &&
        Objects.equals(this.licensing, basicTenantLicensingDTO.licensing);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, subdomain, createDate, updateDate, licensing);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BasicTenantLicensingDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    subdomain: ").append(toIndentedString(subdomain)).append("\n");
    sb.append("    createDate: ").append(toIndentedString(createDate)).append("\n");
    sb.append("    updateDate: ").append(toIndentedString(updateDate)).append("\n");
    sb.append("    licensing: ").append(toIndentedString(licensing)).append("\n");
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

