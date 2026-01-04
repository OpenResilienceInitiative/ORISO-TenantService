package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.Content;
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
 * RestrictedTenantDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class RestrictedTenantDTO {

  private Long id = null;

  private String name;

  private String subdomain;

  private Theming theming;

  private Content content;

  private Settings settings;

  /**
   * Default constructor
   * @deprecated Use {@link RestrictedTenantDTO#RestrictedTenantDTO(Long, String)}
   */
  @Deprecated
  public RestrictedTenantDTO() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RestrictedTenantDTO(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public RestrictedTenantDTO id(Long id) {
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

  public RestrictedTenantDTO name(String name) {
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

  public RestrictedTenantDTO subdomain(String subdomain) {
    this.subdomain = subdomain;
    return this;
  }

  /**
   * Get subdomain
   * @return subdomain
  */
  @Size(max = 100) 
  @Schema(name = "subdomain", example = "subdomain", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("subdomain")
  public String getSubdomain() {
    return subdomain;
  }

  public void setSubdomain(String subdomain) {
    this.subdomain = subdomain;
  }

  public RestrictedTenantDTO theming(Theming theming) {
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

  public RestrictedTenantDTO content(Content content) {
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

  public RestrictedTenantDTO settings(Settings settings) {
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
    RestrictedTenantDTO restrictedTenantDTO = (RestrictedTenantDTO) o;
    return Objects.equals(this.id, restrictedTenantDTO.id) &&
        Objects.equals(this.name, restrictedTenantDTO.name) &&
        Objects.equals(this.subdomain, restrictedTenantDTO.subdomain) &&
        Objects.equals(this.theming, restrictedTenantDTO.theming) &&
        Objects.equals(this.content, restrictedTenantDTO.content) &&
        Objects.equals(this.settings, restrictedTenantDTO.settings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, subdomain, theming, content, settings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RestrictedTenantDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    subdomain: ").append(toIndentedString(subdomain)).append("\n");
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

