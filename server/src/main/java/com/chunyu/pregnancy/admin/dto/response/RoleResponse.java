package com.chunyu.pregnancy.admin.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** 预置角色只读清单；角色/权限矩阵通过版本化SQL维护。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
// Lombok 在编译期生成 setter，减少字段赋值方法的重复代码。
@Setter
public class RoleResponse {
  private String code;
  private String name;
  private List<String> permissions;
}
