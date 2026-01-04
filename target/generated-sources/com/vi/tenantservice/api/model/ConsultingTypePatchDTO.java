package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.ConsultingTypePatchDTOWelcomeMessage;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ConsultingTypePatchDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class ConsultingTypePatchDTO {

  private Boolean isVideoCallAllowed;

  private Boolean languageFormal;

  private Boolean sendFurtherStepsMessage;

  private ConsultingTypePatchDTOWelcomeMessage welcomeMessage;

  public ConsultingTypePatchDTO isVideoCallAllowed(Boolean isVideoCallAllowed) {
    this.isVideoCallAllowed = isVideoCallAllowed;
    return this;
  }

  /**
   * Get isVideoCallAllowed
   * @return isVideoCallAllowed
  */
  
  @Schema(name = "isVideoCallAllowed", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("isVideoCallAllowed")
  public Boolean getIsVideoCallAllowed() {
    return isVideoCallAllowed;
  }

  public void setIsVideoCallAllowed(Boolean isVideoCallAllowed) {
    this.isVideoCallAllowed = isVideoCallAllowed;
  }

  public ConsultingTypePatchDTO languageFormal(Boolean languageFormal) {
    this.languageFormal = languageFormal;
    return this;
  }

  /**
   * Get languageFormal
   * @return languageFormal
  */
  
  @Schema(name = "languageFormal", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("languageFormal")
  public Boolean getLanguageFormal() {
    return languageFormal;
  }

  public void setLanguageFormal(Boolean languageFormal) {
    this.languageFormal = languageFormal;
  }

  public ConsultingTypePatchDTO sendFurtherStepsMessage(Boolean sendFurtherStepsMessage) {
    this.sendFurtherStepsMessage = sendFurtherStepsMessage;
    return this;
  }

  /**
   * Get sendFurtherStepsMessage
   * @return sendFurtherStepsMessage
  */
  
  @Schema(name = "sendFurtherStepsMessage", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sendFurtherStepsMessage")
  public Boolean getSendFurtherStepsMessage() {
    return sendFurtherStepsMessage;
  }

  public void setSendFurtherStepsMessage(Boolean sendFurtherStepsMessage) {
    this.sendFurtherStepsMessage = sendFurtherStepsMessage;
  }

  public ConsultingTypePatchDTO welcomeMessage(ConsultingTypePatchDTOWelcomeMessage welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
    return this;
  }

  /**
   * Get welcomeMessage
   * @return welcomeMessage
  */
  @Valid 
  @Schema(name = "welcomeMessage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("welcomeMessage")
  public ConsultingTypePatchDTOWelcomeMessage getWelcomeMessage() {
    return welcomeMessage;
  }

  public void setWelcomeMessage(ConsultingTypePatchDTOWelcomeMessage welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsultingTypePatchDTO consultingTypePatchDTO = (ConsultingTypePatchDTO) o;
    return Objects.equals(this.isVideoCallAllowed, consultingTypePatchDTO.isVideoCallAllowed) &&
        Objects.equals(this.languageFormal, consultingTypePatchDTO.languageFormal) &&
        Objects.equals(this.sendFurtherStepsMessage, consultingTypePatchDTO.sendFurtherStepsMessage) &&
        Objects.equals(this.welcomeMessage, consultingTypePatchDTO.welcomeMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isVideoCallAllowed, languageFormal, sendFurtherStepsMessage, welcomeMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsultingTypePatchDTO {\n");
    sb.append("    isVideoCallAllowed: ").append(toIndentedString(isVideoCallAllowed)).append("\n");
    sb.append("    languageFormal: ").append(toIndentedString(languageFormal)).append("\n");
    sb.append("    sendFurtherStepsMessage: ").append(toIndentedString(sendFurtherStepsMessage)).append("\n");
    sb.append("    welcomeMessage: ").append(toIndentedString(welcomeMessage)).append("\n");
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

