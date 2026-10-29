package com.vi.tenantservice.api.service.search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class TenantSearchText {
  private TenantSearchText() {}

  public static String fold(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT);
  }

  public static List<String> tokens(String text) {
    if (text == null || text.replaceAll("(?U)^\\s+|\\s+$", "").equals("*")) return List.of();
    return Arrays.stream(text.split("(?U)\\s+"))
        .filter(t -> !t.isEmpty())
        .map(TenantSearchText::fold)
        .filter(t -> !t.isEmpty())
        .distinct()
        .toList();
  }
}
