package com.chunyu.pregnancy.admin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 按内部递增ID倒序进行稳定游标分页，nextBeforeId为null表示本次没有下一页。 */
@Getter
@AllArgsConstructor
public class PageResponse<T> {
  private final List<T> records;
  private final Long nextBeforeId;
}
