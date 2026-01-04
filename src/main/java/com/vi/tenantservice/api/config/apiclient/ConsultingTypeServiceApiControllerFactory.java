package com.vi.tenantservice.api.config.apiclient;

import com.vi.tenantservice.consultingtypeservice.generated.ApiClient;
import com.vi.tenantservice.consultingtypeservice.generated.web.ConsultingTypeControllerApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ConsultingTypeServiceApiControllerFactory {

  @Value("${consulting.type.service.api.url}")
  private String consultingTypeServiceApiUrl;

  @Autowired private RestTemplate restTemplate;

  public ConsultingTypeControllerApi createControllerApi() {
    var apiClient = new ApiClient(restTemplate).setBasePath(this.consultingTypeServiceApiUrl);
    return new ConsultingTypeControllerApi(apiClient);
  }
}
