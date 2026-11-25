package com.vi.tenantservice.api.service.consultingtype;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.config.apiclient.ConsultingTypeServiceApiControllerFactory;
import com.vi.tenantservice.api.exception.ConsultingSettingsOutcomeUnknownException;
import com.vi.tenantservice.api.service.ConfigurationFileLoader;
import com.vi.tenantservice.api.service.consultingtype.ConsultingSettingsTelemetry.Outcome;
import com.vi.tenantservice.api.service.httpheader.SecurityHeaderSupplier;
import com.vi.tenantservice.api.tenant.TenantResolverService;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypeDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.FullConsultingTypeResponseDTO;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultingTypeService {

  private final @NonNull ConsultingTypeServiceApiControllerFactory
      consultingTypeServiceApiControllerFactory;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;
  private final @NonNull TenantResolverService tenantResolverService;
  private final @NonNull ConfigurationFileLoader configurationFileLoader;
  private final @NonNull ConsultingSettingsTelemetry settingsTelemetry;

  @Value("${default.consulting.types.json.path}")
  private String defaultConsultingTypesFilePath;

  public void createDefaultConsultingTypes(Long tenantId) {
    createDefaultConsultingTypes(tenantId, null);
  }

  public void createDefaultConsultingTypes(Long tenantId, String creationContext) {
    if (StringUtils.isBlank(defaultConsultingTypesFilePath)) {
      log.warn(
          "default.consulting.types.json.path is empty - skipping default consulting type bootstrap for tenant {}",
          tenantId);
      return;
    }

    final File file = configurationFileLoader.loadFrom(defaultConsultingTypesFilePath);
    if (!file.exists() || file.isDirectory()) {
      log.warn(
          "Default consulting types file does not exist or is not a file (path: {}) - skipping bootstrap for tenant {}",
          defaultConsultingTypesFilePath,
          tenantId);
      return;
    }

    try {
      ConsultingTypeDTO consultingTypeDTO =
          new ObjectMapper().readValue(file, ConsultingTypeDTO.class);
      consultingTypeDTO.setTenantId(tenantId.intValue());
      createConsultingType(consultingTypeDTO, creationContext);
    } catch (IOException ioException) {
      log.error("Error while reading default consulting types configuration file", ioException);
      throw new IllegalStateException(ioException);
    }
  }

  private void createConsultingType(ConsultingTypeDTO consultingTypeDTO, String creationContext)
      throws RestClientException {
    var consultingTypeControllerApi =
        consultingTypeServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(consultingTypeControllerApi.getApiClient());
    if (creationContext != null) {
      consultingTypeControllerApi
          .getApiClient()
          .addDefaultHeader(
              com.vi.tenantservice.config.security.TenantCreationContext.HEADER, creationContext);
    }
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
    if (id == null) {
      // Let the generated client apply its input validation without counting a network attempt.
      return consultingTypeControllerApi.patchConsultingType(id, consultingTypeDTO);
    }
    var attempt = settingsTelemetry.startPatch();
    consultingTypeControllerApi
        .getApiClient()
        .addDefaultHeader(
            ConsultingSettingsTelemetry.REQUEST_ID_HEADER, attempt.requestId().toString());
    var outcome = Outcome.OTHER;
    try {
      var result = consultingTypeControllerApi.patchConsultingType(id, consultingTypeDTO);
      outcome = Outcome.CONFIRMED;
      return result;
    } catch (ResponseStatusException e) {
      // The CTS response handler preserves its explicit HTTP status.
      if (e.getStatusCode().is5xxServerError()) {
        outcome = Outcome.UPSTREAM_ERROR;
        throw new ConsultingSettingsOutcomeUnknownException(e);
      }
      if (e.getStatusCode().is4xxClientError()) {
        outcome = Outcome.REFUSED;
      }
      throw e;
    } catch (RestClientException e) {
      if (e instanceof UnknownContentTypeException
          || e.getCause() instanceof HttpMessageNotReadableException) {
        outcome = Outcome.INVALID_RESPONSE;
        throw new ConsultingSettingsOutcomeUnknownException(e);
      }
      if (e instanceof ResourceAccessException || e.getCause() instanceof IOException) {
        outcome =
            e.contains(HttpTimeoutException.class) || e.contains(SocketTimeoutException.class)
                ? Outcome.TIMEOUT
                : Outcome.TRANSPORT_ERROR;
        throw new ConsultingSettingsOutcomeUnknownException(e);
      }
      throw e;
    } finally {
      settingsTelemetry.recordPatch(attempt, outcome);
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
