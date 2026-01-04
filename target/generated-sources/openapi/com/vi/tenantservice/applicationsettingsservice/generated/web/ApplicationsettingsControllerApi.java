package com.vi.tenantservice.applicationsettingsservice.generated.web;

import com.vi.tenantservice.applicationsettingsservice.generated.ApiClient;

import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsDTO;
import com.vi.tenantservice.applicationsettingsservice.generated.web.model.ApplicationSettingsPatchDTO;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-12-25T23:21:39.902151275Z[Etc/UTC]")
public class ApplicationsettingsControllerApi {
    private ApiClient apiClient;

    public ApplicationsettingsControllerApi() {
        this(new ApiClient());
    }

    public ApplicationsettingsControllerApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Get all application settings
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>204</b> - NO CONTENT - no content found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid Keycloak token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @return ApplicationSettingsDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ApplicationSettingsDTO getApplicationSettings() throws RestClientException {
        return getApplicationSettingsWithHttpInfo().getBody();
    }

    /**
     * Get all application settings
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>204</b> - NO CONTENT - no content found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid Keycloak token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @return ResponseEntity&lt;ApplicationSettingsDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ApplicationSettingsDTO> getApplicationSettingsWithHttpInfo() throws RestClientException {
        Object localVarPostBody = null;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ApplicationSettingsDTO> localReturnType = new ParameterizedTypeReference<ApplicationSettingsDTO>() {};
        return apiClient.invokeAPI("/settings", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Patch application settings
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>204</b> - NO CONTENT - no content found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid Keycloak token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param applicationSettingsPatchDTO  (optional)
     * @return ApplicationSettingsDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ApplicationSettingsDTO patchApplicationSettings(ApplicationSettingsPatchDTO applicationSettingsPatchDTO) throws RestClientException {
        return patchApplicationSettingsWithHttpInfo(applicationSettingsPatchDTO).getBody();
    }

    /**
     * Patch application settings
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>204</b> - NO CONTENT - no content found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid Keycloak token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param applicationSettingsPatchDTO  (optional)
     * @return ResponseEntity&lt;ApplicationSettingsDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ApplicationSettingsDTO> patchApplicationSettingsWithHttpInfo(ApplicationSettingsPatchDTO applicationSettingsPatchDTO) throws RestClientException {
        Object localVarPostBody = applicationSettingsPatchDTO;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<ApplicationSettingsDTO> localReturnType = new ParameterizedTypeReference<ApplicationSettingsDTO>() {};
        return apiClient.invokeAPI("/settingsadmin", HttpMethod.PATCH, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
}
