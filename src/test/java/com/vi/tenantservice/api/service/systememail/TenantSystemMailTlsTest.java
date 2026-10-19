package com.vi.tenantservice.api.service.systememail;

import static org.assertj.core.api.Assertions.*;

import com.vi.tenantservice.api.model.TenantSmtpSettings;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.*;
import javax.net.ssl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TenantSystemMailTlsTest {
  @TempDir Path temporary;
  final TenantSystemMailTransport transport = new TenantSystemMailTransport();

  SystemEmailDeliveryRequest request() {
    return new SystemEmailDeliveryRequest(
        SystemEmailDeliveryRequest.Purpose.EMAIL_ADDRESS_CHANGED,
        "recipient@example.org",
        "Subject",
        "<p>Body</p>",
        "Body",
        UUID.randomUUID());
  }

  TenantSmtpSettings settings(int port, boolean secure) {
    return TenantSmtpSettings.builder()
        .host("localhost")
        .port(port)
        .secure(secure)
        .username("test-user")
        .from("sender@example.org")
        .build();
  }

  @Test
  void serverWithoutStarttlsIsRejectedBeforeAnyAuthentication() throws Exception {
    var commands = new java.util.concurrent.CopyOnWriteArrayList<String>();
    try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      server.setSoTimeout(5000);
      Thread peer =
          new Thread(
              () -> {
                try (var socket = server.accept()) {
                  socket.setSoTimeout(5000);
                  var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                  var out = new PrintWriter(socket.getOutputStream(), true);
                  out.print("220 localhost ESMTP\r\n");
                  out.flush();
                  String command = in.readLine();
                  commands.add(command);
                  out.print("250-localhost\r\n250 AUTH PLAIN\r\n");
                  out.flush();
                  while ((command = in.readLine()) != null) {
                    commands.add(command);
                    if (command.startsWith("QUIT")) {
                      out.print("221 bye\r\n");
                      out.flush();
                      break;
                    }
                  }
                } catch (IOException ignored) {
                }
              });
      peer.setDaemon(true);
      peer.start();
      assertThatThrownBy(
              () ->
                  transport.send(
                      settings(server.getLocalPort(), false), "test-password", request()))
          .isInstanceOf(jakarta.mail.MessagingException.class)
          .hasMessageContaining("STARTTLS is required");
      peer.join(6000);
      assertThat(commands).anyMatch(c -> c.startsWith("EHLO"));
      assertThat(commands)
          .noneMatch(c -> c.startsWith("AUTH") || c.startsWith("MAIL") || c.startsWith("DATA"));
    }
  }

  @Test
  void untrustedImplicitTlsCertificateIsRejected() throws Exception {
    Path store = temporary.resolve("synthetic.p12");
    var keytool =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair",
                "-alias",
                "test",
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "1",
                "-dname",
                "CN=localhost",
                "-ext",
                "SAN=dns:localhost",
                "-storetype",
                "PKCS12",
                "-keystore",
                store.toString(),
                "-storepass",
                "test-only-password",
                "-keypass",
                "test-only-password",
                "-noprompt")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start();
    assertThat(keytool.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    assertThat(keytool.exitValue()).isZero();
    var keys = KeyStore.getInstance("PKCS12");
    try (var in = Files.newInputStream(store)) {
      keys.load(in, "test-only-password".toCharArray());
    }
    var km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    km.init(keys, "test-only-password".toCharArray());
    var context = SSLContext.getInstance("TLS");
    context.init(km.getKeyManagers(), null, null);
    try (var server =
        (SSLServerSocket)
            context
                .getServerSocketFactory()
                .createServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      server.setSoTimeout(5000);
      Thread peer =
          new Thread(
              () -> {
                try (var socket = (SSLSocket) server.accept()) {
                  socket.setSoTimeout(5000);
                  socket.startHandshake();
                } catch (IOException ignored) {
                }
              });
      peer.setDaemon(true);
      peer.start();
      assertThatThrownBy(
              () ->
                  transport.send(settings(server.getLocalPort(), true), "test-password", request()))
          .isInstanceOf(jakarta.mail.MessagingException.class)
          .hasStackTraceContaining("SSLHandshakeException")
          .hasStackTraceContaining("PKIX");
      peer.join(6000);
    }
  }
}
