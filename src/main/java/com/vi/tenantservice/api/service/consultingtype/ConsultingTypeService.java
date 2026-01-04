package com.vi.tenantservice.api.service.consultingtype;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.service.ConfigurationFileLoader;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypeDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.FullConsultingTypeResponseDTO;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultingTypeService {

  private final @NonNull ConsultingTypeServiceApiControllerFactory
      consultingTypeServiceApiControllerFactory;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;
  private final @NonNull TenantResolverService tenantResolverService;
  private final @NonNull ConfigurationFileLoader configurationFileLoader;

  @Value("${default.consulting.types.json.path}")
  private String defaultConsultingTypesFilePath;

  public void createDefaultConsultingTypes(Long tenantId) {
    final File file = configurationFileLoader.loadFrom(defaultConsultingTypesFilePath);
    try {
      ConsultingTypeDTO consultingTypeDTO =
          new ObjectMapper().readValue(file, ConsultingTypeDTO.class);
      consultingTypeDTO.setTenantId(tenantId.intValue());
      createConsultingType(consultingTypeDTO);
    } catch (IOException ioException) {
      log.error("Error while reading default consulting types configuration file", ioException);
      throw new IllegalStateException(ioException);
    }
  }

  private void createConsultingType(ConsultingTypeDTO consultingTypeDTO)
      throws RestClientException {
    var consultingTypeControllerApi =
        consultingTypeServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(consultingTypeControllerApi.getApiClient());
    try {
      consultingTypeControllerApi.createConsultingType(consultingTypeDTO);
    } catch (RestClientException e) {
      log.error("Error while creating consulting type {}", consultingTypeDTO, e);
      throw e;
    }
  }

  public FullConsultingTypeResponseDTO patchConsultingType(
      Integer id, ConsultingTypePatchDTO consultingTypeDTO) throws RestClientException {
    var consultingTypeControllerApi =
        consultingTypeServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(consultingTypeControllerApi.getApiClient());
    try {
      return consultingTypeControllerApi.patchConsultingType(id, consultingTypeDTO);
    } catch (RestClientException e) {
      log.error("Error while patching consulting type {}", id, e);
      throw e;
    }
  }

  public FullConsultingTypeResponseDTO getConsultingTypesByTenantId(Integer tenantId) {
    var consultingTypeControllerApi =
        consultingTypeServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(consultingTypeControllerApi.getApiClient());
    try {
      return consultingTypeControllerApi.getFullConsultingTypeByTenantId(tenantId);
    } catch (RestClientException e) {
      log.error("Error while getting consulting types for tenant {}", tenantId, e);
      throw e;
    }
  }

  private void addDefaultHeaders(
      com.vi.tenantservice.consultingtypeservice.generated.ApiClient apiClient) {
    var headers = this.securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders();

    Optional<Long> optionalTenant = tenantResolverService.tryResolve();
    optionalTenant.ifPresent(aLong -> headers.add("tenantId", aLong.toString()));
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }
}
