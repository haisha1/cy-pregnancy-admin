package com.chunyu.pregnancy.admin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 独立公开DTO，绝不返回passwordHash。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
@AllArgsConstructor
public class OperatorResponse {
  private final String uuid;
  private final String username;
  private final String displayName;
  private final String state;
  private final long revision;
  private final List<String> roleCodes;
}
