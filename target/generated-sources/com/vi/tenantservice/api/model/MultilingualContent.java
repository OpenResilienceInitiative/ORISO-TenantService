package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.DataProtectionContactTemplateDTO;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * MultilingualContent
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class MultilingualContent {

  @Valid
  private Map<String, String> impressum = new HashMap<>();

  @Valid
  private Map<String, String> claim = new HashMap<>();

  @Valid
  private Map<String, String> privacy = new HashMap<>();

  private Boolean confirmPrivacy;

  @Valid
  private Map<String, String> termsAndConditions = new HashMap<>();

  private Boolean confirmTermsAndConditions;

  @Valid
  private Map<String, DataProtectionContactTemplateDTO> dataProtectionContactTemplate = new HashMap<>();

  /**
   * Default constructor
   * @deprecated Use {@link MultilingualContent#MultilingualContent(Map<String, String>)}
   */
  @Deprecated
  public MultilingualContent() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MultilingualContent(Map<String, String> impressum) {
    this.impressum = impressum;
  }

  public MultilingualContent impressum(Map<String, String> impressum) {
    this.impressum = impressum;
    return this;
  }

  public MultilingualContent putImpressumItem(String key, String impressumItem) {
    if (this.impressum == null) {
      this.impressum = new HashMap<>();
    }
    this.impressum.put(key, impressumItem);
    return this;
  }

  /**
   * Get impressum
   * @return impressum
  */
  @NotNull 
  @Schema(name = "impressum", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("impressum")
  public Map<String, String> getImpressum() {
    return impressum;
  }

  public void setImpressum(Map<String, String> impressum) {
    this.impressum = impressum;
  }

  public MultilingualContent claim(Map<String, String> claim) {
    this.claim = claim;
    return this;
  }

  public MultilingualContent putClaimItem(String key, String claimItem) {
    if (this.claim == null) {
      this.claim = new HashMap<>();
    }
    this.claim.put(key, claimItem);
    return this;
  }

  /**
   * Get claim
   * @return claim
  */
  
  @Schema(name = "claim", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("claim")
  public Map<String, String> getClaim() {
    return claim;
  }

  public void setClaim(Map<String, String> claim) {
    this.claim = claim;
  }

  public MultilingualContent privacy(Map<String, String> privacy) {
    this.privacy = privacy;
    return this;
  }

  public MultilingualContent putPrivacyItem(String key, String privacyItem) {
    if (this.privacy == null) {
      this.privacy = new HashMap<>();
    }
    this.privacy.put(key, privacyItem);
    return this;
  }

  /**
   * Get privacy
   * @return privacy
  */
  
  @Schema(name = "privacy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("privacy")
  public Map<String, String> getPrivacy() {
    return privacy;
  }

  public void setPrivacy(Map<String, String> privacy) {
    this.privacy = privacy;
  }

  public MultilingualContent confirmPrivacy(Boolean confirmPrivacy) {
    this.confirmPrivacy = confirmPrivacy;
    return this;
  }

  /**
   * Get confirmPrivacy
   * @return confirmPrivacy
  */
  
  @Schema(name = "confirmPrivacy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("confirmPrivacy")
  public Boolean getConfirmPrivacy() {
    return confirmPrivacy;
  }

  public void setConfirmPrivacy(Boolean confirmPrivacy) {
    this.confirmPrivacy = confirmPrivacy;
  }

  public MultilingualContent termsAndConditions(Map<String, String> termsAndConditions) {
    this.termsAndConditions = termsAndConditions;
    return this;
  }

  public MultilingualContent putTermsAndConditionsItem(String key, String termsAndConditionsItem) {
    if (this.termsAndConditions == null) {
      this.termsAndConditions = new HashMap<>();
    }
    this.termsAndConditions.put(key, termsAndConditionsItem);
    return this;
  }

  /**
   * Get termsAndConditions
   * @return termsAndConditions
  */
  
  @Schema(name = "termsAndConditions", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("termsAndConditions")
  public Map<String, String> getTermsAndConditions() {
    return termsAndConditions;
  }

  public void setTermsAndConditions(Map<String, String> termsAndConditions) {
    this.termsAndConditions = termsAndConditions;
  }

  public MultilingualContent confirmTermsAndConditions(Boolean confirmTermsAndConditions) {
    this.confirmTermsAndConditions = confirmTermsAndConditions;
    return this;
  }

  /**
   * Get confirmTermsAndConditions
   * @return confirmTermsAndConditions
  */
  
  @Schema(name = "confirmTermsAndConditions", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("confirmTermsAndConditions")
  public Boolean getConfirmTermsAndConditions() {
    return confirmTermsAndConditions;
  }

  public void setConfirmTermsAndConditions(Boolean confirmTermsAndConditions) {
    this.confirmTermsAndConditions = confirmTermsAndConditions;
  }

  public MultilingualContent dataProtectionContactTemplate(Map<String, DataProtectionContactTemplateDTO> dataProtectionContactTemplate) {
    this.dataProtectionContactTemplate = dataProtectionContactTemplate;
    return this;
  }

  public MultilingualContent putDataProtectionContactTemplateItem(String key, DataProtectionContactTemplateDTO dataProtectionContactTemplateItem) {
    if (this.dataProtectionContactTemplate == null) {
      this.dataProtectionContactTemplate = new HashMap<>();
    }
    this.dataProtectionContactTemplate.put(key, dataProtectionContactTemplateItem);
    return this;
  }

  /**
   * Get dataProtectionContactTemplate
   * @return dataProtectionContactTemplate
  */
  @Valid 
  @Schema(name = "dataProtectionContactTemplate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dataProtectionContactTemplate")
  public Map<String, DataProtectionContactTemplateDTO> getDataProtectionContactTemplate() {
    return dataProtectionContactTemplate;
  }

  public void setDataProtectionContactTemplate(Map<String, DataProtectionContactTemplateDTO> dataProtectionContactTemplate) {
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
    MultilingualContent multilingualContent = (MultilingualContent) o;
    return Objects.equals(this.impressum, multilingualContent.impressum) &&
        Objects.equals(this.claim, multilingualContent.claim) &&
        Objects.equals(this.privacy, multilingualContent.privacy) &&
        Objects.equals(this.confirmPrivacy, multilingualContent.confirmPrivacy) &&
        Objects.equals(this.termsAndConditions, multilingualContent.termsAndConditions) &&
        Objects.equals(this.confirmTermsAndConditions, multilingualContent.confirmTermsAndConditions) &&
        Objects.equals(this.dataProtectionContactTemplate, multilingualContent.dataProtectionContactTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(impressum, claim, privacy, confirmPrivacy, termsAndConditions, confirmTermsAndConditions, dataProtectionContactTemplate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MultilingualContent {\n");
    sb.append("    impressum: ").append(toIndentedString(impressum)).append("\n");
    sb.append("    claim: ").append(toIndentedString(claim)).append("\n");
    sb.append("    privacy: ").append(toIndentedString(privacy)).append("\n");
    sb.append("    confirmPrivacy: ").append(toIndentedString(confirmPrivacy)).append("\n");
    sb.append("    termsAndConditions: ").append(toIndentedString(termsAndConditions)).append("\n");
    sb.append("    confirmTermsAndConditions: ").append(toIndentedString(confirmTermsAndConditions)).append("\n");
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

