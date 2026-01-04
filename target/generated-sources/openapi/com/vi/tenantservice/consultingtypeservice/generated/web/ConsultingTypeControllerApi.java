package com.vi.tenantservice.consultingtypeservice.generated.web;

import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;

import com.vi.tenantservice.consultingtypeservice.generated.web.model.BasicConsultingTypeResponseDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypeDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypeGroupResponseDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ConsultingTypePatchDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.ExtendedConsultingTypeResponseDTO;
import com.vi.tenantservice.consultingtypeservice.generated.web.model.FullConsultingTypeResponseDTO;

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

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-12-25T23:21:39.589894428Z[Etc/UTC]")
public class ConsultingTypeControllerApi {
    private ApiClient apiClient;

    public ConsultingTypeControllerApi() {
        this(new ApiClient());
    }

    public ConsultingTypeControllerApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Create a consulting type [Authorization: Role: tenant-admin]
     * 
     * <p><b>201</b> - CREATED - consulting type was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>403</b> - FORBIDDEN - no/invalid CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultingTypeDTO  (required)
     * @return FullConsultingTypeResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public FullConsultingTypeResponseDTO createConsultingType(ConsultingTypeDTO consultingTypeDTO) throws RestClientException {
        return createConsultingTypeWithHttpInfo(consultingTypeDTO).getBody();
    }

