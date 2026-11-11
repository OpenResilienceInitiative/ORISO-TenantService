package com.vi.tenantservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Team rule (ORISO-Helm#368): a deployed service never invents a URL. A missing origin must fail,
 * and the production host must not appear at all. Only the local and testing profiles may default,
 * and even they never name the production host.
 */
class NoHardcodedUrlFallbacksTest {

  private static final Path RESOURCES = Path.of("src/main/resources");
  private static final Path MAIN_JAVA = Path.of("src/main/java");
  private static final Pattern URL_DEFAULT = Pattern.compile("\\$\\{[^}:]+:\\s*https?://");
  private static final Pattern ORISO_HOST = Pattern.compile("oriso\\.org");

  @Test
  void deployedProfiles_declareNoUrlDefaults_andNeverNameTheProductionHost() throws IOException {
    assertThat(violations(propertyFiles(false), URL_DEFAULT, ORISO_HOST)).isEmpty();
  }

  @Test
  void localAndTestingProfiles_neverNameTheProductionHost() throws IOException {
    assertThat(violations(propertyFiles(true), ORISO_HOST)).isEmpty();
  }

  @Test
  void javaSources_declareNoUrlDefaultsInPropertyPlaceholders() throws IOException {
    List<Path> sources;
    try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
      sources = files.filter(p -> p.toString().endsWith(".java")).toList();
    }
    assertThat(violations(sources, URL_DEFAULT)).isEmpty();
  }

  @Test
  void publicDpaOrigin_hasNoDeployedUrlDefault() throws IOException {
    assertThat(
            publicDpaOriginAssignments(
                Files.readAllLines(RESOURCES.resolve("application.properties"))))
        .containsExactly("app.base.url=${APP_BASE_URL:}");
    for (Path profile : propertyFiles(false)) {
      if (!profile.getFileName().toString().equals("application.properties")) {
        assertThat(publicDpaOriginAssignments(Files.readAllLines(profile))).isEmpty();
      }
    }
  }

  @Test
  void publicDpaOriginGuard_detectsSpacedDuplicateAssignment() {
    assertThat(
            publicDpaOriginAssignments(
                List.of("app.base.url=${APP_BASE_URL:}", "app.base.url = https://app.example.org")))
        .hasSize(2);
  }

  private static List<String> publicDpaOriginAssignments(List<String> lines) {
    return lines.stream().filter(line -> line.matches("\\s*app\\.base\\.url\\s*[=:].*")).toList();
  }

  private static List<Path> propertyFiles(boolean localOrTesting) throws IOException {
    try (Stream<Path> files = Files.list(RESOURCES)) {
      return files
          .filter(p -> p.getFileName().toString().matches("application(-[a-z]+)?\\.properties"))
          .filter(
              p ->
                  localOrTesting
                      == p.getFileName().toString().matches("application-(local|testing)\\..*"))
          .toList();
    }
  }

  private static List<String> violations(List<Path> files, Pattern... patterns) {
    return files.stream().flatMap(file -> violationsIn(file, patterns)).toList();
  }

  private static Stream<String> violationsIn(Path file, Pattern... patterns) {
    try {
      List<String> lines = Files.readAllLines(file);
      return IntStream.range(0, lines.size())
          .filter(i -> Stream.of(patterns).anyMatch(p -> p.matcher(lines.get(i)).find()))
          .mapToObj(i -> file + ":" + (i + 1) + " " + lines.get(i));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
