package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.DataProtectionContactTemplateDTO;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Content
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class Content {

  private String impressum;

  private String claim;

  private String privacy;

  private String renderedPrivacy;

  private String termsAndConditions;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime termsAndConditionsConfirmation;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime dataPrivacyConfirmation;

  private DataProtectionContactTemplateDTO dataProtectionContactTemplate;

  /**
   * Default constructor
   * @deprecated Use {@link Content#Content(String)}
   */
  @Deprecated
  public Content() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Content(String impressum) {
    this.impressum = impressum;
  }

  public Content impressum(String impressum) {
    this.impressum = impressum;
    return this;
  }

  /**
   * Get impressum
   * @return impressum
  */
  @NotNull 
  @Schema(name = "impressum", example = "Llorem ipsum...", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("impressum")
  public String getImpressum() {
    return impressum;
  }

  public void setImpressum(String impressum) {
    this.impressum = impressum;
  }

  public Content claim(String claim) {
    this.claim = claim;
    return this;
  }

  /**
   * Get claim
   * @return claim
  */
  @Size(max = 40) 
  @Schema(name = "claim", example = "Llorem ipsum...", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("claim")
  public String getClaim() {
    return claim;
  }

  public void setClaim(String claim) {
    this.claim = claim;
  }

  public Content privacy(String privacy) {
    this.privacy = privacy;
    return this;
  }

  /**
   * Get privacy
   * @return privacy
  */
  
  @Schema(name = "privacy", example = "Llorem ipsum...", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("privacy")
  public String getPrivacy() {
    return privacy;
  }

  public void setPrivacy(String privacy) {
    this.privacy = privacy;
  }

  public Content renderedPrivacy(String renderedPrivacy) {
    this.renderedPrivacy = renderedPrivacy;
    return this;
  }

  /**
   * Get renderedPrivacy
   * @return renderedPrivacy
  */
  
  @Schema(name = "renderedPrivacy", example = "Llorem ipsum...", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("renderedPrivacy")
  public String getRenderedPrivacy() {
    return renderedPrivacy;
  }

  public void setRenderedPrivacy(String renderedPrivacy) {
    this.renderedPrivacy = renderedPrivacy;
  }

  public Content termsAndConditions(String termsAndConditions) {
    this.termsAndConditions = termsAndConditions;
    return this;
  }

  /**
   * Get termsAndConditions
   * @return termsAndConditions
  */
  
  @Schema(name = "termsAndConditions", example = "Llorem ipsum...", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("termsAndConditions")
  public String getTermsAndConditions() {
    return termsAndConditions;
  }

  public void setTermsAndConditions(String termsAndConditions) {
    this.termsAndConditions = termsAndConditions;
  }

  public Content termsAndConditionsConfirmation(LocalDateTime termsAndConditionsConfirmation) {
    this.termsAndConditionsConfirmation = termsAndConditionsConfirmation;
    return this;
  }

  /**
   * Datetime stamp when new terms and condition defined
   * @return termsAndConditionsConfirmation
  */
  @Valid 
  @Schema(name = "termsAndConditionsConfirmation", description = "Datetime stamp when new terms and condition defined", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("termsAndConditionsConfirmation")
  public LocalDateTime getTermsAndConditionsConfirmation() {
    return termsAndConditionsConfirmation;
  }

  public void setTermsAndConditionsConfirmation(LocalDateTime termsAndConditionsConfirmation) {
    this.termsAndConditionsConfirmation = termsAndConditionsConfirmation;
  }

  public Content dataPrivacyConfirmation(LocalDateTime dataPrivacyConfirmation) {
    this.dataPrivacyConfirmation = dataPrivacyConfirmation;
    return this;
  }

  /**
   * Datetime stamp when new privacy defined
   * @return dataPrivacyConfirmation
  */
  @Valid 
  @Schema(name = "dataPrivacyConfirmation", description = "Datetime stamp when new privacy defined", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dataPrivacyConfirmation")
  public LocalDateTime getDataPrivacyConfirmation() {
    return dataPrivacyConfirmation;
  }

  public void setDataPrivacyConfirmation(LocalDateTime dataPrivacyConfirmation) {
    this.dataPrivacyConfirmation = dataPrivacyConfirmation;
  }

  public Content dataProtectionContactTemplate(DataProtectionContactTemplateDTO dataProtectionContactTemplate) {
    this.dataProtectionContactTemplate = dataProtectionContactTemplate;
    return this;
  }

  /**
   * Get dataProtectionContactTemplate
   * @return dataProtectionContactTemplate
  */
  @Valid 
  @Schema(name = "dataProtectionContactTemplate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dataProtectionContactTemplate")
  public DataProtectionContactTemplateDTO getDataProtectionContactTemplate() {
    return dataProtectionContactTemplate;
  }

  public void setDataProtectionContactTemplate(DataProtectionContactTemplateDTO dataProtectionContactTemplate) {
    this.dataProtectionContactTemplate = dataProtectionContactTemplate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Content content = (Content) o;
    return Objects.equals(this.impressum, content.impressum) &&
        Objects.equals(this.claim, content.claim) &&
        Objects.equals(this.privacy, content.privacy) &&
        Objects.equals(this.renderedPrivacy, content.renderedPrivacy) &&
        Objects.equals(this.termsAndConditions, content.termsAndConditions) &&
        Objects.equals(this.termsAndConditionsConfirmation, content.termsAndConditionsConfirmation) &&
        Objects.equals(this.dataPrivacyConfirmation, content.dataPrivacyConfirmation) &&
        Objects.equals(this.dataProtectionContactTemplate, content.dataProtectionContactTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(impressum, claim, privacy, renderedPrivacy, termsAndConditions, termsAndConditionsConfirmation, dataPrivacyConfirmation, dataProtectionContactTemplate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Content {\n");
    sb.append("    impressum: ").append(toIndentedString(impressum)).append("\n");
    sb.append("    claim: ").append(toIndentedString(claim)).append("\n");
    sb.append("    privacy: ").append(toIndentedString(privacy)).append("\n");
    sb.append("    renderedPrivacy: ").append(toIndentedString(renderedPrivacy)).append("\n");
    sb.append("    termsAndConditions: ").append(toIndentedString(termsAndConditions)).append("\n");
    sb.append("    termsAndConditionsConfirmation: ").append(toIndentedString(termsAndConditionsConfirmation)).append("\n");
    sb.append("    dataPrivacyConfirmation: ").append(toIndentedString(dataPrivacyConfirmation)).append("\n");
    sb.append("    dataProtectionContactTemplate: ").append(toIndentedString(dataProtectionContactTemplate)).append("\n");
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

