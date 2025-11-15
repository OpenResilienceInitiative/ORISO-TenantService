package com.vi.tenantservice.api.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class TranslationService {

  private static final String DEFAULT_LANGUAGE = "de";
  private static final String LANGUAGE_COOKIE_NAME = "lang";

  public String getCurrentLanguageContext() {
    HttpServletRequest currentRequest =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    if (currentRequest.getCookies() != null) {
      Optional<Cookie> languageCookie = findLanguageCookie(currentRequest);
      if (languageCookie.isPresent()) {
        return languageCookie.get().getValue();
      }
    }
    return DEFAULT_LANGUAGE;
  }

  private static Optional<Cookie> findLanguageCookie(HttpServletRequest currentRequest) {
    return Arrays.stream(currentRequest.getCookies())
        .filter(cookie -> LANGUAGE_COOKIE_NAME.equals(cookie.getName()))
        .findFirst();
  }
}
