package com.vi.tenantservice.api.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.service.TenantHeldBootstrap;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Only a fixed current registered SETUP call can start or reconcile its held operation. */
@RestController
public class TenantHeldBootstrapController {
  private final TenantHeldBootstrap bootstrap;
  private final ObjectMapper json=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
      .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
  public TenantHeldBootstrapController(TenantHeldBootstrap bootstrap) {this.bootstrap=bootstrap;}

  @PostMapping(value="/tenantadmin/bootstrap",consumes="application/json",produces="application/json")
  @PreAuthorize("@taskServiceIdentity.allows(authentication, 'CONFIG_WIZARD')")
  public ResponseEntity<TenantHeldBootstrap.Result> createReservedTenant(@RequestBody byte[] command) {
    if(command==null || command.length<2 || command.length>1_048_576) throw new IllegalArgumentException();
    try {
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(bootstrap.create(json.readTree(command)));
    } catch(java.io.IOException malformed) {throw new IllegalArgumentException("Invalid held tenant request");}
  }

  @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class})
  public ResponseEntity<Void> invalid() {return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();}
}
