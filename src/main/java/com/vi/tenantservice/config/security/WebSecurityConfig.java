package com.vi.tenantservice.config.security;

import com.vi.tenantservice.api.config.SpringFoxConfig;
import lombok.RequiredArgsConstructor;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Configuration class to provide the keycloak security configuration. */
@KeycloakConfiguration
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
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/tenant")
                    .authenticated()
                    .requestMatchers("/tenant/*")
                    .authenticated()
                    .requestMatchers("/tenantadmin")
                    .authenticated()
                    .requestMatchers("/tenantadmin/*")
                    .authenticated()
                    .requestMatchers("/tenant/public/**")
                    .permitAll()
                    .requestMatchers(SpringFoxConfig.WHITE_LIST)
                    .permitAll()
                    .anyRequest()
                    .permitAll())
        .headers(
            headers ->
                headers
                    .xssProtection(xss -> xss.disable())
                    .contentSecurityPolicy(csp -> csp.policyDirectives("script-src 'self'")))
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

    return http.build();
  }
}
