package com.chunyu.pregnancy.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 写操作最小回执，可用于幂等重试；parentUuid在版本写入时指所属条目。 */
@Getter
@AllArgsConstructor
public class MutationResponse {
  private final String uuid;
  private final long revision;
  private final String state;
  private final String parentUuid;
}
