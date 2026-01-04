package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ConsultingTypePatchDTOWelcomeMessage
 */

@JsonTypeName("ConsultingTypePatchDTO_welcomeMessage")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class ConsultingTypePatchDTOWelcomeMessage {

  private Boolean sendWelcomeMessage;

  private String welcomeMessageText;

  public ConsultingTypePatchDTOWelcomeMessage sendWelcomeMessage(Boolean sendWelcomeMessage) {
    this.sendWelcomeMessage = sendWelcomeMessage;
    return this;
  }

  /**
   * Get sendWelcomeMessage
   * @return sendWelcomeMessage
  */
  
  @Schema(name = "sendWelcomeMessage", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sendWelcomeMessage")
  public Boolean getSendWelcomeMessage() {
    return sendWelcomeMessage;
  }

  public void setSendWelcomeMessage(Boolean sendWelcomeMessage) {
    this.sendWelcomeMessage = sendWelcomeMessage;
  }

  public ConsultingTypePatchDTOWelcomeMessage welcomeMessageText(String welcomeMessageText) {
    this.welcomeMessageText = welcomeMessageText;
    return this;
  }

  /**
   * Get welcomeMessageText
   * @return welcomeMessageText
  */
  
  @Schema(name = "welcomeMessageText", example = "Lorem ipsum", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("welcomeMessageText")
  public String getWelcomeMessageText() {
    return welcomeMessageText;
  }

  public void setWelcomeMessageText(String welcomeMessageText) {
    this.welcomeMessageText = welcomeMessageText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsultingTypePatchDTOWelcomeMessage consultingTypePatchDTOWelcomeMessage = (ConsultingTypePatchDTOWelcomeMessage) o;
    return Objects.equals(this.sendWelcomeMessage, consultingTypePatchDTOWelcomeMessage.sendWelcomeMessage) &&
        Objects.equals(this.welcomeMessageText, consultingTypePatchDTOWelcomeMessage.welcomeMessageText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sendWelcomeMessage, welcomeMessageText);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsultingTypePatchDTOWelcomeMessage {\n");
    sb.append("    sendWelcomeMessage: ").append(toIndentedString(sendWelcomeMessage)).append("\n");
    sb.append("    welcomeMessageText: ").append(toIndentedString(welcomeMessageText)).append("\n");
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

