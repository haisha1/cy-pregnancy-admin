package com.chunyu.pregnancy.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 写操作最小回执，可用于幂等重试；parentUuid在版本写入时指所属条目。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
@AllArgsConstructor
public class MutationResponse {
  private final String uuid;
  private final long revision;
  private final String state;
  private final String parentUuid;
}
