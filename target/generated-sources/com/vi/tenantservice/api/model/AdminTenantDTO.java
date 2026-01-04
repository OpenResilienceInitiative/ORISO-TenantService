package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * AdminTenantDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class AdminTenantDTO {

  private Long id = null;

  private String name;

  private String subdomain;

  private Integer beraterCount;

  @Valid
  private List<String> adminEmails;

  private String createDate;

  private String updateDate;

  /**
   * Default constructor
   * @deprecated Use {@link AdminTenantDTO#AdminTenantDTO(Long, String, String)}
   */
  @Deprecated
  public AdminTenantDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AdminTenantDTO(Long id, String name, String subdomain) {
    this.id = id;
    this.name = name;
    this.subdomain = subdomain;
  }

  public AdminTenantDTO id(Long id) {
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

  public AdminTenantDTO name(String name) {
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

  public AdminTenantDTO subdomain(String subdomain) {
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

  public AdminTenantDTO beraterCount(Integer beraterCount) {
    this.beraterCount = beraterCount;
    return this;
  }

  /**
   * Get beraterCount
   * @return beraterCount
  */
  
  @Schema(name = "beraterCount", example = "10", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("beraterCount")
  public Integer getBeraterCount() {
    return beraterCount;
  }

  public void setBeraterCount(Integer beraterCount) {
    this.beraterCount = beraterCount;
  }

  public AdminTenantDTO adminEmails(List<String> adminEmails) {
    this.adminEmails = adminEmails;
    return this;
  }

  public AdminTenantDTO addAdminEmailsItem(String adminEmailsItem) {
    if (this.adminEmails == null) {
      this.adminEmails = new ArrayList<>();
    }
    this.adminEmails.add(adminEmailsItem);
    return this;
  }

  /**
   * Get adminEmails
   * @return adminEmails
  */
  
  @Schema(name = "adminEmails", example = "[admin1@ob.de, admin2@ob.de]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("adminEmails")
  public List<String> getAdminEmails() {
    return adminEmails;
  }

  public void setAdminEmails(List<String> adminEmails) {
    this.adminEmails = adminEmails;
  }

  public AdminTenantDTO createDate(String createDate) {
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

  public AdminTenantDTO updateDate(String updateDate) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AdminTenantDTO adminTenantDTO = (AdminTenantDTO) o;
    return Objects.equals(this.id, adminTenantDTO.id) &&
        Objects.equals(this.name, adminTenantDTO.name) &&
        Objects.equals(this.subdomain, adminTenantDTO.subdomain) &&
        Objects.equals(this.beraterCount, adminTenantDTO.beraterCount) &&
        Objects.equals(this.adminEmails, adminTenantDTO.adminEmails) &&
        Objects.equals(this.createDate, adminTenantDTO.createDate) &&
        Objects.equals(this.updateDate, adminTenantDTO.updateDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, subdomain, beraterCount, adminEmails, createDate, updateDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AdminTenantDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    subdomain: ").append(toIndentedString(subdomain)).append("\n");
    sb.append("    beraterCount: ").append(toIndentedString(beraterCount)).append("\n");
    sb.append("    adminEmails: ").append(toIndentedString(adminEmails)).append("\n");
    sb.append("    createDate: ").append(toIndentedString(createDate)).append("\n");
    sb.append("    updateDate: ").append(toIndentedString(updateDate)).append("\n");
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