    /**
     * Create a consulting type [Authorization: Role: tenant-admin]
     * 
     * <p><b>201</b> - CREATED - consulting type was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>403</b> - FORBIDDEN - no/invalid CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultingTypeDTO  (required)
     * @return ResponseEntity&lt;FullConsultingTypeResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FullConsultingTypeResponseDTO> createConsultingTypeWithHttpInfo(ConsultingTypeDTO consultingTypeDTO) throws RestClientException {
        Object localVarPostBody = consultingTypeDTO;
        
        // verify the required parameter 'consultingTypeDTO' is set
        if (consultingTypeDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultingTypeDTO' when calling createConsultingType");
        }
        

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

        ParameterizedTypeReference<FullConsultingTypeResponseDTO> localReturnType = new ParameterizedTypeReference<FullConsultingTypeResponseDTO>() {};
        return apiClient.invokeAPI("/consultingtypes", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the consulting type with basic set of properties for a given consulting type id
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultingTypeId cnsulting type id (required)
     * @return BasicConsultingTypeResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public BasicConsultingTypeResponseDTO getBasicConsultingTypeById(Integer consultingTypeId) throws RestClientException {
        return getBasicConsultingTypeByIdWithHttpInfo(consultingTypeId).getBody();
    }

    /**
     * Returns the consulting type with basic set of properties for a given consulting type id
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultingTypeId cnsulting type id (required)
     * @return ResponseEntity&lt;BasicConsultingTypeResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<BasicConsultingTypeResponseDTO> getBasicConsultingTypeByIdWithHttpInfo(Integer consultingTypeId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'consultingTypeId' is set
        if (consultingTypeId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultingTypeId' when calling getBasicConsultingTypeById");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultingTypeId", consultingTypeId);

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

        ParameterizedTypeReference<BasicConsultingTypeResponseDTO> localReturnType = new ParameterizedTypeReference<BasicConsultingTypeResponseDTO>() {};
        return apiClient.invokeAPI("/consultingtypes/{consultingTypeId}/basic", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns a list of all consulting types with basic properties 
     * 
     * <p><b>200</b> - successful operation
     * <p><b>204</b> - successful operation, but no content
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @return List&lt;BasicConsultingTypeResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public List<BasicConsultingTypeResponseDTO> getBasicConsultingTypeList() throws RestClientException {
        return getBasicConsultingTypeListWithHttpInfo().getBody();
    }

    /**
     * Returns a list of all consulting types with basic properties 
     * 
     * <p><b>200</b> - successful operation
     * <p><b>204</b> - successful operation, but no content
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @return ResponseEntity&lt;List&lt;BasicConsultingTypeResponseDTO&gt;&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<BasicConsultingTypeResponseDTO>> getBasicConsultingTypeListWithHttpInfo() throws RestClientException {
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

        ParameterizedTypeReference<List<BasicConsultingTypeResponseDTO>> localReturnType = new ParameterizedTypeReference<List<BasicConsultingTypeResponseDTO>>() {};
        return apiClient.invokeAPI("/consultingtypes/basic", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the group structure of all consulting types
     * 
     * <p><b>200</b> - successful operation
     * <p><b>204</b> - NO CONTENT - no groups defined
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @return List&lt;ConsultingTypeGroupResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public List<ConsultingTypeGroupResponseDTO> getConsultingTypeGroups() throws RestClientException {
        return getConsultingTypeGroupsWithHttpInfo().getBody();
    }

    /**
     * Returns the group structure of all consulting types
     * 
     * <p><b>200</b> - successful operation
     * <p><b>204</b> - NO CONTENT - no groups defined
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @return ResponseEntity&lt;List&lt;ConsultingTypeGroupResponseDTO&gt;&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<ConsultingTypeGroupResponseDTO>> getConsultingTypeGroupsWithHttpInfo() throws RestClientException {
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

        ParameterizedTypeReference<List<ConsultingTypeGroupResponseDTO>> localReturnType = new ParameterizedTypeReference<List<ConsultingTypeGroupResponseDTO>>() {};
        return apiClient.invokeAPI("/consultingtypes/groups", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the consulting type with extended set of properties for a given consulting type id
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultingTypeId cnsulting type id (required)
     * @return ExtendedConsultingTypeResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ExtendedConsultingTypeResponseDTO getExtendedConsultingTypeById(Integer consultingTypeId) throws RestClientException {
        return getExtendedConsultingTypeByIdWithHttpInfo(consultingTypeId).getBody();
    }

    /**
     * Returns the consulting type with extended set of properties for a given consulting type id
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultingTypeId cnsulting type id (required)
     * @return ResponseEntity&lt;ExtendedConsultingTypeResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ExtendedConsultingTypeResponseDTO> getExtendedConsultingTypeByIdWithHttpInfo(Integer consultingTypeId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'consultingTypeId' is set
        if (consultingTypeId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultingTypeId' when calling getExtendedConsultingTypeById");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultingTypeId", consultingTypeId);

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

        ParameterizedTypeReference<ExtendedConsultingTypeResponseDTO> localReturnType = new ParameterizedTypeReference<ExtendedConsultingTypeResponseDTO>() {};
        return apiClient.invokeAPI("/consultingtypes/{consultingTypeId}/extended", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the consulting type with all properties for a given consulting type id
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultingTypeId cnsulting type id (required)
     * @return FullConsultingTypeResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public FullConsultingTypeResponseDTO getFullConsultingTypeById(Integer consultingTypeId) throws RestClientException {
        return getFullConsultingTypeByIdWithHttpInfo(consultingTypeId).getBody();
    }

    /**
     * Returns the consulting type with all properties for a given consulting type id
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultingTypeId cnsulting type id (required)
     * @return ResponseEntity&lt;FullConsultingTypeResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FullConsultingTypeResponseDTO> getFullConsultingTypeByIdWithHttpInfo(Integer consultingTypeId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'consultingTypeId' is set
        if (consultingTypeId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultingTypeId' when calling getFullConsultingTypeById");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultingTypeId", consultingTypeId);

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

        ParameterizedTypeReference<FullConsultingTypeResponseDTO> localReturnType = new ParameterizedTypeReference<FullConsultingTypeResponseDTO>() {};
        return apiClient.invokeAPI("/consultingtypes/{consultingTypeId}/full", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the consulting type with all properties for a given consulting type slug
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param slug cnsulting type slug (required)
     * @return FullConsultingTypeResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public FullConsultingTypeResponseDTO getFullConsultingTypeBySlug(String slug) throws RestClientException {
        return getFullConsultingTypeBySlugWithHttpInfo(slug).getBody();
    }

    /**
     * Returns the consulting type with all properties for a given consulting type slug
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param slug cnsulting type slug (required)
     * @return ResponseEntity&lt;FullConsultingTypeResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FullConsultingTypeResponseDTO> getFullConsultingTypeBySlugWithHttpInfo(String slug) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'slug' is set
        if (slug == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'slug' when calling getFullConsultingTypeBySlug");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("slug", slug);

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

        ParameterizedTypeReference<FullConsultingTypeResponseDTO> localReturnType = new ParameterizedTypeReference<FullConsultingTypeResponseDTO>() {};
        return apiClient.invokeAPI("/consultingtypes/byslug/{slug}/full", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the consulting type with all properties for a given consulting type tenantId
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param tenantId consulting type tenantId (required)
     * @return FullConsultingTypeResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public FullConsultingTypeResponseDTO getFullConsultingTypeByTenantId(Integer tenantId) throws RestClientException {
        return getFullConsultingTypeByTenantIdWithHttpInfo(tenantId).getBody();
    }

    /**
     * Returns the consulting type with all properties for a given consulting type tenantId
     * 
     * <p><b>200</b> - successful operation
     * <p><b>404</b> - NOT FOUND - ConsultingType not found
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param tenantId consulting type tenantId (required)
     * @return ResponseEntity&lt;FullConsultingTypeResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FullConsultingTypeResponseDTO> getFullConsultingTypeByTenantIdWithHttpInfo(Integer tenantId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'tenantId' is set
        if (tenantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'tenantId' when calling getFullConsultingTypeByTenantId");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("tenantId", tenantId);

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

        ParameterizedTypeReference<FullConsultingTypeResponseDTO> localReturnType = new ParameterizedTypeReference<FullConsultingTypeResponseDTO>() {};
        return apiClient.invokeAPI("/consultingtypes/bytenant/{tenantId}/full", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Updates a consulting type [Authorization: Role: tenant-admin]
     * 
     * <p><b>200</b> - Successful operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid Keycloak token
     * <p><b>409</b> - CONFLICT - unique constraint validation fails
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param id ConsultingType ID (required)
     * @param consultingTypePatchDTO  (optional)
     * @return FullConsultingTypeResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public FullConsultingTypeResponseDTO patchConsultingType(Integer id, ConsultingTypePatchDTO consultingTypePatchDTO) throws RestClientException {
        return patchConsultingTypeWithHttpInfo(id, consultingTypePatchDTO).getBody();
    }

    /**
     * Updates a consulting type [Authorization: Role: tenant-admin]
     * 
     * <p><b>200</b> - Successful operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid Keycloak token
     * <p><b>409</b> - CONFLICT - unique constraint validation fails
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param id ConsultingType ID (required)
     * @param consultingTypePatchDTO  (optional)
     * @return ResponseEntity&lt;FullConsultingTypeResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<FullConsultingTypeResponseDTO> patchConsultingTypeWithHttpInfo(Integer id, ConsultingTypePatchDTO consultingTypePatchDTO) throws RestClientException {
        Object localVarPostBody = consultingTypePatchDTO;
        
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'id' when calling patchConsultingType");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("id", id);

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

        ParameterizedTypeReference<FullConsultingTypeResponseDTO> localReturnType = new ParameterizedTypeReference<FullConsultingTypeResponseDTO>() {};
        return apiClient.invokeAPI("/consultingtypes/{id}", HttpMethod.PATCH, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
}
