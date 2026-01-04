package com.vi.tenantservice.api.service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import javax.ws.rs.InternalServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConfigurationFileLoader {

  public File loadFrom(final String filePath) {
    try {
      var fileUrl = Paths.get(filePath).toUri().toURL();
      return new File(fileUrl.toURI());
    } catch (URISyntaxException | MalformedURLException | InvalidPathException exception) {
      log.error("Could not load configuration file {}", filePath, exception);
      throw new InternalServerErrorException();
    }
  }
}
