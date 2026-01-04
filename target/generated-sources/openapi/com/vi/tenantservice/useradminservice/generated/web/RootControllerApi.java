package com.vi.tenantservice.useradminservice.generated.web;

import com.vi.tenantservice.useradminservice.generated.ApiClient;

import com.vi.tenantservice.useradminservice.generated.web.model.RootDTO;

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

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-12-25T23:21:40.085179316Z[Etc/UTC]")
public class RootControllerApi {
    private ApiClient apiClient;

    public RootControllerApi() {
        this(new ApiClient());
    }

    public RootControllerApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Returns the hal root entry point. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * @return RootDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public RootDTO getRoot() throws RestClientException {
        return getRootWithHttpInfo().getBody();
    }

    /**
     * Returns the hal root entry point. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * @return ResponseEntity&lt;RootDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<RootDTO> getRootWithHttpInfo() throws RestClientException {
        Object localVarPostBody = null;
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<RootDTO> localReturnType = new ParameterizedTypeReference<RootDTO>() {};
        return apiClient.invokeAPI("/useradmin", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
}
