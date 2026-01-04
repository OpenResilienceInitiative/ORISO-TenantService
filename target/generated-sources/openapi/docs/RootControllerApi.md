# RootControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getRoot**](RootControllerApi.md#getRoot) | **GET** /useradmin | Returns the hal root entry point. [Authorization: Role: user-admin] |



## getRoot

> RootDTO getRoot()

Returns the hal root entry point. [Authorization: Role: user-admin]

### Example

```java
// Import classes:
import com.vi.tenantservice.useradminservice.generated.ApiClient;
import com.vi.tenantservice.useradminservice.generated.ApiException;
import com.vi.tenantservice.useradminservice.generated.Configuration;
import com.vi.tenantservice.useradminservice.generated.auth.*;
import com.vi.tenantservice.useradminservice.generated.models.*;
import com.vi.tenantservice.useradminservice.generated.web.RootControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");
        
        // Configure API key authorization: Bearer
        ApiKeyAuth Bearer = (ApiKeyAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //Bearer.setApiKeyPrefix("Token");

        RootControllerApi apiInstance = new RootControllerApi(defaultClient);
        try {
            RootDTO result = apiInstance.getRoot();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling RootControllerApi#getRoot");
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

[**RootDTO**](RootDTO.md)

### Authorization

[Bearer](../README.md#Bearer)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/hal+json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK - successfull operation |  -  |

