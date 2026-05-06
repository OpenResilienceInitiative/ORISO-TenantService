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
 * SmtpConfig
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-11T15:41:30.641955926Z[Etc/UTC]")
public class SmtpConfig {

  private Boolean enabled;

  private String host;

  private Integer port;

  private Boolean secure;

  private String username;

  private String password;

  private String from;

  private String emailThemeColor;

  public SmtpConfig enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Get enabled
   * @return enabled
  */
  
  @Schema(name = "enabled", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public SmtpConfig host(String host) {
    this.host = host;
    return this;
  }

  /**
   * Get host
   * @return host
  */
  
  @Schema(name = "host", example = "smtp.titan.email", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public SmtpConfig port(Integer port) {
    this.port = port;
    return this;
  }

  /**
   * Get port
   * @return port
  */
  
  @Schema(name = "port", example = "587", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("port")
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public SmtpConfig secure(Boolean secure) {
    this.secure = secure;
    return this;
  }

  /**
   * Get secure
   * @return secure
  */
  
  @Schema(name = "secure", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secure")
  public Boolean getSecure() {
    return secure;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  public SmtpConfig username(String username) {
    this.username = username;
    return this;
  }

  /**
   * Get username
   * @return username
  */
  
  @Schema(name = "username", example = "notifications@oriso-dev.site", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public SmtpConfig password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Get password
   * @return password
  */
  
  @Schema(name = "password", example = "********", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public SmtpConfig from(String from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
  */
  
  @Schema(name = "from", example = "notifications@oriso-dev.site", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public SmtpConfig emailThemeColor(String emailThemeColor) {
    this.emailThemeColor = emailThemeColor;
    return this;
  }

  /**
   * Get emailThemeColor
   * @return emailThemeColor
  */
  
  @Schema(name = "emailThemeColor", example = "#0f3b8f", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("emailThemeColor")
  public String getEmailThemeColor() {
    return emailThemeColor;
  }

  public void setEmailThemeColor(String emailThemeColor) {
    this.emailThemeColor = emailThemeColor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SmtpConfig smtpConfig = (SmtpConfig) o;
    return Objects.equals(this.enabled, smtpConfig.enabled) &&
        Objects.equals(this.host, smtpConfig.host) &&
        Objects.equals(this.port, smtpConfig.port) &&
        Objects.equals(this.secure, smtpConfig.secure) &&
        Objects.equals(this.username, smtpConfig.username) &&
        Objects.equals(this.password, smtpConfig.password) &&
        Objects.equals(this.from, smtpConfig.from) &&
        Objects.equals(this.emailThemeColor, smtpConfig.emailThemeColor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, host, port, secure, username, password, from, emailThemeColor);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SmtpConfig {\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    host: ").append(toIndentedString(host)).append("\n");
    sb.append("    port: ").append(toIndentedString(port)).append("\n");
    sb.append("    secure: ").append(toIndentedString(secure)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    emailThemeColor: ").append(toIndentedString(emailThemeColor)).append("\n");
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

