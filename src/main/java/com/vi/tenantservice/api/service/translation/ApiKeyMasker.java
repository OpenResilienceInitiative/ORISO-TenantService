package com.vi.tenantservice.api.service.translation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Masks provider API keys for admin read endpoints: keys can be set but never read back in full.
 * Follows the password-type admin field convention (e.g. the SMTP password example "********"), but
 * keeps a recognisable prefix + the last 4 characters so admins can tell keys apart.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiKeyMasker {

  private static final int PREFIX_LENGTH = 3;
  private static final int SUFFIX_LENGTH = 4;
  private static final int MIN_LENGTH_FOR_PARTIAL_REVEAL = 12;
  private static final String ELLIPSIS = "…";

  /**
   * Returns e.g. {@code sk-…abcd} for a long key, a fully masked {@code …} for short keys and
   * {@code null} when no key is configured.
   */
  public static String mask(String apiKey) {
    if (StringUtils.isBlank(apiKey)) {
      return null;
    }
    if (apiKey.length() < MIN_LENGTH_FOR_PARTIAL_REVEAL) {
      return ELLIPSIS;
    }
    return apiKey.substring(0, PREFIX_LENGTH)
        + ELLIPSIS
        + apiKey.substring(apiKey.length() - SUFFIX_LENGTH);
  }
}
