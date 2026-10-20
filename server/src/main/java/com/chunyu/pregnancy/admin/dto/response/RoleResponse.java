package com.chunyu.pregnancy.admin.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** 预置角色只读清单；角色/权限矩阵通过版本化SQL维护。 */
@Getter
@Setter
public class RoleResponse {
  private String code;
  private String name;
  private List<String> permissions;
}
