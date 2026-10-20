package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** 所有管理列表共用的有界查询。筛选先于游标分页执行，不在当前页假装全库搜索。 */
@Getter
@Setter
public class AdminListQuery {
  @Min(1)
  private Long beforeId;

  @Min(1)
  @Max(100)
  private int limit = 20;

  @Size(max = 100)
  private String q;

  @Size(max = 64)
  private String category;

  @Pattern(regexp = "|draft|submitted|approved|rejected|published|withdrawn|active|disabled")
  private String state;

  @Size(max = 40)
  private String role;

  @Size(max = 64)
  private String action;

  @Size(max = 40)
  private String result;

  @Pattern(regexp = "|\\d{4}-\\d{2}-\\d{2}")
  private String start;

  @Pattern(regexp = "|\\d{4}-\\d{2}-\\d{2}")
  private String end;
}
