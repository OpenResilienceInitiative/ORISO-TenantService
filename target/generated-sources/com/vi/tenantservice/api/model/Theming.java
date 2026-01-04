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
 * Theming
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class Theming {

  private String logo;

  private String associationLogo;

  private String favicon;

  private String primaryColor;

  private String secondaryColor;

  public Theming logo(String logo) {
    this.logo = logo;
    return this;
  }

  /**
   * Get logo
   * @return logo
  */
  
  @Schema(name = "logo", example = "base64 encoded image", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("logo")
  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public Theming associationLogo(String associationLogo) {
    this.associationLogo = associationLogo;
    return this;
  }

  /**
   * Get associationLogo
   * @return associationLogo
  */
  
  @Schema(name = "associationLogo", example = "base64 encoded image", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("associationLogo")
  public String getAssociationLogo() {
    return associationLogo;
  }

  public void setAssociationLogo(String associationLogo) {
    this.associationLogo = associationLogo;
  }

  public Theming favicon(String favicon) {
    this.favicon = favicon;
    return this;
  }

  /**
   * Get favicon
   * @return favicon
  */
  
  @Schema(name = "favicon", example = "base64 encoded image", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("favicon")
  public String getFavicon() {
    return favicon;
  }

  public void setFavicon(String favicon) {
    this.favicon = favicon;
  }

  public Theming primaryColor(String primaryColor) {
    this.primaryColor = primaryColor;
    return this;
  }

  /**
   * Get primaryColor
   * @return primaryColor
  */
  
  @Schema(name = "primaryColor", example = "#FFFFFF", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("primaryColor")
  public String getPrimaryColor() {
    return primaryColor;
  }

  public void setPrimaryColor(String primaryColor) {
    this.primaryColor = primaryColor;
  }

  public Theming secondaryColor(String secondaryColor) {
    this.secondaryColor = secondaryColor;
    return this;
  }

  /**
   * Get secondaryColor
   * @return secondaryColor
  */
  
  @Schema(name = "secondaryColor", example = "#FFFFFF", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secondaryColor")
  public String getSecondaryColor() {
    return secondaryColor;
  }

  public void setSecondaryColor(String secondaryColor) {
    this.secondaryColor = secondaryColor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Theming theming = (Theming) o;
    return Objects.equals(this.logo, theming.logo) &&
        Objects.equals(this.associationLogo, theming.associationLogo) &&
        Objects.equals(this.favicon, theming.favicon) &&
        Objects.equals(this.primaryColor, theming.primaryColor) &&
        Objects.equals(this.secondaryColor, theming.secondaryColor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(logo, associationLogo, favicon, primaryColor, secondaryColor);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Theming {\n");
    sb.append("    logo: ").append(toIndentedString(logo)).append("\n");
    sb.append("    associationLogo: ").append(toIndentedString(associationLogo)).append("\n");
    sb.append("    favicon: ").append(toIndentedString(favicon)).append("\n");
    sb.append("    primaryColor: ").append(toIndentedString(primaryColor)).append("\n");
    sb.append("    secondaryColor: ").append(toIndentedString(secondaryColor)).append("\n");
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

