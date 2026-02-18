package com.vi.tenantservice.api.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.SpringVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/version")
public class VersionController {

  private static final String DEFAULT_JAVA_TARGET_VERSION = "21";
  private static final String DEFAULT_SPRING_BOOT_TARGET_VERSION = "4.0.1";
  private static final String DEFAULT_SPRING_FRAMEWORK_TARGET_VERSION = "6.2.0";

  private final String javaTargetVersion;
  private final String springBootTargetVersion;
  private final String springFrameworkTargetVersion;

  public VersionController() {
    Properties props = loadVersionProperties();
    this.javaTargetVersion = props.getProperty("java.upper.version", DEFAULT_JAVA_TARGET_VERSION);
    this.springBootTargetVersion =
        props.getProperty("springboot.upper.version", DEFAULT_SPRING_BOOT_TARGET_VERSION);
    this.springFrameworkTargetVersion =
        props.getProperty("spring.upper.version", DEFAULT_SPRING_FRAMEWORK_TARGET_VERSION);
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
    versionInfo.put("java.version", System.getProperty("java.version"));
    versionInfo.put("java.runtime.version", System.getProperty("java.runtime.version"));
    versionInfo.put("spring.boot.version", SpringBootVersion.getVersion());
    versionInfo.put("spring.framework.version", SpringVersion.getVersion());
    versionInfo.put("java.targetVersion", javaTargetVersion);
    versionInfo.put("spring.boot.targetVersion", springBootTargetVersion);
    versionInfo.put("spring.framework.targetVersion", springFrameworkTargetVersion);
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
            System.getProperty("java.version"),
            "runtimeVersion",
            System.getProperty("java.runtime.version"),
            "vendor",
            System.getProperty("java.vendor"),
            "targetVersion",
            javaTargetVersion));
    info.put(
        "spring",
        Map.of(
            "boot",
            SpringBootVersion.getVersion(),
            "framework",
            SpringVersion.getVersion(),
            "bootTargetVersion",
            springBootTargetVersion,
            "frameworkTargetVersion",
            springFrameworkTargetVersion));
    info.put("application", Map.of(
        "name", "TenantService",
        "version", "0.0.1-SNAPSHOT"
    ));
    return ResponseEntity.ok(info);
  }
}


