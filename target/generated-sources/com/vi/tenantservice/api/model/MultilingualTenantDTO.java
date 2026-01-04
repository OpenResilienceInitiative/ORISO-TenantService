package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.MultilingualContent;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.Theming;
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
 * MultilingualTenantDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class MultilingualTenantDTO {

  private Long id = null;

  private String name;

  private String subdomain;

  @Valid
  private List<String> adminEmails;

  private String createDate;

  private String updateDate;

  private Licensing licensing;

  private Theming theming;

  private MultilingualContent content;

  private Settings settings;

  /**
   * Default constructor
   * @deprecated Use {@link MultilingualTenantDTO#MultilingualTenantDTO(String)}
   */
  @Deprecated
  public MultilingualTenantDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MultilingualTenantDTO(String name) {
    this.name = name;
  }

  public MultilingualTenantDTO id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  
  @Schema(name = "id", example = "12132", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public MultilingualTenantDTO name(String name) {
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

  public MultilingualTenantDTO subdomain(String subdomain) {
    this.subdomain = subdomain;
    return this;
  }

  /**
   * Get subdomain
   * @return subdomain
  */
  @Size(max = 100) 
  @Schema(name = "subdomain", example = "companyname", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("subdomain")
  public String getSubdomain() {
    return subdomain;
  }

  public void setSubdomain(String subdomain) {
    this.subdomain = subdomain;
  }

  public MultilingualTenantDTO adminEmails(List<String> adminEmails) {
    this.adminEmails = adminEmails;
    return this;
  }

  public MultilingualTenantDTO addAdminEmailsItem(String adminEmailsItem) {
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

  public MultilingualTenantDTO createDate(String createDate) {
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

  public MultilingualTenantDTO updateDate(String updateDate) {
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

  public MultilingualTenantDTO licensing(Licensing licensing) {
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

  public MultilingualTenantDTO theming(Theming theming) {
    this.theming = theming;
    return this;
  }

  /**
   * Get theming
   * @return theming
  */
  @Valid 
  @Schema(name = "theming", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("theming")
  public Theming getTheming() {
    return theming;
  }

  public void setTheming(Theming theming) {
    this.theming = theming;
  }

  public MultilingualTenantDTO content(MultilingualContent content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
  */
  @Valid 
  @Schema(name = "content", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("content")
  public MultilingualContent getContent() {
    return content;
  }

  public void setContent(MultilingualContent content) {
    this.content = content;
  }

  public MultilingualTenantDTO settings(Settings settings) {
    this.settings = settings;
    return this;
  }

  /**
   * Get settings
   * @return settings
  */
  @Valid 
  @Schema(name = "settings", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("settings")
  public Settings getSettings() {
    return settings;
  }

  public void setSettings(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MultilingualTenantDTO multilingualTenantDTO = (MultilingualTenantDTO) o;
    return Objects.equals(this.id, multilingualTenantDTO.id) &&
        Objects.equals(this.name, multilingualTenantDTO.name) &&
        Objects.equals(this.subdomain, multilingualTenantDTO.subdomain) &&
        Objects.equals(this.adminEmails, multilingualTenantDTO.adminEmails) &&
        Objects.equals(this.createDate, multilingualTenantDTO.createDate) &&
        Objects.equals(this.updateDate, multilingualTenantDTO.updateDate) &&
        Objects.equals(this.licensing, multilingualTenantDTO.licensing) &&
        Objects.equals(this.theming, multilingualTenantDTO.theming) &&
        Objects.equals(this.content, multilingualTenantDTO.content) &&
        Objects.equals(this.settings, multilingualTenantDTO.settings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, subdomain, adminEmails, createDate, updateDate, licensing, theming, content, settings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MultilingualTenantDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    subdomain: ").append(toIndentedString(subdomain)).append("\n");
    sb.append("    adminEmails: ").append(toIndentedString(adminEmails)).append("\n");
    sb.append("    createDate: ").append(toIndentedString(createDate)).append("\n");
    sb.append("    updateDate: ").append(toIndentedString(updateDate)).append("\n");
    sb.append("    licensing: ").append(toIndentedString(licensing)).append("\n");
    sb.append("    theming: ").append(toIndentedString(theming)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    settings: ").append(toIndentedString(settings)).append("\n");
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

