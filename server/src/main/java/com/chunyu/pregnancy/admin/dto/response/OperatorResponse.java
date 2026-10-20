package com.chunyu.pregnancy.admin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 独立公开DTO，绝不返回passwordHash。 */
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
