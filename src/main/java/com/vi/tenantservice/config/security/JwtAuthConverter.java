package com.vi.tenantservice.config.security;

import com.vi.tenantservice.api.authorisation.Authority;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final String TECHNICAL_AUTHORITY_PREFIX =
      Authority.AuthorityValue.PREFIX + "TECHNICAL_";

  private final @NonNull AuthorisationService authorisationService;

  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
      new JwtGrantedAuthoritiesConverter();

  private final JwtAuthConverterProperties properties;

  private final TechnicalServiceIdentity technicalServiceIdentity;

  public JwtAuthConverter(
      JwtAuthConverterProperties properties,
      AuthorisationService authorisationService,
      TechnicalServiceIdentity technicalServiceIdentity) {
    this.properties = properties;
    this.authorisationService = authorisationService;
    this.technicalServiceIdentity = technicalServiceIdentity;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    var authorities = getGrantedAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities, getPrincipalClaimName(jwt));
  }

  private Collection<GrantedAuthority> getGrantedAuthorities(Jwt jwt) {
    Collection<GrantedAuthority> convertedGrantedAuthorities =
        jwtGrantedAuthoritiesConverter.convert(jwt);
    Stream<GrantedAuthority> authorities =
        convertedGrantedAuthorities != null
            ? Stream.concat(
                convertedGrantedAuthorities.stream(),
                authorisationService.extractRealmAuthorities(jwt).stream())
            : authorisationService.extractRealmAuthorities(jwt).stream();
    // The role alone is not the service identity: technical-only rights need its subject too.
    if (!technicalServiceIdentity.isServiceIdentity(jwt)) {
      authorities = authorities.filter(authority -> !isTechnicalOnly(authority));
    }
    return authorities.collect(Collectors.toSet());
  }

  private static boolean isTechnicalOnly(GrantedAuthority authority) {
    return authority.getAuthority() != null
        && authority.getAuthority().startsWith(TECHNICAL_AUTHORITY_PREFIX);
  }

  private String getPrincipalClaimName(Jwt jwt) {
    String claimName = JwtClaimNames.SUB;
    if (properties.getPrincipalAttribute() != null) {
      claimName = properties.getPrincipalAttribute();
    }
    return jwt.getClaim(claimName);
  }
}
