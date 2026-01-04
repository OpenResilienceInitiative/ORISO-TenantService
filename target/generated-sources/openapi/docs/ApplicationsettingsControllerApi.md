# ApplicationsettingsControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getApplicationSettings**](ApplicationsettingsControllerApi.md#getApplicationSettings) | **GET** /settings | Get all application settings |
| [**patchApplicationSettings**](ApplicationsettingsControllerApi.md#patchApplicationSettings) | **PATCH** /settingsadmin | Patch application settings |



## getApplicationSettings

> ApplicationSettingsDTO getApplicationSettings()

Get all application settings

### Example

```java
// Import classes:
import com.vi.tenantservice.applicationsettingsservice.generated.ApiClient;
import com.vi.tenantservice.applicationsettingsservice.generated.ApiException;
import com.vi.tenantservice.applicationsettingsservice.generated.Configuration;
import com.vi.tenantservice.applicationsettingsservice.generated.models.*;
import com.vi.tenantservice.applicationsettingsservice.generated.web.ApplicationsettingsControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ApplicationsettingsControllerApi apiInstance = new ApplicationsettingsControllerApi(defaultClient);
        try {
            ApplicationSettingsDTO result = apiInstance.getApplicationSettings();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ApplicationsettingsControllerApi#getApplicationSettings");
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

[**ApplicationSettingsDTO**](ApplicationSettingsDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successful operation |  -  |
| **204** | NO CONTENT - no content found |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid Keycloak token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |


## patchApplicationSettings

> ApplicationSettingsDTO patchApplicationSettings(applicationSettingsPatchDTO)

Patch application settings

### Example

```java
// Import classes:
import com.vi.tenantservice.applicationsettingsservice.generated.ApiClient;
import com.vi.tenantservice.applicationsettingsservice.generated.ApiException;
import com.vi.tenantservice.applicationsettingsservice.generated.Configuration;
import com.vi.tenantservice.applicationsettingsservice.generated.models.*;
import com.vi.tenantservice.applicationsettingsservice.generated.web.ApplicationsettingsControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        ApplicationsettingsControllerApi apiInstance = new ApplicationsettingsControllerApi(defaultClient);
        ApplicationSettingsPatchDTO applicationSettingsPatchDTO = new ApplicationSettingsPatchDTO(); // ApplicationSettingsPatchDTO | 
        try {
            ApplicationSettingsDTO result = apiInstance.patchApplicationSettings(applicationSettingsPatchDTO);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ApplicationsettingsControllerApi#patchApplicationSettings");
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
| **applicationSettingsPatchDTO** | [**ApplicationSettingsPatchDTO**](ApplicationSettingsPatchDTO.md)|  | [optional] |

### Return type

[**ApplicationSettingsDTO**](ApplicationSettingsDTO.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successful operation |  -  |
| **204** | NO CONTENT - no content found |  -  |
| **400** | BAD REQUEST - invalid/incomplete request or body object |  -  |
| **401** | UNAUTHORIZED - no/invalid Keycloak token |  -  |
| **500** | INTERNAL SERVER ERROR - server encountered unexpected condition |  -  |

