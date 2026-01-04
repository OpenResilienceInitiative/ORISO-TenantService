package com.vi.tenantservice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.PaginationLinks;
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
 * TenantsSearchResultDTO
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-25T23:21:38.920585706Z[Etc/UTC]")
public class TenantsSearchResultDTO {

  @Valid
  private List<@Valid AdminTenantDTO> embedded;

  private PaginationLinks links;

  private Integer total;

  public TenantsSearchResultDTO embedded(List<@Valid AdminTenantDTO> embedded) {
    this.embedded = embedded;
    return this;
  }

  public TenantsSearchResultDTO addEmbeddedItem(AdminTenantDTO embeddedItem) {
    if (this.embedded == null) {
      this.embedded = new ArrayList<>();
    }
    this.embedded.add(embeddedItem);
    return this;
  }

  /**
   * Get embedded
   * @return embedded
  */
  @Valid 
  @Schema(name = "_embedded", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("_embedded")
  public List<@Valid AdminTenantDTO> getEmbedded() {
    return embedded;
  }

  public void setEmbedded(List<@Valid AdminTenantDTO> embedded) {
    this.embedded = embedded;
  }

  public TenantsSearchResultDTO links(PaginationLinks links) {
    this.links = links;
    return this;
  }

  /**
   * Get links
   * @return links
  */
  @Valid 
  @Schema(name = "_links", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("_links")
  public PaginationLinks getLinks() {
    return links;
  }

  public void setLinks(PaginationLinks links) {
    this.links = links;
  }

  public TenantsSearchResultDTO total(Integer total) {
    this.total = total;
    return this;
  }

  /**
   * Get total
   * @return total
  */
  
  @Schema(name = "total", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("total")
  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TenantsSearchResultDTO tenantsSearchResultDTO = (TenantsSearchResultDTO) o;
    return Objects.equals(this.embedded, tenantsSearchResultDTO.embedded) &&
        Objects.equals(this.links, tenantsSearchResultDTO.links) &&
        Objects.equals(this.total, tenantsSearchResultDTO.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(embedded, links, total);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TenantsSearchResultDTO {\n");
    sb.append("    embedded: ").append(toIndentedString(embedded)).append("\n");
    sb.append("    links: ").append(toIndentedString(links)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
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

