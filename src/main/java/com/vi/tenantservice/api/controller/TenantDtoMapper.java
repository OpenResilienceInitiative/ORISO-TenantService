package com.vi.tenantservice.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.vi.tenantservice.api.model.AdminTenantDTO;
import com.vi.tenantservice.api.model.HalLink;
import com.vi.tenantservice.api.model.HalLink.MethodEnum;
import com.vi.tenantservice.api.model.PaginationLinks;
import com.vi.tenantservice.api.model.TenantsSearchResultDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

@Service
public class TenantDtoMapper {

  public String mappedFieldOf(String field) {
    switch (field) {
      case "NAME":
        return "name";
      case "ID":
        return "id";
      default:
    }
    throw new IllegalArgumentException("Mapping of field '" + field + "' not supported.");
  }

  public TenantsSearchResultDTO tenantsSearchResultOf(
      Map<String, Object> resultMap,
      String query,
      Integer page,
      Integer perPage,
      String field,
      String order) {
    var tenants = new ArrayList<AdminTenantDTO>();

    var tenantMaps = (List<Map<String, Object>>) resultMap.get("tenants");
    tenantMaps.forEach(
        tenantMap -> {
          var response = new AdminTenantDTO();
          response.setId((Long) tenantMap.get("id"));
          response.setName((String) tenantMap.get("name"));
          response.setSubdomain((String) tenantMap.get("subdomain"));
          response.setBeraterCount((Integer) tenantMap.get("beraterCount"));
          response.setAdminEmails((List<String>) tenantMap.get("adminEmails"));
          response.setCreateDate((String) tenantMap.get("createDate"));
          response.setUpdateDate((String) tenantMap.get("updateDate"));
          tenants.add(response);
        });

    var result = new TenantsSearchResultDTO();
    result.setTotal((Integer) resultMap.get("totalElements"));
    result.setEmbedded(tenants);

    var pagination = new PaginationLinks().self(pageLinkOf(query, page, perPage, field, order));
    if (!(boolean) resultMap.get("isFirstPage")) {
      pagination.previous(pageLinkOf(query, page - 1, perPage, field, order));
    }
    if (!(boolean) resultMap.get("isLastPage")) {
      pagination.next(pageLinkOf(query, page + 1, perPage, field, order));
    }
    result.setLinks(pagination);

    return result;
  }

  private HalLink pageLinkOf(String query, int page, int perPage, String field, String order) {
    var httpEntity =
        methodOn(TenantController.class).searchTenants(query, page, perPage, field, order);

    return halLinkOf(httpEntity, MethodEnum.GET);
  }

  private HalLink halLinkOf(HttpEntity<?> httpEntity, MethodEnum method) {
    var link = linkTo(httpEntity).withSelfRel();

    return new HalLink().href(link.getHref()).method(method).templated(link.isTemplated());
  }
}
