package com.vi.tenantservice.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.tenantservice.api.model.DataProtectionContactTemplateDTO;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service for mail templates */
@Service
@Slf4j
public class TemplateService {

  private static final String TEMPLATE_DIR = "/templates/";
  private static final String TEMPLATE_EXTENSION = ".json";

  @Value("${template.use.custom.resources.path}")
  private boolean useCustomResourcesPath;

  @Value("${template.custom.resources.path}")
  private String customResourcePath;

  public DataProtectionContactTemplateDTO getDefaultDataProtectionTemplate()
      throws TemplateDescriptionServiceException {

    return loadTemplateDescription("dataProtectionContactTemplate");
  }

  public Map<String, DataProtectionContactTemplateDTO> getMultilingualDataProtectionTemplate()
      throws TemplateDescriptionServiceException {
    return Map.of(
        "de", loadTemplateDescription("dataProtectionContactTemplate"),
        "en", loadTemplateDescription("dataProtectionContactTemplate.en"));
  }

  /**
   * Load template description
   *
   * @param templateName the template name
   * @return the template description
   */
  private DataProtectionContactTemplateDTO loadTemplateDescription(String templateName)
      throws TemplateDescriptionServiceException {
    var mapper = new ObjectMapper();
    String templateDescriptionJson = loadTemplateDescriptionFile(templateName);
    try {
      return mapper.readValue(templateDescriptionJson, DataProtectionContactTemplateDTO.class);
    } catch (Exception ex) {
      throw new TemplateDescriptionServiceException(
          String.format(
              "Json file with template description could not be parsed, template name: %s",
              templateName),
          ex);
    }
  }

  /**
   * Load template file from resources. InputStream is needed as file is located in jar.
   *
   * @param templateName the name of the template
   * @return the content of the template description file
   */
  private String loadTemplateDescriptionFile(String templateName)
      throws TemplateDescriptionServiceException {
    try {
      var inputStream =
          useCustomResourcesPath
              ? buildStreamForExternalPath(templateName)
              : TemplateService.class.getResourceAsStream(getTemplateFilename(templateName));
      log.info("useCustomResourcesPath: {}", useCustomResourcesPath);
      if (inputStream == null) {
        log.error("Template file could not be loaded, template name: {}", templateName);
        return "";
      }
      assert inputStream != null;
      final List<String> fileLines =
          IOUtils.readLines(inputStream, StandardCharsets.UTF_8.displayName());
      return String.join("", fileLines);
    } catch (Exception ex) {
      throw new TemplateDescriptionServiceException(
          String.format(
              "Json file with template description could not be loaded, template name: %s",
              templateName),
          ex);
    }
  }

  private FileInputStream buildStreamForExternalPath(String templateName)
      throws FileNotFoundException {
    return new FileInputStream(customResourcePath + templateName + TEMPLATE_EXTENSION);
  }

  /**
   * Get the filename and filepath for the template description file
   *
   * @param templateName the template name
   * @return the filename with filepath of the template description file
   */
  private String getTemplateFilename(String templateName) {
    return TEMPLATE_DIR + templateName + TEMPLATE_EXTENSION;
  }
}
