package com.vi.tenantservice.useradminservice.generated.web;

import com.vi.tenantservice.useradminservice.generated.ApiClient;

import com.vi.tenantservice.useradminservice.generated.web.model.AdminFilter;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminResponseDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.AdminSearchResultDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.AgencyConsultantResponseDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.AskerResponseDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.ConsultantAdminResponseDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.ConsultantAgencyResponseDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.ConsultantFilter;
import com.vi.tenantservice.useradminservice.generated.web.model.ConsultantSearchResultDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.CreateAdminAgencyRelationDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.CreateAdminDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.CreateConsultantAgencyDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.CreateConsultantDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.PatchAdminDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.SessionAdminResultDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.SessionFilter;
import com.vi.tenantservice.useradminservice.generated.web.model.Sort;
import com.vi.tenantservice.useradminservice.generated.web.model.UpdateAdminConsultantDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.UpdateAgencyAdminDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.UpdateTenantAdminDTO;
import com.vi.tenantservice.useradminservice.generated.web.model.ViolationDTO;

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
public class AdminUserControllerApi {
    private ApiClient apiClient;

    public AdminUserControllerApi() {
        this(new ApiClient());
    }

    public AdminUserControllerApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Create an admin-agency relation [Authorization: Role: user-admin]
     * 
     * <p><b>201</b> - CREATED - admin-agency relation was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin Id (required)
     * @param createAdminAgencyRelationDTO  (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void createAdminAgencyRelation(String adminId, CreateAdminAgencyRelationDTO createAdminAgencyRelationDTO) throws RestClientException {
        createAdminAgencyRelationWithHttpInfo(adminId, createAdminAgencyRelationDTO);
    }

    /**
     * Create an admin-agency relation [Authorization: Role: user-admin]
     * 
     * <p><b>201</b> - CREATED - admin-agency relation was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin Id (required)
     * @param createAdminAgencyRelationDTO  (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> createAdminAgencyRelationWithHttpInfo(String adminId, CreateAdminAgencyRelationDTO createAdminAgencyRelationDTO) throws RestClientException {
        Object localVarPostBody = createAdminAgencyRelationDTO;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling createAdminAgencyRelation");
        }
        
        // verify the required parameter 'createAdminAgencyRelationDTO' is set
        if (createAdminAgencyRelationDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'createAdminAgencyRelationDTO' when calling createAdminAgencyRelation");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins/{adminId}/agencies", HttpMethod.POST, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Creates a new Agency Admin [Authorization: Role: user-admin]
     * 
     * <p><b>201</b> - CREATED - Agency Admin was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>409</b> - CONFLICT - invalid/unavailable username or email address
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param createAdminDTO  (required)
     * @return AdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminResponseDTO createAgencyAdmin(CreateAdminDTO createAdminDTO) throws RestClientException {
        return createAgencyAdminWithHttpInfo(createAdminDTO).getBody();
    }

    /**
     * Creates a new Agency Admin [Authorization: Role: user-admin]
     * 
     * <p><b>201</b> - CREATED - Agency Admin was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>409</b> - CONFLICT - invalid/unavailable username or email address
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param createAdminDTO  (required)
     * @return ResponseEntity&lt;AdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminResponseDTO> createAgencyAdminWithHttpInfo(CreateAdminDTO createAdminDTO) throws RestClientException {
        Object localVarPostBody = createAdminDTO;
        
        // verify the required parameter 'createAdminDTO' is set
        if (createAdminDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'createAdminDTO' when calling createAgencyAdmin");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminResponseDTO> localReturnType = new ParameterizedTypeReference<AdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Creates a new consultant [Authorization: Role: consultant-admin]
     * 
     * <p><b>201</b> - CREATED - consultant was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>409</b> - CONFLICT - invalid/unavailable username or email address
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param createConsultantDTO  (required)
     * @return ConsultantAdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ConsultantAdminResponseDTO createConsultant(CreateConsultantDTO createConsultantDTO) throws RestClientException {
        return createConsultantWithHttpInfo(createConsultantDTO).getBody();
    }

    /**
     * Creates a new consultant [Authorization: Role: consultant-admin]
     * 
     * <p><b>201</b> - CREATED - consultant was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>409</b> - CONFLICT - invalid/unavailable username or email address
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param createConsultantDTO  (required)
     * @return ResponseEntity&lt;ConsultantAdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ConsultantAdminResponseDTO> createConsultantWithHttpInfo(CreateConsultantDTO createConsultantDTO) throws RestClientException {
        Object localVarPostBody = createConsultantDTO;
        
        // verify the required parameter 'createConsultantDTO' is set
        if (createConsultantDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'createConsultantDTO' when calling createConsultant");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<ConsultantAdminResponseDTO> localReturnType = new ParameterizedTypeReference<ConsultantAdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/consultants", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Create a consultant-agency relation [Authorization: Role: user-admin]
     * 
     * <p><b>201</b> - CREATED - consultant-agency relation was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId Consultant Id (required)
     * @param createConsultantAgencyDTO  (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void createConsultantAgency(String consultantId, CreateConsultantAgencyDTO createConsultantAgencyDTO) throws RestClientException {
        createConsultantAgencyWithHttpInfo(consultantId, createConsultantAgencyDTO);
    }

    /**
     * Create a consultant-agency relation [Authorization: Role: user-admin]
     * 
     * <p><b>201</b> - CREATED - consultant-agency relation was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId Consultant Id (required)
     * @param createConsultantAgencyDTO  (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> createConsultantAgencyWithHttpInfo(String consultantId, CreateConsultantAgencyDTO createConsultantAgencyDTO) throws RestClientException {
        Object localVarPostBody = createConsultantAgencyDTO;
        
        // verify the required parameter 'consultantId' is set
        if (consultantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultantId' when calling createConsultantAgency");
        }
        
        // verify the required parameter 'createConsultantAgencyDTO' is set
        if (createConsultantAgencyDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'createConsultantAgencyDTO' when calling createConsultantAgency");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultantId", consultantId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/consultants/{consultantId}/agencies", HttpMethod.POST, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Creates a new Tenant Admin [Authorization: Role: tenant-admin]
     * 
     * <p><b>201</b> - CREATED - Agency Admin was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>409</b> - CONFLICT - invalid/unavailable username or email address
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param createAdminDTO  (required)
     * @return AdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminResponseDTO createTenantAdmin(CreateAdminDTO createAdminDTO) throws RestClientException {
        return createTenantAdminWithHttpInfo(createAdminDTO).getBody();
    }

    /**
     * Creates a new Tenant Admin [Authorization: Role: tenant-admin]
     * 
     * <p><b>201</b> - CREATED - Agency Admin was created successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>409</b> - CONFLICT - invalid/unavailable username or email address
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param createAdminDTO  (required)
     * @return ResponseEntity&lt;AdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminResponseDTO> createTenantAdminWithHttpInfo(CreateAdminDTO createAdminDTO) throws RestClientException {
        Object localVarPostBody = createAdminDTO;
        
        // verify the required parameter 'createAdminDTO' is set
        if (createAdminDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'createAdminDTO' when calling createTenantAdmin");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminResponseDTO> localReturnType = new ParameterizedTypeReference<AdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/tenantadmins", HttpMethod.POST, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Delete a admin-agency relation [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - admin-agency relation was deleted successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId Admin Id (required)
     * @param agencyId Agency Id (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void deleteAdminAgencyRelation(String adminId, Long agencyId) throws RestClientException {
        deleteAdminAgencyRelationWithHttpInfo(adminId, agencyId);
    }

    /**
     * Delete a admin-agency relation [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - admin-agency relation was deleted successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId Admin Id (required)
     * @param agencyId Agency Id (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteAdminAgencyRelationWithHttpInfo(String adminId, Long agencyId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling deleteAdminAgencyRelation");
        }
        
        // verify the required parameter 'agencyId' is set
        if (agencyId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'agencyId' when calling deleteAdminAgencyRelation");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);
        uriVariables.put("agencyId", agencyId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins/{adminId}/agencies/{agencyId}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * delete an agency admin [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - agency admin was marked for deletion successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - agency admin not found
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin id (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void deleteAgencyAdmin(String adminId) throws RestClientException {
        deleteAgencyAdminWithHttpInfo(adminId);
    }

    /**
     * delete an agency admin [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - agency admin was marked for deletion successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - agency admin not found
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin id (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteAgencyAdminWithHttpInfo(String adminId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling deleteAgencyAdmin");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins/{adminId}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Delete a consultant-agency relation [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - consultant-agency relation was deleted successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId Consultant Id (required)
     * @param agencyId Agency Id (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void deleteConsultantAgency(String consultantId, Long agencyId) throws RestClientException {
        deleteConsultantAgencyWithHttpInfo(consultantId, agencyId);
    }

    /**
     * Delete a consultant-agency relation [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - consultant-agency relation was deleted successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId Consultant Id (required)
     * @param agencyId Agency Id (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteConsultantAgencyWithHttpInfo(String consultantId, Long agencyId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'consultantId' is set
        if (consultantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultantId' when calling deleteConsultantAgency");
        }
        
        // verify the required parameter 'agencyId' is set
        if (agencyId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'agencyId' when calling deleteConsultantAgency");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultantId", consultantId);
        uriVariables.put("agencyId", agencyId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/consultants/{consultantId}/agencies/{agencyId}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * delete tenant admin [Authorization: Role: tenant-admin]
     * 
     * <p><b>200</b> - OK - agency admin was marked for deletion successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - agency admin not found
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin id (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void deleteTenantAdmin(String adminId) throws RestClientException {
        deleteTenantAdminWithHttpInfo(adminId);
    }

    /**
     * delete tenant admin [Authorization: Role: tenant-admin]
     * 
     * <p><b>200</b> - OK - agency admin was marked for deletion successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - agency admin not found
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin id (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteTenantAdminWithHttpInfo(String adminId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling deleteTenantAdmin");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/tenantadmins/{adminId}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns an generated report containing data integration violations. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @return List&lt;ViolationDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public List<ViolationDTO> generateViolationReport() throws RestClientException {
        return generateViolationReportWithHttpInfo().getBody();
    }

    /**
     * Returns an generated report containing data integration violations. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @return ResponseEntity&lt;List&lt;ViolationDTO&gt;&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<ViolationDTO>> generateViolationReportWithHttpInfo() throws RestClientException {
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

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<List<ViolationDTO>> localReturnType = new ParameterizedTypeReference<List<ViolationDTO>>() {};
        return apiClient.invokeAPI("/useradmin/report", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the list of agencies for a given admin Id. [Authorization: Role: restricted-agency-admin]
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - consultant with given id does not exist
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin Id (required)
     * @return List&lt;Long&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public List<Long> getAdminAgencies(String adminId) throws RestClientException {
        return getAdminAgenciesWithHttpInfo(adminId).getBody();
    }

    /**
     * Returns the list of agencies for a given admin Id. [Authorization: Role: restricted-agency-admin]
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - consultant with given id does not exist
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin Id (required)
     * @return ResponseEntity&lt;List&lt;Long&gt;&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<Long>> getAdminAgenciesWithHttpInfo(String adminId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling getAdminAgencies");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

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

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<List<Long>> localReturnType = new ParameterizedTypeReference<List<Long>>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins/{adminId}/agencies", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Get an agency admin by given id [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - consultant found
     * <p><b>204</b> - NO CONTENT - agency admin with the specific id was not found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin id (required)
     * @return AdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminResponseDTO getAgencyAdmin(String adminId) throws RestClientException {
        return getAgencyAdminWithHttpInfo(adminId).getBody();
    }

    /**
     * Get an agency admin by given id [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - consultant found
     * <p><b>204</b> - NO CONTENT - agency admin with the specific id was not found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin id (required)
     * @return ResponseEntity&lt;AdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminResponseDTO> getAgencyAdminWithHttpInfo(String adminId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling getAgencyAdmin");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

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

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminResponseDTO> localReturnType = new ParameterizedTypeReference<AdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins/{adminId}", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the list of agency admins by filter query parameter. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param page Number of page where to start in the query (1 &#x3D; first page) (required)
     * @param perPage Number of items which are being returned per page (required)
     * @param filter The filter parameters to search for. If no filter is set all admins are being returned. (optional)
     * @param sort The sort parameter containing field and order the response should be sorted (optional)
     * @return AdminSearchResultDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminSearchResultDTO getAgencyAdmins(Integer page, Integer perPage, AdminFilter filter, Sort sort) throws RestClientException {
        return getAgencyAdminsWithHttpInfo(page, perPage, filter, sort).getBody();
    }

    /**
     * Returns the list of agency admins by filter query parameter. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param page Number of page where to start in the query (1 &#x3D; first page) (required)
     * @param perPage Number of items which are being returned per page (required)
     * @param filter The filter parameters to search for. If no filter is set all admins are being returned. (optional)
     * @param sort The sort parameter containing field and order the response should be sorted (optional)
     * @return ResponseEntity&lt;AdminSearchResultDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminSearchResultDTO> getAgencyAdminsWithHttpInfo(Integer page, Integer perPage, AdminFilter filter, Sort sort) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'page' is set
        if (page == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'page' when calling getAgencyAdmins");
        }
        
        // verify the required parameter 'perPage' is set
        if (perPage == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'perPage' when calling getAgencyAdmins");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "perPage", perPage));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "sort", sort));

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminSearchResultDTO> localReturnType = new ParameterizedTypeReference<AdminSearchResultDTO>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the list of consultants for a given agency Id. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - agency with given id does not exist
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param agencyId Agency Id (required)
     * @return AgencyConsultantResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AgencyConsultantResponseDTO getAgencyConsultants(String agencyId) throws RestClientException {
        return getAgencyConsultantsWithHttpInfo(agencyId).getBody();
    }

    /**
     * Returns the list of consultants for a given agency Id. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - agency with given id does not exist
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param agencyId Agency Id (required)
     * @return ResponseEntity&lt;AgencyConsultantResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AgencyConsultantResponseDTO> getAgencyConsultantsWithHttpInfo(String agencyId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'agencyId' is set
        if (agencyId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'agencyId' when calling getAgencyConsultants");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("agencyId", agencyId);

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

        ParameterizedTypeReference<AgencyConsultantResponseDTO> localReturnType = new ParameterizedTypeReference<AgencyConsultantResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/agencies/{agencyId}/consultants", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Get an asker by given id [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - asker found
     * <p><b>204</b> - NO CONTENT - asker with the specific id was not found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param askerId asker id (required)
     * @return AskerResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AskerResponseDTO getAsker(String askerId) throws RestClientException {
        return getAskerWithHttpInfo(askerId).getBody();
    }

    /**
     * Get an asker by given id [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - asker found
     * <p><b>204</b> - NO CONTENT - asker with the specific id was not found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param askerId asker id (required)
     * @return ResponseEntity&lt;AskerResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AskerResponseDTO> getAskerWithHttpInfo(String askerId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'askerId' is set
        if (askerId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'askerId' when calling getAsker");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("askerId", askerId);

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

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AskerResponseDTO> localReturnType = new ParameterizedTypeReference<AskerResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/askers/{askerId}", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Get a consultant by given id [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - consultant found
     * <p><b>204</b> - NO CONTENT - consultant with the specific id was not found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId consultant id (required)
     * @return ConsultantAdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ConsultantAdminResponseDTO getConsultant(String consultantId) throws RestClientException {
        return getConsultantWithHttpInfo(consultantId).getBody();
    }

    /**
     * Get a consultant by given id [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - consultant found
     * <p><b>204</b> - NO CONTENT - consultant with the specific id was not found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId consultant id (required)
     * @return ResponseEntity&lt;ConsultantAdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ConsultantAdminResponseDTO> getConsultantWithHttpInfo(String consultantId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'consultantId' is set
        if (consultantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultantId' when calling getConsultant");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultantId", consultantId);

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

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<ConsultantAdminResponseDTO> localReturnType = new ParameterizedTypeReference<ConsultantAdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/consultants/{consultantId}", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the list of agencies for a given consultant Id. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - consultant with given id does not exist
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId Consultant Id (required)
     * @return ConsultantAgencyResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ConsultantAgencyResponseDTO getConsultantAgencies(String consultantId) throws RestClientException {
        return getConsultantAgenciesWithHttpInfo(consultantId).getBody();
    }

    /**
     * Returns the list of agencies for a given consultant Id. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - consultant with given id does not exist
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId Consultant Id (required)
     * @return ResponseEntity&lt;ConsultantAgencyResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ConsultantAgencyResponseDTO> getConsultantAgenciesWithHttpInfo(String consultantId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'consultantId' is set
        if (consultantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultantId' when calling getConsultantAgencies");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultantId", consultantId);

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

        ParameterizedTypeReference<ConsultantAgencyResponseDTO> localReturnType = new ParameterizedTypeReference<ConsultantAgencyResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/consultants/{consultantId}/agencies", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the list of consultants by filter query parameter. [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param page Number of page where to start in the query (1 &#x3D; first page) (required)
     * @param perPage Number of items which are being returned per page (required)
     * @param filter The filter parameters to search for. If no filter is set all consultant are being returned. (optional)
     * @param sort The sort parameter containing field and order the response should be sorted (optional)
     * @return ConsultantSearchResultDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ConsultantSearchResultDTO getConsultants(Integer page, Integer perPage, ConsultantFilter filter, Sort sort) throws RestClientException {
        return getConsultantsWithHttpInfo(page, perPage, filter, sort).getBody();
    }

    /**
     * Returns the list of consultants by filter query parameter. [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param page Number of page where to start in the query (1 &#x3D; first page) (required)
     * @param perPage Number of items which are being returned per page (required)
     * @param filter The filter parameters to search for. If no filter is set all consultant are being returned. (optional)
     * @param sort The sort parameter containing field and order the response should be sorted (optional)
     * @return ResponseEntity&lt;ConsultantSearchResultDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ConsultantSearchResultDTO> getConsultantsWithHttpInfo(Integer page, Integer perPage, ConsultantFilter filter, Sort sort) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'page' is set
        if (page == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'page' when calling getConsultants");
        }
        
        // verify the required parameter 'perPage' is set
        if (perPage == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'perPage' when calling getConsultants");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "perPage", perPage));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "sort", sort));

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<ConsultantSearchResultDTO> localReturnType = new ParameterizedTypeReference<ConsultantSearchResultDTO>() {};
        return apiClient.invokeAPI("/useradmin/consultants", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the list of sessions by filter query parameter. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param page Number of page where to start in the query (1 &#x3D; first page) (required)
     * @param perPage Number of items which are being returned per page (required)
     * @param filter The filter parameters to search for. If no filter is set all sessions are being returned. If more than one filter is set the first given filter is used only. (optional)
     * @return SessionAdminResultDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public SessionAdminResultDTO getSessions(Integer page, Integer perPage, SessionFilter filter) throws RestClientException {
        return getSessionsWithHttpInfo(page, perPage, filter).getBody();
    }

    /**
     * Returns the list of sessions by filter query parameter. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successfull operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param page Number of page where to start in the query (1 &#x3D; first page) (required)
     * @param perPage Number of items which are being returned per page (required)
     * @param filter The filter parameters to search for. If no filter is set all sessions are being returned. If more than one filter is set the first given filter is used only. (optional)
     * @return ResponseEntity&lt;SessionAdminResultDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<SessionAdminResultDTO> getSessionsWithHttpInfo(Integer page, Integer perPage, SessionFilter filter) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'page' is set
        if (page == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'page' when calling getSessions");
        }
        
        // verify the required parameter 'perPage' is set
        if (perPage == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'perPage' when calling getSessions");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "filter", filter));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "perPage", perPage));

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<SessionAdminResultDTO> localReturnType = new ParameterizedTypeReference<SessionAdminResultDTO>() {};
        return apiClient.invokeAPI("/useradmin/sessions", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Get a tenant admin by given id [Authorization: Role: tenant-admin]
     * 
     * <p><b>200</b> - OK - consultant found
     * <p><b>204</b> - NO CONTENT - agency admin with the specific id was not found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin id (required)
     * @return AdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminResponseDTO getTenantAdmin(String adminId) throws RestClientException {
        return getTenantAdminWithHttpInfo(adminId).getBody();
    }

    /**
     * Get a tenant admin by given id [Authorization: Role: tenant-admin]
     * 
     * <p><b>200</b> - OK - consultant found
     * <p><b>204</b> - NO CONTENT - agency admin with the specific id was not found
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId admin id (required)
     * @return ResponseEntity&lt;AdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminResponseDTO> getTenantAdminWithHttpInfo(String adminId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling getTenantAdmin");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

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

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminResponseDTO> localReturnType = new ParameterizedTypeReference<AdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/tenantadmins/{adminId}", HttpMethod.GET, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Returns the list of tenant admins by filter query parameter. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param tenantId tenant id (required)
     * @return List&lt;AdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public List<AdminResponseDTO> getTenantAdmins(Integer tenantId) throws RestClientException {
        return getTenantAdminsWithHttpInfo(tenantId).getBody();
    }

    /**
     * Returns the list of tenant admins by filter query parameter. [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param tenantId tenant id (required)
     * @return ResponseEntity&lt;List&lt;AdminResponseDTO&gt;&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<List<AdminResponseDTO>> getTenantAdminsWithHttpInfo(Integer tenantId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'tenantId' is set
        if (tenantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'tenantId' when calling getTenantAdmins");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "tenantId", tenantId));

        final String[] localVarAccepts = { 
            "application/json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<List<AdminResponseDTO>> localReturnType = new ParameterizedTypeReference<List<AdminResponseDTO>>() {};
        return apiClient.invokeAPI("/useradmin/tenantadmins", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Delete a asker by given id [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - asker was marked for deletion successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - asker not found
     * <p><b>409</b> - CONFLICT - asker is already marked for deletion
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param askerId asker id (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void markAskerForDeletion(String askerId) throws RestClientException {
        markAskerForDeletionWithHttpInfo(askerId);
    }

    /**
     * Delete a asker by given id [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - asker was marked for deletion successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - asker not found
     * <p><b>409</b> - CONFLICT - asker is already marked for deletion
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param askerId asker id (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> markAskerForDeletionWithHttpInfo(String askerId) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'askerId' is set
        if (askerId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'askerId' when calling markAskerForDeletion");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("askerId", askerId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/askers/{askerId}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Mark a consultant for deletion [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - consultant was marked for deletion successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - consultant not found
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId consultant id (required)
     * @param forceDeleteSessions parameter specifying if consultant sessions need to be removed prior the deletion (optional)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void markConsultantForDeletion(String consultantId, Boolean forceDeleteSessions) throws RestClientException {
        markConsultantForDeletionWithHttpInfo(consultantId, forceDeleteSessions);
    }

    /**
     * Mark a consultant for deletion [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - consultant was marked for deletion successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>404</b> - NOT FOUND - consultant not found
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId consultant id (required)
     * @param forceDeleteSessions parameter specifying if consultant sessions need to be removed prior the deletion (optional)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> markConsultantForDeletionWithHttpInfo(String consultantId, Boolean forceDeleteSessions) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'consultantId' is set
        if (consultantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultantId' when calling markConsultantForDeletion");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultantId", consultantId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "forceDeleteSessions", forceDeleteSessions));

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/consultants/{consultantId}", HttpMethod.DELETE, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Sets or updates the email, first and lastname of authorized admin account [Authorization:  Role: single-tenant-admin, restricted-agency-admin]
     * 
     * <p><b>200</b> - OK - admin was updated successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid Keycloak token
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>409</b> - CONFLICT - invalid/unavailable email
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param patchAdminDTO  (required)
     * @return AdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminResponseDTO patchAdminData(PatchAdminDTO patchAdminDTO) throws RestClientException {
        return patchAdminDataWithHttpInfo(patchAdminDTO).getBody();
    }

    /**
     * Sets or updates the email, first and lastname of authorized admin account [Authorization:  Role: single-tenant-admin, restricted-agency-admin]
     * 
     * <p><b>200</b> - OK - admin was updated successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid Keycloak token
     * <p><b>403</b> - FORBIDDEN - no/invalid role/authorization or CSRF token
     * <p><b>409</b> - CONFLICT - invalid/unavailable email
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param patchAdminDTO  (required)
     * @return ResponseEntity&lt;AdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminResponseDTO> patchAdminDataWithHttpInfo(PatchAdminDTO patchAdminDTO) throws RestClientException {
        Object localVarPostBody = patchAdminDTO;
        
        // verify the required parameter 'patchAdminDTO' is set
        if (patchAdminDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'patchAdminDTO' when calling patchAdminData");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminResponseDTO> localReturnType = new ParameterizedTypeReference<AdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/data", HttpMethod.PATCH, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Get Agency admins matching the given query [Auth: user-admin]
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param query URL-encoded infix to search for in id first name, last name, or email. A non-encoded star symbol searches for all. (required)
     * @param page Page number (first page &#x3D; 1) (optional, default to 1)
     * @param perPage Number of items returned per page (optional, default to 10)
     * @param field field to sort by (optional, default to FIRSTNAME)
     * @param order sort order (optional, default to ASC)
     * @return AdminSearchResultDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminSearchResultDTO searchAgencyAdmins(String query, Integer page, Integer perPage, String field, String order) throws RestClientException {
        return searchAgencyAdminsWithHttpInfo(query, page, perPage, field, order).getBody();
    }

    /**
     * Get Agency admins matching the given query [Auth: user-admin]
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param query URL-encoded infix to search for in id first name, last name, or email. A non-encoded star symbol searches for all. (required)
     * @param page Page number (first page &#x3D; 1) (optional, default to 1)
     * @param perPage Number of items returned per page (optional, default to 10)
     * @param field field to sort by (optional, default to FIRSTNAME)
     * @param order sort order (optional, default to ASC)
     * @return ResponseEntity&lt;AdminSearchResultDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminSearchResultDTO> searchAgencyAdminsWithHttpInfo(String query, Integer page, Integer perPage, String field, String order) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'query' is set
        if (query == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'query' when calling searchAgencyAdmins");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "query", query));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "perPage", perPage));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "field", field));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "order", order));

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminSearchResultDTO> localReturnType = new ParameterizedTypeReference<AdminSearchResultDTO>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins/search", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Get tenant admins matching the given query [Auth: tenant-admin]
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param query URL-encoded infix to search for in first name, last name, email or tenant id. A non-encoded star symbol searches for all. (required)
     * @param page Page number (first page &#x3D; 1) (optional, default to 1)
     * @param perPage Number of items returned per page (optional, default to 10)
     * @param field field to sort by (optional, default to FIRSTNAME)
     * @param order sort order (optional, default to ASC)
     * @return AdminSearchResultDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminSearchResultDTO searchTenantAdmins(String query, Integer page, Integer perPage, String field, String order) throws RestClientException {
        return searchTenantAdminsWithHttpInfo(query, page, perPage, field, order).getBody();
    }

    /**
     * Get tenant admins matching the given query [Auth: tenant-admin]
     * 
     * <p><b>200</b> - OK - successful operation
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request or body object
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param query URL-encoded infix to search for in first name, last name, email or tenant id. A non-encoded star symbol searches for all. (required)
     * @param page Page number (first page &#x3D; 1) (optional, default to 1)
     * @param perPage Number of items returned per page (optional, default to 10)
     * @param field field to sort by (optional, default to FIRSTNAME)
     * @param order sort order (optional, default to ASC)
     * @return ResponseEntity&lt;AdminSearchResultDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminSearchResultDTO> searchTenantAdminsWithHttpInfo(String query, Integer page, Integer perPage, String field, String order) throws RestClientException {
        Object localVarPostBody = null;
        
        // verify the required parameter 'query' is set
        if (query == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'query' when calling searchTenantAdmins");
        }
        

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "query", query));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "page", page));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "perPage", perPage));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "field", field));
        localVarQueryParams.putAll(apiClient.parameterToMultiValueMap(null, "order", order));

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = {  };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminSearchResultDTO> localReturnType = new ParameterizedTypeReference<AdminSearchResultDTO>() {};
        return apiClient.invokeAPI("/useradmin/tenantadmins/search", HttpMethod.GET, Collections.<String, Object>emptyMap(), localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Set admin-agency relations [Authorization: Role: user-admin]
     * Existing relations are deleted, passed relations added.
     * <p><b>200</b> - OK - admin-agency relations have been set
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId  (required)
     * @param createAdminAgencyRelationDTO  (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void setAdminAgenciesRelation(String adminId, List<CreateAdminAgencyRelationDTO> createAdminAgencyRelationDTO) throws RestClientException {
        setAdminAgenciesRelationWithHttpInfo(adminId, createAdminAgencyRelationDTO);
    }

    /**
     * Set admin-agency relations [Authorization: Role: user-admin]
     * Existing relations are deleted, passed relations added.
     * <p><b>200</b> - OK - admin-agency relations have been set
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId  (required)
     * @param createAdminAgencyRelationDTO  (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> setAdminAgenciesRelationWithHttpInfo(String adminId, List<CreateAdminAgencyRelationDTO> createAdminAgencyRelationDTO) throws RestClientException {
        Object localVarPostBody = createAdminAgencyRelationDTO;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling setAdminAgenciesRelation");
        }
        
        // verify the required parameter 'createAdminAgencyRelationDTO' is set
        if (createAdminAgencyRelationDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'createAdminAgencyRelationDTO' when calling setAdminAgenciesRelation");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins/{adminId}/agencies", HttpMethod.PUT, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Set consultant-agency relations [Authorization: Role: user-admin]
     * Existing relations are deleted, passed relations added.
     * <p><b>200</b> - OK - consultant-agency relations have been set
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId  (required)
     * @param createConsultantAgencyDTO  (required)
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public void setConsultantAgencies(String consultantId, List<CreateConsultantAgencyDTO> createConsultantAgencyDTO) throws RestClientException {
        setConsultantAgenciesWithHttpInfo(consultantId, createConsultantAgencyDTO);
    }

    /**
     * Set consultant-agency relations [Authorization: Role: user-admin]
     * Existing relations are deleted, passed relations added.
     * <p><b>200</b> - OK - consultant-agency relations have been set
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId  (required)
     * @param createConsultantAgencyDTO  (required)
     * @return ResponseEntity&lt;Void&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> setConsultantAgenciesWithHttpInfo(String consultantId, List<CreateConsultantAgencyDTO> createConsultantAgencyDTO) throws RestClientException {
        Object localVarPostBody = createConsultantAgencyDTO;
        
        // verify the required parameter 'consultantId' is set
        if (consultantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultantId' when calling setConsultantAgencies");
        }
        
        // verify the required parameter 'createConsultantAgencyDTO' is set
        if (createConsultantAgencyDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'createConsultantAgencyDTO' when calling setConsultantAgencies");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultantId", consultantId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = {  };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<Void> localReturnType = new ParameterizedTypeReference<Void>() {};
        return apiClient.invokeAPI("/useradmin/consultants/{consultantId}/agencies", HttpMethod.PUT, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Updates an agency admin [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - agency admin was updated successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId agency admin id (required)
     * @param updateAgencyAdminDTO  (required)
     * @return AdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminResponseDTO updateAgencyAdmin(String adminId, UpdateAgencyAdminDTO updateAgencyAdminDTO) throws RestClientException {
        return updateAgencyAdminWithHttpInfo(adminId, updateAgencyAdminDTO).getBody();
    }

    /**
     * Updates an agency admin [Authorization: Role: user-admin]
     * 
     * <p><b>200</b> - OK - agency admin was updated successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId agency admin id (required)
     * @param updateAgencyAdminDTO  (required)
     * @return ResponseEntity&lt;AdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminResponseDTO> updateAgencyAdminWithHttpInfo(String adminId, UpdateAgencyAdminDTO updateAgencyAdminDTO) throws RestClientException {
        Object localVarPostBody = updateAgencyAdminDTO;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling updateAgencyAdmin");
        }
        
        // verify the required parameter 'updateAgencyAdminDTO' is set
        if (updateAgencyAdminDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'updateAgencyAdminDTO' when calling updateAgencyAdmin");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminResponseDTO> localReturnType = new ParameterizedTypeReference<AdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/agencyadmins/{adminId}", HttpMethod.PUT, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Updates a consultant [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - consultant was updated successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId consultant id (required)
     * @param updateAdminConsultantDTO  (required)
     * @return ConsultantAdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ConsultantAdminResponseDTO updateConsultant(String consultantId, UpdateAdminConsultantDTO updateAdminConsultantDTO) throws RestClientException {
        return updateConsultantWithHttpInfo(consultantId, updateAdminConsultantDTO).getBody();
    }

    /**
     * Updates a consultant [Authorization: Role: consultant-admin]
     * 
     * <p><b>200</b> - OK - consultant was updated successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param consultantId consultant id (required)
     * @param updateAdminConsultantDTO  (required)
     * @return ResponseEntity&lt;ConsultantAdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ConsultantAdminResponseDTO> updateConsultantWithHttpInfo(String consultantId, UpdateAdminConsultantDTO updateAdminConsultantDTO) throws RestClientException {
        Object localVarPostBody = updateAdminConsultantDTO;
        
        // verify the required parameter 'consultantId' is set
        if (consultantId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'consultantId' when calling updateConsultant");
        }
        
        // verify the required parameter 'updateAdminConsultantDTO' is set
        if (updateAdminConsultantDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'updateAdminConsultantDTO' when calling updateConsultant");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("consultantId", consultantId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<ConsultantAdminResponseDTO> localReturnType = new ParameterizedTypeReference<ConsultantAdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/consultants/{consultantId}", HttpMethod.PUT, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
    /**
     * Updates a tenant admin [Authorization: Role: tenant-admin]
     * 
     * <p><b>200</b> - OK - agency admin was updated successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId agency admin id (required)
     * @param updateTenantAdminDTO  (required)
     * @return AdminResponseDTO
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public AdminResponseDTO updateTenantAdmin(String adminId, UpdateTenantAdminDTO updateTenantAdminDTO) throws RestClientException {
        return updateTenantAdminWithHttpInfo(adminId, updateTenantAdminDTO).getBody();
    }

    /**
     * Updates a tenant admin [Authorization: Role: tenant-admin]
     * 
     * <p><b>200</b> - OK - agency admin was updated successfully
     * <p><b>400</b> - BAD REQUEST - invalid/incomplete request
     * <p><b>401</b> - UNAUTHORIZED - no/invalid role/authorization
     * <p><b>500</b> - INTERNAL SERVER ERROR - server encountered unexpected condition
     * @param adminId agency admin id (required)
     * @param updateTenantAdminDTO  (required)
     * @return ResponseEntity&lt;AdminResponseDTO&gt;
     * @throws RestClientException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<AdminResponseDTO> updateTenantAdminWithHttpInfo(String adminId, UpdateTenantAdminDTO updateTenantAdminDTO) throws RestClientException {
        Object localVarPostBody = updateTenantAdminDTO;
        
        // verify the required parameter 'adminId' is set
        if (adminId == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'adminId' when calling updateTenantAdmin");
        }
        
        // verify the required parameter 'updateTenantAdminDTO' is set
        if (updateTenantAdminDTO == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'updateTenantAdminDTO' when calling updateTenantAdmin");
        }
        
        // create path and map variables
        final Map<String, Object> uriVariables = new HashMap<String, Object>();
        uriVariables.put("adminId", adminId);

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/hal+json"
         };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
         };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "Bearer" };

        ParameterizedTypeReference<AdminResponseDTO> localReturnType = new ParameterizedTypeReference<AdminResponseDTO>() {};
        return apiClient.invokeAPI("/useradmin/tenantadmins/{adminId}", HttpMethod.PUT, uriVariables, localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAccept, localVarContentType, localVarAuthNames, localReturnType);
    }
}
