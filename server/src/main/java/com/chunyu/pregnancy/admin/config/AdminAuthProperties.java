package com.chunyu.pregnancy.admin.config;

import javax.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 管理端独立密钥和会话配置，没有默认签名密钥；绝不读取用户端JWT_SECRET。 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AdminAuthProperties {
  @NotBlank
  @Size(min = 32)
  private String secret;

  @NotBlank private String issuer = "cy-pregnancy-admin";

  @Min(60)
  @Max(3600)
  private long accessSeconds = 1800;

  private boolean bootstrapEnabled;
  private String bootstrapUsername;
  private String bootstrapPassword;
}
