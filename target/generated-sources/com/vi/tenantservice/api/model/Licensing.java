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
 * Licensing
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class Licensing {

  private Integer allowedNumberOfUsers;

  /**
   * Default constructor
   * @deprecated Use {@link Licensing#Licensing(Integer)}
   */
  @Deprecated
  public Licensing() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Licensing(Integer allowedNumberOfUsers) {
    this.allowedNumberOfUsers = allowedNumberOfUsers;
  }

  public Licensing allowedNumberOfUsers(Integer allowedNumberOfUsers) {
    this.allowedNumberOfUsers = allowedNumberOfUsers;
    return this;
  }

  /**
   * Get allowedNumberOfUsers
   * @return allowedNumberOfUsers
  */
  @NotNull 
  @Schema(name = "allowedNumberOfUsers", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("allowedNumberOfUsers")
  public Integer getAllowedNumberOfUsers() {
    return allowedNumberOfUsers;
  }

  public void setAllowedNumberOfUsers(Integer allowedNumberOfUsers) {
    this.allowedNumberOfUsers = allowedNumberOfUsers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Licensing licensing = (Licensing) o;
    return Objects.equals(this.allowedNumberOfUsers, licensing.allowedNumberOfUsers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowedNumberOfUsers);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Licensing {\n");
    sb.append("    allowedNumberOfUsers: ").append(toIndentedString(allowedNumberOfUsers)).append("\n");
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

