package com.chunyu.pregnancy.admin.config;

import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.security.AdminSessionFilter;
import com.chunyu.pregnancy.admin.service.AdminSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/** 管理鉴权与小程序独立：issuer/audience/kind均校验，密码只存BCrypt散列。 无Cookie认证，因此关闭CSRF和HTTP Session；未开放任意来源CORS。 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class AdminSecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  public SecretKey adminJwtKey(AdminAuthProperties properties) {
    return new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  public JwtEncoder adminJwtEncoder(SecretKey key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  public JwtDecoder adminJwtDecoder(SecretKey key, AdminAuthProperties properties) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    OAuth2TokenValidator<Jwt> claims =
        jwt -> {
          boolean valid =
              jwt.getAudience().contains("admin")
                  && "admin_access".equals(jwt.getClaimAsString("kind"))
                  && jwt.getSubject() != null
                  && jwt.getSubject().matches("[1-9][0-9]*")
                  && jwt.getClaimAsString("sid") != null
                  && jwt.getExpiresAt() != null;
          return valid
              ? OAuth2TokenValidatorResult.success()
              : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            java.util.Arrays.asList(
                JwtValidators.createDefaultWithIssuer(properties.getIssuer()), claims)));
    return decoder;
  }

  @Bean
  public SecurityFilterChain adminSecurity(
      HttpSecurity http, ObjectMapper json, AdminSessionService sessions, Environment environment)
      throws Exception {
    http.csrf().disable().formLogin().disable().httpBasic().disable().logout().disable();
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    http.authorizeRequests()
        .antMatchers(HttpMethod.GET, "/internal/content/knowledge", "/internal/content/knowledge/*")
        .permitAll()
        .antMatchers("/actuator/health")
        .permitAll()
        .antMatchers(HttpMethod.POST, "/admin/api/v1/auth/login")
        .permitAll();
    if (environment.acceptsProfiles(Profiles.of("local"))) {
      http.authorizeRequests()
          .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
          .permitAll();
    }
    http.authorizeRequests().anyRequest().authenticated();
    org.springframework.security.web.AuthenticationEntryPoint unauthorized =
        (request, response, ex) -> {
          response.setStatus(401);
          response.setContentType("application/json;charset=UTF-8");
          json.writeValue(response.getOutputStream(), ApiResponse.error("AUTH_EXPIRED", "请先登录管理端"));
        };
    http.exceptionHandling()
        .authenticationEntryPoint(unauthorized)
        .accessDeniedHandler(
            (request, response, ex) -> {
              response.setStatus(403);
              response.setContentType("application/json;charset=UTF-8");
              json.writeValue(response.getOutputStream(), ApiResponse.error("FORBIDDEN", "没有访问权限"));
            });
    http.oauth2ResourceServer().authenticationEntryPoint(unauthorized).jwt();
    http.addFilterAfter(
        new AdminSessionFilter(sessions, json), BearerTokenAuthenticationFilter.class);
    return http.build();
  }
}
