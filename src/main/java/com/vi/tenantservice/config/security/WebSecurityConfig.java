package com.vi.tenantservice.config.security;

import com.vi.tenantservice.api.config.SpringFoxConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

/** Configuration class to provide the keycloak security configuration. */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

  @Autowired JwtAuthConverterProperties jwtAuthConverterProperties;

  @Autowired AuthorisationService authorisationService;

  @Bean
  public JwtAuthConverter jwtAuthConverter() {
    return new JwtAuthConverter(jwtAuthConverterProperties, authorisationService);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(new AntPathRequestMatcher("/tenant")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/tenant/*")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/tenantadmin")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/tenantadmin/*")).authenticated()
            .requestMatchers(new AntPathRequestMatcher("/tenant/public/**")).permitAll()
            .requestMatchers(SpringFoxConfig.WHITE_LIST).permitAll()
            .requestMatchers(new NegatedRequestMatcher(new AntPathRequestMatcher("/tenant"))).permitAll()
            .requestMatchers(new NegatedRequestMatcher(new AntPathRequestMatcher("/tenant/**"))).permitAll()
            .anyRequest().permitAll())
        .headers(headers -> headers
            .xssProtection(xss -> {})
            .contentSecurityPolicy(csp -> csp.policyDirectives("script-src 'self'")))
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

    return http.build();
  }
}
