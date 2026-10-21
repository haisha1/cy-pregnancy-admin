package com.chunyu.pregnancy.admin.config;

import javax.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 管理端独立密钥和会话配置，没有默认签名密钥；绝不读取用户端JWT_SECRET。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
// Lombok 在编译期生成 setter，减少字段赋值方法的重复代码。
@Setter
// 通用 Spring 组件：由容器创建并管理，其他类可通过构造器使用它。
@Component
// 启用此处的参数约束校验；配置类可校验配置值，Controller 可校验方法参数。
@Validated
// 按 prefix 批量绑定外部配置，避免在业务代码里硬编码连接信息、密钥或有效期。
@ConfigurationProperties(prefix = "app.auth")
public class AdminAuthProperties {
  // 要求文本非 null 且不能全是空白字符，用于必填文本。
  @NotBlank
  // 限制非 null 文本的长度或集合大小；是否必填还需配合 NotNull、NotBlank 等约束。
  @Size(min = 32)
  private String secret;

  @NotBlank private String issuer = "cy-pregnancy-admin";

  // 限制数值下界，常用于限制有效期、页大小等参数。
  @Min(60)
  // 限制数值上界，防止一次请求读取过多数据或传入超大范围。
  @Max(3600)
  private long accessSeconds = 1800;

  private boolean bootstrapEnabled;
  private String bootstrapUsername;
  private String bootstrapPassword;
}
