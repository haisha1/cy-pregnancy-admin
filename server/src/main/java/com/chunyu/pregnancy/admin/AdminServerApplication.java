package com.chunyu.pregnancy.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 独立管理服务入口；扫描范围不包含小程序用户端，防止两套Controller/鉴权误装配。 */
// 启动注解：启用自动配置，并从当前包向下扫描 Controller、Service 等 Spring 组件。
@SpringBootApplication
// 扫描指定 Mapper 包，交给 MyBatis 创建接口代理；不用手写 Mapper 实现类。
@MapperScan("com.chunyu.pregnancy.admin.mapper")
public class AdminServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(AdminServerApplication.class, args);
  }
}
