package com.chunyu.pregnancy.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 独立管理服务入口；扫描范围不包含小程序用户端，防止两套Controller/鉴权误装配。 */
@SpringBootApplication
@MapperScan("com.chunyu.pregnancy.admin.mapper")
public class AdminServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(AdminServerApplication.class, args);
  }
}
