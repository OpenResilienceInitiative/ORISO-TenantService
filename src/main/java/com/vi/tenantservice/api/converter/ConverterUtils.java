package com.vi.tenantservice.api.converter;

import com.google.common.collect.Lists;
import java.util.List;

public class ConverterUtils {

  private ConverterUtils() {}

  public static final String DE = "de";

  public static boolean nullAsFalse(Boolean topicsInRegistrationEnabled) {
    return Boolean.TRUE.equals(topicsInRegistrationEnabled);
  }

  public static List<String> nullAsGerman(List<String> activeLanguages) {
    if (activeLanguages == null) {
      return Lists.newArrayList(DE);
    }
    return activeLanguages;
  }
}
