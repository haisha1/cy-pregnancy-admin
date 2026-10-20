package com.chunyu.pregnancy.admin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 当前账号和实时权限，前端隐藏按钮不能替代服务端鉴权。 */
@Getter
@AllArgsConstructor
public class MeResponse {
  private final OperatorResponse operator;
  private final List<String> permissions;
}
