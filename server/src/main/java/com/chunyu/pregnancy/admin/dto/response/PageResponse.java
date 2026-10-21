package com.chunyu.pregnancy.admin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 按内部递增ID倒序进行稳定游标分页，nextBeforeId为null表示本次没有下一页。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
@AllArgsConstructor
public class PageResponse<T> {
  private final List<T> records;
  private final Long nextBeforeId;
}
