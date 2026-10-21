package com.chunyu.pregnancy.admin.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.*;

/** 只生成已经存在的控制器契约；本地文档不代表已开放公网或接通用户端消费。 */
// 配置类：集中声明需要交给 Spring 管理的对象及框架行为。
@Configuration
public class AdminOpenApiConfig {
  // 把方法返回的对象注册为 Spring Bean；参数中的依赖由 Spring 从容器中提供。
  @Bean
  public OpenAPI adminOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("产科工具 · 独立管理API")
                .version("0.1.0")
                .description("账号/角色/审计与知识审核发布；管理发布暂不自动推送小程序。"))
        .addServersItem(new Server().url("/"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "adminBearer",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
