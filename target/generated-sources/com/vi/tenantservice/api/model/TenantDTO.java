package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.Content;
import com.vi.tenantservice.api.model.Licensing;
import com.vi.tenantservice.api.model.Settings;
import com.vi.tenantservice.api.model.Theming;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TenantDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class TenantDTO {

  private Long id = null;

  private String name;

  private String subdomain;

  private String createDate;

  private String updateDate;

  private Licensing licensing;

  private Theming theming;

  private Content content;

  private Settings settings;

  /**
   * Default constructor
   * @deprecated Use {@link TenantDTO#TenantDTO(Long, String, String)}
   */
  @Deprecated
  public TenantDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TenantDTO(Long id, String name, String subdomain) {
    this.id = id;
    this.name = name;
    this.subdomain = subdomain;
  }

  public TenantDTO id(Long id) {
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

  public TenantDTO name(String name) {
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

  public TenantDTO subdomain(String subdomain) {
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

  public TenantDTO createDate(String createDate) {
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

  public TenantDTO updateDate(String updateDate) {
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

  public TenantDTO licensing(Licensing licensing) {
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

  public TenantDTO theming(Theming theming) {
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

  public TenantDTO content(Content content) {
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
  public Content getContent() {
    return content;
  }

  public void setContent(Content content) {
    this.content = content;
  }

  public TenantDTO settings(Settings settings) {
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
    TenantDTO tenantDTO = (TenantDTO) o;
    return Objects.equals(this.id, tenantDTO.id) &&
        Objects.equals(this.name, tenantDTO.name) &&
        Objects.equals(this.subdomain, tenantDTO.subdomain) &&
        Objects.equals(this.createDate, tenantDTO.createDate) &&
        Objects.equals(this.updateDate, tenantDTO.updateDate) &&
        Objects.equals(this.licensing, tenantDTO.licensing) &&
        Objects.equals(this.theming, tenantDTO.theming) &&
        Objects.equals(this.content, tenantDTO.content) &&
        Objects.equals(this.settings, tenantDTO.settings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, subdomain, createDate, updateDate, licensing, theming, content, settings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TenantDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    subdomain: ").append(toIndentedString(subdomain)).append("\n");
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

