# AdminUserControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createAdminAgencyRelation**](AdminUserControllerApi.md#createAdminAgencyRelation) | **POST** /useradmin/agencyadmins/{adminId}/agencies | Create an admin-agency relation [Authorization: Role: user-admin] |
| [**createAgencyAdmin**](AdminUserControllerApi.md#createAgencyAdmin) | **POST** /useradmin/agencyadmins | Creates a new Agency Admin [Authorization: Role: user-admin] |
| [**createConsultant**](AdminUserControllerApi.md#createConsultant) | **POST** /useradmin/consultants | Creates a new consultant [Authorization: Role: consultant-admin] |
| [**createConsultantAgency**](AdminUserControllerApi.md#createConsultantAgency) | **POST** /useradmin/consultants/{consultantId}/agencies | Create a consultant-agency relation [Authorization: Role: user-admin] |
| [**createTenantAdmin**](AdminUserControllerApi.md#createTenantAdmin) | **POST** /useradmin/tenantadmins | Creates a new Tenant Admin [Authorization: Role: tenant-admin] |
| [**deleteAdminAgencyRelation**](AdminUserControllerApi.md#deleteAdminAgencyRelation) | **DELETE** /useradmin/agencyadmins/{adminId}/agencies/{agencyId} | Delete a admin-agency relation [Authorization: Role: user-admin] |
| [**deleteAgencyAdmin**](AdminUserControllerApi.md#deleteAgencyAdmin) | **DELETE** /useradmin/agencyadmins/{adminId} | delete an agency admin [Authorization: Role: user-admin] |
| [**deleteConsultantAgency**](AdminUserControllerApi.md#deleteConsultantAgency) | **DELETE** /useradmin/consultants/{consultantId}/agencies/{agencyId} | Delete a consultant-agency relation [Authorization: Role: user-admin] |
| [**deleteTenantAdmin**](AdminUserControllerApi.md#deleteTenantAdmin) | **DELETE** /useradmin/tenantadmins/{adminId} | delete tenant admin [Authorization: Role: tenant-admin] |
| [**generateViolationReport**](AdminUserControllerApi.md#generateViolationReport) | **GET** /useradmin/report | Returns an generated report containing data integration violations. [Authorization: Role: user-admin] |
| [**getAdminAgencies**](AdminUserControllerApi.md#getAdminAgencies) | **GET** /useradmin/agencyadmins/{adminId}/agencies | Returns the list of agencies for a given admin Id. [Authorization: Role: restricted-agency-admin] |
| [**getAgencyAdmin**](AdminUserControllerApi.md#getAgencyAdmin) | **GET** /useradmin/agencyadmins/{adminId} | Get an agency admin by given id [Authorization: Role: user-admin] |
| [**getAgencyAdmins**](AdminUserControllerApi.md#getAgencyAdmins) | **GET** /useradmin/agencyadmins | Returns the list of agency admins by filter query parameter. [Authorization: Role: user-admin] |
| [**getAgencyConsultants**](AdminUserControllerApi.md#getAgencyConsultants) | **GET** /useradmin/agencies/{agencyId}/consultants | Returns the list of consultants for a given agency Id. [Authorization: Role: user-admin] |
| [**getAsker**](AdminUserControllerApi.md#getAsker) | **GET** /useradmin/askers/{askerId} | Get an asker by given id [Authorization: Role: consultant-admin] |
| [**getConsultant**](AdminUserControllerApi.md#getConsultant) | **GET** /useradmin/consultants/{consultantId} | Get a consultant by given id [Authorization: Role: consultant-admin] |
| [**getConsultantAgencies**](AdminUserControllerApi.md#getConsultantAgencies) | **GET** /useradmin/consultants/{consultantId}/agencies | Returns the list of agencies for a given consultant Id. [Authorization: Role: user-admin] |
| [**getConsultants**](AdminUserControllerApi.md#getConsultants) | **GET** /useradmin/consultants | Returns the list of consultants by filter query parameter. [Authorization: Role: consultant-admin] |
| [**getSessions**](AdminUserControllerApi.md#getSessions) | **GET** /useradmin/sessions | Returns the list of sessions by filter query parameter. [Authorization: Role: user-admin] |
| [**getTenantAdmin**](AdminUserControllerApi.md#getTenantAdmin) | **GET** /useradmin/tenantadmins/{adminId} | Get a tenant admin by given id [Authorization: Role: tenant-admin] |
| [**getTenantAdmins**](AdminUserControllerApi.md#getTenantAdmins) | **GET** /useradmin/tenantadmins | Returns the list of tenant admins by filter query parameter. [Authorization: Role: user-admin] |
| [**markAskerForDeletion**](AdminUserControllerApi.md#markAskerForDeletion) | **DELETE** /useradmin/askers/{askerId} | Delete a asker by given id [Authorization: Role: consultant-admin] |
| [**markConsultantForDeletion**](AdminUserControllerApi.md#markConsultantForDeletion) | **DELETE** /useradmin/consultants/{consultantId} | Mark a consultant for deletion [Authorization: Role: consultant-admin] |
| [**patchAdminData**](AdminUserControllerApi.md#patchAdminData) | **PATCH** /useradmin/data | Sets or updates the email, first and lastname of authorized admin account [Authorization:  Role: single-tenant-admin, restricted-agency-admin] |
| [**searchAgencyAdmins**](AdminUserControllerApi.md#searchAgencyAdmins) | **GET** /useradmin/agencyadmins/search | Get Agency admins matching the given query [Auth: user-admin] |
| [**searchTenantAdmins**](AdminUserControllerApi.md#searchTenantAdmins) | **GET** /useradmin/tenantadmins/search | Get tenant admins matching the given query [Auth: tenant-admin] |
| [**setAdminAgenciesRelation**](AdminUserControllerApi.md#setAdminAgenciesRelation) | **PUT** /useradmin/agencyadmins/{adminId}/agencies | Set admin-agency relations [Authorization: Role: user-admin] |
| [**setConsultantAgencies**](AdminUserControllerApi.md#setConsultantAgencies) | **PUT** /useradmin/consultants/{consultantId}/agencies | Set consultant-agency relations [Authorization: Role: user-admin] |
| [**updateAgencyAdmin**](AdminUserControllerApi.md#updateAgencyAdmin) | **PUT** /useradmin/agencyadmins/{adminId} | Updates an agency admin [Authorization: Role: user-admin] |
| [**updateConsultant**](AdminUserControllerApi.md#updateConsultant) | **PUT** /useradmin/consultants/{consultantId} | Updates a consultant [Authorization: Role: consultant-admin] |
| [**updateTenantAdmin**](AdminUserControllerApi.md#updateTenantAdmin) | **PUT** /useradmin/tenantadmins/{adminId} | Updates a tenant admin [Authorization: Role: tenant-admin] |



## createAdminAgencyRelation

> createAdminAgencyRelation(adminId, createAdminAgencyRelationDTO)

Create an admin-agency relation [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | admin Id
        CreateAdminAgencyRelationDTO createAdminAgencyRelationDTO = new CreateAdminAgencyRelationDTO(); // CreateAdminAgencyRelationDTO | 
        try {
            apiInstance.createAdminAgencyRelation(adminId, createAdminAgencyRelationDTO);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#createAdminAgencyRelation");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| admin Id | |
| **createAdminAgencyRelationDTO** | [**CreateAdminAgencyRelationDTO**](CreateAdminAgencyRelationDTO.md)|  | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | CREATED - admin-agency relation was created successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## createAgencyAdmin

> AdminResponseDTO createAgencyAdmin(createAdminDTO)

Creates a new Agency Admin [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        CreateAdminDTO createAdminDTO = new CreateAdminDTO(); // CreateAdminDTO | 
        try {
            AdminResponseDTO result = apiInstance.createAgencyAdmin(createAdminDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#createAgencyAdmin");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **createAdminDTO** | [**CreateAdminDTO**](CreateAdminDTO.md)|  | |

### Return type

[**AdminResponseDTO**](AdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | CREATED - Agency Admin was created successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **409** | CONFLICT - invalid/unavailable username or email address |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## createConsultant

> ConsultantAdminResponseDTO createConsultant(createConsultantDTO)

Creates a new consultant [Authorization: Role: consultant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        CreateConsultantDTO createConsultantDTO = new CreateConsultantDTO(); // CreateConsultantDTO | 
        try {
            ConsultantAdminResponseDTO result = apiInstance.createConsultant(createConsultantDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#createConsultant");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **createConsultantDTO** | [**CreateConsultantDTO**](CreateConsultantDTO.md)|  | |

### Return type

[**ConsultantAdminResponseDTO**](ConsultantAdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | CREATED - consultant was created successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **409** | CONFLICT - invalid/unavailable username or email address |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## createConsultantAgency

> createConsultantAgency(consultantId, createConsultantAgencyDTO)

Create a consultant-agency relation [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String consultantId = "consultantId_example"; // String | Consultant Id
        CreateConsultantAgencyDTO createConsultantAgencyDTO = new CreateConsultantAgencyDTO(); // CreateConsultantAgencyDTO | 
        try {
            apiInstance.createConsultantAgency(consultantId, createConsultantAgencyDTO);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#createConsultantAgency");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **consultantId** | **String**| Consultant Id | |
| **createConsultantAgencyDTO** | [**CreateConsultantAgencyDTO**](CreateConsultantAgencyDTO.md)|  | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | CREATED - consultant-agency relation was created successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## createTenantAdmin

> AdminResponseDTO createTenantAdmin(createAdminDTO)

Creates a new Tenant Admin [Authorization: Role: tenant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        CreateAdminDTO createAdminDTO = new CreateAdminDTO(); // CreateAdminDTO | 
        try {
            AdminResponseDTO result = apiInstance.createTenantAdmin(createAdminDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#createTenantAdmin");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **createAdminDTO** | [**CreateAdminDTO**](CreateAdminDTO.md)|  | |

### Return type

[**AdminResponseDTO**](AdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | CREATED - Agency Admin was created successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **409** | CONFLICT - invalid/unavailable username or email address |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## deleteAdminAgencyRelation

> deleteAdminAgencyRelation(adminId, agencyId)

Delete a admin-agency relation [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | Admin Id
        Long agencyId = 56L; // Long | Agency Id
        try {
            apiInstance.deleteAdminAgencyRelation(adminId, agencyId);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#deleteAdminAgencyRelation");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| Admin Id | |
| **agencyId** | **Long**| Agency Id | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - admin-agency relation was deleted successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## deleteAgencyAdmin

> deleteAgencyAdmin(adminId)

delete an agency admin [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | admin id
        try {
            apiInstance.deleteAgencyAdmin(adminId);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#deleteAgencyAdmin");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| admin id | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - agency admin was marked for deletion successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **404** | NOT FOUND - agency admin not found |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## deleteConsultantAgency

> deleteConsultantAgency(consultantId, agencyId)

Delete a consultant-agency relation [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String consultantId = "consultantId_example"; // String | Consultant Id
        Long agencyId = 56L; // Long | Agency Id
        try {
            apiInstance.deleteConsultantAgency(consultantId, agencyId);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#deleteConsultantAgency");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **consultantId** | **String**| Consultant Id | |
| **agencyId** | **Long**| Agency Id | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - consultant-agency relation was deleted successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## deleteTenantAdmin

> deleteTenantAdmin(adminId)

delete tenant admin [Authorization: Role: tenant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | admin id
        try {
            apiInstance.deleteTenantAdmin(adminId);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#deleteTenantAdmin");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| admin id | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - agency admin was marked for deletion successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **404** | NOT FOUND - agency admin not found |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## generateViolationReport

> List&lt;ViolationDTO&gt; generateViolationReport()

Returns an generated report containing data integration violations. [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        try {
            List<ViolationDTO> result = apiInstance.generateViolationReport();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#generateViolationReport");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**List&lt;ViolationDTO&gt;**](ViolationDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successfull operation |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getAdminAgencies

> List&lt;Long&gt; getAdminAgencies(adminId)

Returns the list of agencies for a given admin Id. [Authorization: Role: restricted-agency-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | admin Id
        try {
            List<Long> result = apiInstance.getAdminAgencies(adminId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getAdminAgencies");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| admin Id | |

### Return type

**List&lt;Long&gt;**

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successful operation |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **404** | NOT FOUND - consultant with given id does not exist |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getAgencyAdmin

> AdminResponseDTO getAgencyAdmin(adminId)

Get an agency admin by given id [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | admin id
        try {
            AdminResponseDTO result = apiInstance.getAgencyAdmin(adminId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getAgencyAdmin");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| admin id | |

### Return type

[**AdminResponseDTO**](AdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - consultant found |  -  |
| **204** | NO CONTENT - agency admin with the specific id was not found |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getAgencyAdmins

> AdminSearchResultDTO getAgencyAdmins(page, perPage, filter, sort)

Returns the list of agency admins by filter query parameter. [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        Integer page = 56; // Integer | Number of page where to start in the query (1 = first page)
        Integer perPage = 56; // Integer | Number of items which are being returned per page
        AdminFilter filter = new HashMap(); // AdminFilter | The filter parameters to search for. If no filter is set all admins are being returned.
        Sort sort = new HashMap(); // Sort | The sort parameter containing field and order the response should be sorted
        try {
            AdminSearchResultDTO result = apiInstance.getAgencyAdmins(page, perPage, filter, sort);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getAgencyAdmins");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **page** | **Integer**| Number of page where to start in the query (1 &#x3D; first page) | |
| **perPage** | **Integer**| Number of items which are being returned per page | |
| **filter** | [**AdminFilter**](.md)| The filter parameters to search for. If no filter is set all admins are being returned. | [optional] |
| **sort** | [**Sort**](.md)| The sort parameter containing field and order the response should be sorted | [optional] |

### Return type

[**AdminSearchResultDTO**](AdminSearchResultDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successfull operation |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getAgencyConsultants

> AgencyConsultantResponseDTO getAgencyConsultants(agencyId)

Returns the list of consultants for a given agency Id. [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String agencyId = "agencyId_example"; // String | Agency Id
        try {
            AgencyConsultantResponseDTO result = apiInstance.getAgencyConsultants(agencyId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getAgencyConsultants");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **agencyId** | **String**| Agency Id | |

### Return type

[**AgencyConsultantResponseDTO**](AgencyConsultantResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successfull operation |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **404** | NOT FOUND - agency with given id does not exist |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getAsker

> AskerResponseDTO getAsker(askerId)

Get an asker by given id [Authorization: Role: consultant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String askerId = "askerId_example"; // String | asker id
        try {
            AskerResponseDTO result = apiInstance.getAsker(askerId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getAsker");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **askerId** | **String**| asker id | |

### Return type

[**AskerResponseDTO**](AskerResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - asker found |  -  |
| **204** | NO CONTENT - asker with the specific id was not found |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getConsultant

> ConsultantAdminResponseDTO getConsultant(consultantId)

Get a consultant by given id [Authorization: Role: consultant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String consultantId = "consultantId_example"; // String | consultant id
        try {
            ConsultantAdminResponseDTO result = apiInstance.getConsultant(consultantId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getConsultant");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **consultantId** | **String**| consultant id | |

### Return type

[**ConsultantAdminResponseDTO**](ConsultantAdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - consultant found |  -  |
| **204** | NO CONTENT - consultant with the specific id was not found |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getConsultantAgencies

> ConsultantAgencyResponseDTO getConsultantAgencies(consultantId)

Returns the list of agencies for a given consultant Id. [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String consultantId = "consultantId_example"; // String | Consultant Id
        try {
            ConsultantAgencyResponseDTO result = apiInstance.getConsultantAgencies(consultantId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getConsultantAgencies");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **consultantId** | **String**| Consultant Id | |

### Return type

[**ConsultantAgencyResponseDTO**](ConsultantAgencyResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successfull operation |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **404** | NOT FOUND - consultant with given id does not exist |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getConsultants

> ConsultantSearchResultDTO getConsultants(page, perPage, filter, sort)

Returns the list of consultants by filter query parameter. [Authorization: Role: consultant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        Integer page = 56; // Integer | Number of page where to start in the query (1 = first page)
        Integer perPage = 56; // Integer | Number of items which are being returned per page
        ConsultantFilter filter = new HashMap(); // ConsultantFilter | The filter parameters to search for. If no filter is set all consultant are being returned.
        Sort sort = new HashMap(); // Sort | The sort parameter containing field and order the response should be sorted
        try {
            ConsultantSearchResultDTO result = apiInstance.getConsultants(page, perPage, filter, sort);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getConsultants");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **page** | **Integer**| Number of page where to start in the query (1 &#x3D; first page) | |
| **perPage** | **Integer**| Number of items which are being returned per page | |
| **filter** | [**ConsultantFilter**](.md)| The filter parameters to search for. If no filter is set all consultant are being returned. | [optional] |
| **sort** | [**Sort**](.md)| The sort parameter containing field and order the response should be sorted | [optional] |

### Return type

[**ConsultantSearchResultDTO**](ConsultantSearchResultDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successfull operation |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getSessions

> SessionAdminResultDTO getSessions(page, perPage, filter)

Returns the list of sessions by filter query parameter. [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        Integer page = 56; // Integer | Number of page where to start in the query (1 = first page)
        Integer perPage = 56; // Integer | Number of items which are being returned per page
        SessionFilter filter = new HashMap(); // SessionFilter | The filter parameters to search for. If no filter is set all sessions are being returned. If more than one filter is set the first given filter is used only.
        try {
            SessionAdminResultDTO result = apiInstance.getSessions(page, perPage, filter);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getSessions");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **page** | **Integer**| Number of page where to start in the query (1 &#x3D; first page) | |
| **perPage** | **Integer**| Number of items which are being returned per page | |
| **filter** | [**SessionFilter**](.md)| The filter parameters to search for. If no filter is set all sessions are being returned. If more than one filter is set the first given filter is used only. | [optional] |

### Return type

[**SessionAdminResultDTO**](SessionAdminResultDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successfull operation |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getTenantAdmin

> AdminResponseDTO getTenantAdmin(adminId)

Get a tenant admin by given id [Authorization: Role: tenant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | admin id
        try {
            AdminResponseDTO result = apiInstance.getTenantAdmin(adminId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getTenantAdmin");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| admin id | |

### Return type

[**AdminResponseDTO**](AdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - consultant found |  -  |
| **204** | NO CONTENT - agency admin with the specific id was not found |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getTenantAdmins

> List&lt;AdminResponseDTO&gt; getTenantAdmins(tenantId)

Returns the list of tenant admins by filter query parameter. [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        Integer tenantId = 56; // Integer | tenant id
        try {
            List<AdminResponseDTO> result = apiInstance.getTenantAdmins(tenantId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#getTenantAdmins");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **tenantId** | **Integer**| tenant id | |

### Return type

[**List&lt;AdminResponseDTO&gt;**](AdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successful operation |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## markAskerForDeletion

> markAskerForDeletion(askerId)

Delete a asker by given id [Authorization: Role: consultant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String askerId = "askerId_example"; // String | asker id
        try {
            apiInstance.markAskerForDeletion(askerId);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#markAskerForDeletion");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **askerId** | **String**| asker id | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - asker was marked for deletion successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **404** | NOT FOUND - asker not found |  -  |
| **409** | CONFLICT - asker is already marked for deletion |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## markConsultantForDeletion

> markConsultantForDeletion(consultantId, forceDeleteSessions)

Mark a consultant for deletion [Authorization: Role: consultant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String consultantId = "consultantId_example"; // String | consultant id
        Boolean forceDeleteSessions = true; // Boolean | parameter specifying if consultant sessions need to be removed prior the deletion
        try {
            apiInstance.markConsultantForDeletion(consultantId, forceDeleteSessions);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#markConsultantForDeletion");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **consultantId** | **String**| consultant id | |
| **forceDeleteSessions** | **Boolean**| parameter specifying if consultant sessions need to be removed prior the deletion | [optional] |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - consultant was marked for deletion successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **404** | NOT FOUND - consultant not found |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## patchAdminData

> AdminResponseDTO patchAdminData(patchAdminDTO)

Sets or updates the email, first and lastname of authorized admin account [Authorization:  Role: single-tenant-admin, restricted-agency-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        PatchAdminDTO patchAdminDTO = new PatchAdminDTO(); // PatchAdminDTO | 
        try {
            AdminResponseDTO result = apiInstance.patchAdminData(patchAdminDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#patchAdminData");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **patchAdminDTO** | [**PatchAdminDTO**](PatchAdminDTO.md)|  | |

### Return type

[**AdminResponseDTO**](AdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - admin was updated successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid Keycloak token |  -  |
| **403** | FORBIDDEN - no/invalid role/authorization or CSRF token |  -  |
| **409** | CONFLICT - invalid/unavailable email |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## searchAgencyAdmins

> AdminSearchResultDTO searchAgencyAdmins(query, page, perPage, field, order)

Get Agency admins matching the given query [Auth: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String query = "query_example"; // String | URL-encoded infix to search for in id first name, last name, or email. A non-encoded star symbol searches for all.
        Integer page = 1; // Integer | Page number (first page = 1)
        Integer perPage = 10; // Integer | Number of items returned per page
        String field = "FIRSTNAME"; // String | field to sort by
        String order = "ASC"; // String | sort order
        try {
            AdminSearchResultDTO result = apiInstance.searchAgencyAdmins(query, page, perPage, field, order);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#searchAgencyAdmins");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **query** | **String**| URL-encoded infix to search for in id first name, last name, or email. A non-encoded star symbol searches for all. | |
| **page** | **Integer**| Page number (first page &#x3D; 1) | [optional] [default to 1] |
| **perPage** | **Integer**| Number of items returned per page | [optional] [default to 10] |
| **field** | **String**| field to sort by | [optional] [default to FIRSTNAME] [enum: FIRSTNAME, LASTNAME, EMAIL] |
| **order** | **String**| sort order | [optional] [default to ASC] [enum: ASC, DESC] |

### Return type

[**AdminSearchResultDTO**](AdminSearchResultDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successful operation |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## searchTenantAdmins

> AdminSearchResultDTO searchTenantAdmins(query, page, perPage, field, order)

Get tenant admins matching the given query [Auth: tenant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String query = "query_example"; // String | URL-encoded infix to search for in first name, last name, email or tenant id. A non-encoded star symbol searches for all.
        Integer page = 1; // Integer | Page number (first page = 1)
        Integer perPage = 10; // Integer | Number of items returned per page
        String field = "FIRSTNAME"; // String | field to sort by
        String order = "ASC"; // String | sort order
        try {
            AdminSearchResultDTO result = apiInstance.searchTenantAdmins(query, page, perPage, field, order);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#searchTenantAdmins");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **query** | **String**| URL-encoded infix to search for in first name, last name, email or tenant id. A non-encoded star symbol searches for all. | |
| **page** | **Integer**| Page number (first page &#x3D; 1) | [optional] [default to 1] |
| **perPage** | **Integer**| Number of items returned per page | [optional] [default to 10] |
| **field** | **String**| field to sort by | [optional] [default to FIRSTNAME] [enum: FIRSTNAME, LASTNAME, EMAIL, TENANT_ID] |
| **order** | **String**| sort order | [optional] [default to ASC] [enum: ASC, DESC] |

### Return type

[**AdminSearchResultDTO**](AdminSearchResultDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successful operation |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## setAdminAgenciesRelation

> setAdminAgenciesRelation(adminId, createAdminAgencyRelationDTO)

Set admin-agency relations [Authorization: Role: user-admin]

Existing relations are deleted, passed relations added.

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | 
        List<CreateAdminAgencyRelationDTO> createAdminAgencyRelationDTO = Arrays.asList(); // List<CreateAdminAgencyRelationDTO> | 
        try {
            apiInstance.setAdminAgenciesRelation(adminId, createAdminAgencyRelationDTO);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#setAdminAgenciesRelation");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**|  | |
| **createAdminAgencyRelationDTO** | [**List&lt;CreateAdminAgencyRelationDTO&gt;**](CreateAdminAgencyRelationDTO.md)|  | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - admin-agency relations have been set |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## setConsultantAgencies

> setConsultantAgencies(consultantId, createConsultantAgencyDTO)

Set consultant-agency relations [Authorization: Role: user-admin]

Existing relations are deleted, passed relations added.

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String consultantId = "consultantId_example"; // String | 
        List<CreateConsultantAgencyDTO> createConsultantAgencyDTO = Arrays.asList(); // List<CreateConsultantAgencyDTO> | 
        try {
            apiInstance.setConsultantAgencies(consultantId, createConsultantAgencyDTO);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#setConsultantAgencies");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **consultantId** | **String**|  | |
| **createConsultantAgencyDTO** | [**List&lt;CreateConsultantAgencyDTO&gt;**](CreateConsultantAgencyDTO.md)|  | |

### Return type

null (empty response body)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - consultant-agency relations have been set |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## updateAgencyAdmin

> AdminResponseDTO updateAgencyAdmin(adminId, updateAgencyAdminDTO)

Updates an agency admin [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | agency admin id
        UpdateAgencyAdminDTO updateAgencyAdminDTO = new UpdateAgencyAdminDTO(); // UpdateAgencyAdminDTO | 
        try {
            AdminResponseDTO result = apiInstance.updateAgencyAdmin(adminId, updateAgencyAdminDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#updateAgencyAdmin");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| agency admin id | |
| **updateAgencyAdminDTO** | [**UpdateAgencyAdminDTO**](UpdateAgencyAdminDTO.md)|  | |

### Return type

[**AdminResponseDTO**](AdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - agency admin was updated successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## updateConsultant

> ConsultantAdminResponseDTO updateConsultant(consultantId, updateAdminConsultantDTO)

Updates a consultant [Authorization: Role: consultant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String consultantId = "consultantId_example"; // String | consultant id
        UpdateAdminConsultantDTO updateAdminConsultantDTO = new UpdateAdminConsultantDTO(); // UpdateAdminConsultantDTO | 
        try {
            ConsultantAdminResponseDTO result = apiInstance.updateConsultant(consultantId, updateAdminConsultantDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#updateConsultant");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **consultantId** | **String**| consultant id | |
| **updateAdminConsultantDTO** | [**UpdateAdminConsultantDTO**](UpdateAdminConsultantDTO.md)|  | |

### Return type

[**ConsultantAdminResponseDTO**](ConsultantAdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - consultant was updated successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## updateTenantAdmin

> AdminResponseDTO updateTenantAdmin(adminId, updateTenantAdminDTO)

Updates a tenant admin [Authorization: Role: tenant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.AdminUserControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        AdminUserControllerApi apiInstance = new AdminUserControllerApi(defaultClient);
        String adminId = "adminId_example"; // String | agency admin id
        UpdateTenantAdminDTO updateTenantAdminDTO = new UpdateTenantAdminDTO(); // UpdateTenantAdminDTO | 
        try {
            AdminResponseDTO result = apiInstance.updateTenantAdmin(adminId, updateTenantAdminDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdminUserControllerApi#updateTenantAdmin");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **adminId** | **String**| agency admin id | |
| **updateTenantAdminDTO** | [**UpdateTenantAdminDTO**](UpdateTenantAdminDTO.md)|  | |

### Return type

[**AdminResponseDTO**](AdminResponseDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - agency admin was updated successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request |  -  |
| **401** | UNAUTHORIZED - no/invalid role/authorization |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |

