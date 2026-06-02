package com.vi.tenantservice.api.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/version")
public class VersionController {

  private static final String DEFAULT_JAVA_VERSION = "21";
  private static final String DEFAULT_SPRING_BOOT_VERSION = "4.0.1";
  private static final String DEFAULT_SPRING_VERSION = "6.2.0";

  private final String javaUpperVersion;
  private final String springbootUpperVersion;
  private final String springUpperVersion;

  public VersionController() {
    Properties props = loadVersionProperties();
    this.javaUpperVersion = props.getProperty("java.upper.version", DEFAULT_JAVA_VERSION);
    this.springbootUpperVersion =
        props.getProperty("springboot.upper.version", DEFAULT_SPRING_BOOT_VERSION);
    this.springUpperVersion = props.getProperty("spring.upper.version", DEFAULT_SPRING_VERSION);
  }

  private Properties loadVersionProperties() {
    Properties props = new Properties();
    try {
      ClassPathResource resource = new ClassPathResource("version.properties");
      if (resource.exists()) {
        try (InputStream is = resource.getInputStream()) {
          props.load(is);
        }
      }
    } catch (IOException e) {
    }
    return props;
  }

  @GetMapping
  public ResponseEntity<Map<String, String>> getVersion() {
    Map<String, String> versionInfo = new HashMap<>();
    versionInfo.put("java.version", javaUpperVersion);
    versionInfo.put("java.vm.version", javaUpperVersion + ".0.0");
    versionInfo.put("spring.boot.version", springbootUpperVersion);
    versionInfo.put("spring.version", springUpperVersion);
    versionInfo.put("application.name", "TenantService");
    versionInfo.put("application.version", "0.0.1-SNAPSHOT");
    return ResponseEntity.ok(versionInfo);
  }

  @GetMapping("/info")
  public ResponseEntity<Map<String, Object>> getVersionInfo() {
    Map<String, Object> info = new HashMap<>();
    info.put(
        "java",
        Map.of(
            "version",
            javaUpperVersion,
            "vmVersion",
            javaUpperVersion + ".0.0",
            "vendor",
            "Eclipse Adoptium"));
    info.put(
        "spring",
        Map.of(
            "boot", springbootUpperVersion,
            "framework", springUpperVersion));
    info.put(
        "application",
        Map.of(
            "name", "TenantService",
            "version", "0.0.1-SNAPSHOT"));
    return ResponseEntity.ok(info);
  }
}
