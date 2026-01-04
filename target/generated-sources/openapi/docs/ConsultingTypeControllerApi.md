# ConsultingTypeControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createConsultingType**](ConsultingTypeControllerApi.md#createConsultingType) | **POST** /consultingtypes | Create a consulting type [Authorization: Role: tenant-admin] |
| [**getBasicConsultingTypeById**](ConsultingTypeControllerApi.md#getBasicConsultingTypeById) | **GET** /consultingtypes/{consultingTypeId}/basic | Returns the consulting type with basic set of properties for a given consulting type id |
| [**getBasicConsultingTypeList**](ConsultingTypeControllerApi.md#getBasicConsultingTypeList) | **GET** /consultingtypes/basic | Returns a list of all consulting types with basic properties  |
| [**getConsultingTypeGroups**](ConsultingTypeControllerApi.md#getConsultingTypeGroups) | **GET** /consultingtypes/groups | Returns the group structure of all consulting types |
| [**getExtendedConsultingTypeById**](ConsultingTypeControllerApi.md#getExtendedConsultingTypeById) | **GET** /consultingtypes/{consultingTypeId}/extended | Returns the consulting type with extended set of properties for a given consulting type id |
| [**getFullConsultingTypeById**](ConsultingTypeControllerApi.md#getFullConsultingTypeById) | **GET** /consultingtypes/{consultingTypeId}/full | Returns the consulting type with all properties for a given consulting type id |
| [**getFullConsultingTypeBySlug**](ConsultingTypeControllerApi.md#getFullConsultingTypeBySlug) | **GET** /consultingtypes/byslug/{slug}/full | Returns the consulting type with all properties for a given consulting type slug |
| [**getFullConsultingTypeByTenantId**](ConsultingTypeControllerApi.md#getFullConsultingTypeByTenantId) | **GET** /consultingtypes/bytenant/{tenantId}/full | Returns the consulting type with all properties for a given consulting type tenantId |
| [**patchConsultingType**](ConsultingTypeControllerApi.md#patchConsultingType) | **PATCH** /consultingtypes/{id} | Updates a consulting type [Authorization: Role: tenant-admin] |



## createConsultingType

> FullConsultingTypeResponseDTO createConsultingType(consultingTypeDTO)

Create a consulting type [Authorization: Role: tenant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        ConsultingTypeDTO consultingTypeDTO = new ConsultingTypeDTO(); // ConsultingTypeDTO | 
        try {
            FullConsultingTypeResponseDTO result = apiInstance.createConsultingType(consultingTypeDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#createConsultingType");
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
| **consultingTypeDTO** | [**ConsultingTypeDTO**](ConsultingTypeDTO.md)|  | |

### Return type

[**FullConsultingTypeResponseDTO**](FullConsultingTypeResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | CREATED - consulting type was created successfully |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **403** | FORBIDDEN - no/invalid CSRF token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getBasicConsultingTypeById

> BasicConsultingTypeResponseDTO getBasicConsultingTypeById(consultingTypeId)

Returns the consulting type with basic set of properties for a given consulting type id

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        Integer consultingTypeId = 56; // Integer | cnsulting type id
        try {
            BasicConsultingTypeResponseDTO result = apiInstance.getBasicConsultingTypeById(consultingTypeId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#getBasicConsultingTypeById");
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
| **consultingTypeId** | **Integer**| cnsulting type id | |

### Return type

[**BasicConsultingTypeResponseDTO**](BasicConsultingTypeResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | successful operation |  -  |
| **404** | NOT FOUND - ConsultingType not found |  -  |
| **403** | FORBIDDEN - no/invalid role/authorization or CSRF token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getBasicConsultingTypeList

> List&lt;BasicConsultingTypeResponseDTO&gt; getBasicConsultingTypeList()

Returns a list of all consulting types with basic properties 

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        try {
            List<BasicConsultingTypeResponseDTO> result = apiInstance.getBasicConsultingTypeList();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#getBasicConsultingTypeList");
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

[**List&lt;BasicConsultingTypeResponseDTO&gt;**](BasicConsultingTypeResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | successful operation |  -  |
| **204** | successful operation, but no content |  -  |
| **403** | FORBIDDEN - no/invalid role/authorization or CSRF token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getConsultingTypeGroups

> List&lt;ConsultingTypeGroupResponseDTO&gt; getConsultingTypeGroups()

Returns the group structure of all consulting types

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        try {
            List<ConsultingTypeGroupResponseDTO> result = apiInstance.getConsultingTypeGroups();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#getConsultingTypeGroups");
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

[**List&lt;ConsultingTypeGroupResponseDTO&gt;**](ConsultingTypeGroupResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | successful operation |  -  |
| **204** | NO CONTENT - no groups defined |  -  |
| **403** | FORBIDDEN - no/invalid role/authorization or CSRF token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getExtendedConsultingTypeById

> ExtendedConsultingTypeResponseDTO getExtendedConsultingTypeById(consultingTypeId)

Returns the consulting type with extended set of properties for a given consulting type id

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        Integer consultingTypeId = 56; // Integer | cnsulting type id
        try {
            ExtendedConsultingTypeResponseDTO result = apiInstance.getExtendedConsultingTypeById(consultingTypeId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#getExtendedConsultingTypeById");
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
| **consultingTypeId** | **Integer**| cnsulting type id | |

### Return type

[**ExtendedConsultingTypeResponseDTO**](ExtendedConsultingTypeResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | successful operation |  -  |
| **404** | NOT FOUND - ConsultingType not found |  -  |
| **403** | FORBIDDEN - no/invalid role/authorization or CSRF token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getFullConsultingTypeById

> FullConsultingTypeResponseDTO getFullConsultingTypeById(consultingTypeId)

Returns the consulting type with all properties for a given consulting type id

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        Integer consultingTypeId = 56; // Integer | cnsulting type id
        try {
            FullConsultingTypeResponseDTO result = apiInstance.getFullConsultingTypeById(consultingTypeId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#getFullConsultingTypeById");
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
| **consultingTypeId** | **Integer**| cnsulting type id | |

### Return type

[**FullConsultingTypeResponseDTO**](FullConsultingTypeResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | successful operation |  -  |
| **404** | NOT FOUND - ConsultingType not found |  -  |
| **403** | FORBIDDEN - no/invalid role/authorization or CSRF token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getFullConsultingTypeBySlug

> FullConsultingTypeResponseDTO getFullConsultingTypeBySlug(slug)

Returns the consulting type with all properties for a given consulting type slug

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        String slug = "slug_example"; // String | cnsulting type slug
        try {
            FullConsultingTypeResponseDTO result = apiInstance.getFullConsultingTypeBySlug(slug);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#getFullConsultingTypeBySlug");
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
| **slug** | **String**| cnsulting type slug | |

### Return type

[**FullConsultingTypeResponseDTO**](FullConsultingTypeResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | successful operation |  -  |
| **404** | NOT FOUND - ConsultingType not found |  -  |
| **403** | FORBIDDEN - no/invalid role/authorization or CSRF token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## getFullConsultingTypeByTenantId

> FullConsultingTypeResponseDTO getFullConsultingTypeByTenantId(tenantId)

Returns the consulting type with all properties for a given consulting type tenantId

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        Integer tenantId = 56; // Integer | consulting type tenantId
        try {
            FullConsultingTypeResponseDTO result = apiInstance.getFullConsultingTypeByTenantId(tenantId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#getFullConsultingTypeByTenantId");
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
| **tenantId** | **Integer**| consulting type tenantId | |

### Return type

[**FullConsultingTypeResponseDTO**](FullConsultingTypeResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | successful operation |  -  |
| **404** | NOT FOUND - ConsultingType not found |  -  |
| **403** | FORBIDDEN - no/invalid role/authorization or CSRF token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## patchConsultingType

> FullConsultingTypeResponseDTO patchConsultingType(id, consultingTypePatchDTO)

Updates a consulting type [Authorization: Role: tenant-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.ApiException;
import com.vi.tenantservice.consultingtypeservice.generated.Configuration;
import com.vi.tenantservice.consultingtypeservice.generated.models.*;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ConsultingTypeControllerApi apiInstance = new ConsultingTypeControllerApi(defaultClient);
        Integer id = 56; // Integer | ConsultingType ID
        ConsultingTypePatchDTO consultingTypePatchDTO = new ConsultingTypePatchDTO(); // ConsultingTypePatchDTO | 
        try {
            FullConsultingTypeResponseDTO result = apiInstance.patchConsultingType(id, consultingTypePatchDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsultingTypeControllerApi#patchConsultingType");
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
| **id** | **Integer**| ConsultingType ID | |
| **consultingTypePatchDTO** | [**ConsultingTypePatchDTO**](ConsultingTypePatchDTO.md)|  | [optional] |

### Return type

[**FullConsultingTypeResponseDTO**](FullConsultingTypeResponseDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful operation |  -  |
| **401** | UNAUTHORIZED - no/invalid Keycloak token |  -  |
| **409** | CONFLICT - unique constraint validation fails |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |

