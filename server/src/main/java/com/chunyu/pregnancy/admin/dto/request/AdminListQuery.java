package com.chunyu.pregnancy.admin.dto.request;

import javax.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/** 所有管理列表共用的有界查询。筛选先于游标分页执行，不在当前页假装全库搜索。 */
// Lombok 在编译期生成 getter；IDE 需要启用注解处理，运行时不靠反射生成方法。
@Getter
// Lombok 在编译期生成 setter，减少字段赋值方法的重复代码。
@Setter
public class AdminListQuery {
  // 限制数值下界，常用于限制有效期、页大小等参数。
  @Min(1)
  private Long beforeId;

  @Min(1)
  // 限制数值上界，防止一次请求读取过多数据或传入超大范围。
  @Max(100)
  private int limit = 20;

  // 限制非 null 文本的长度或集合大小；是否必填还需配合 NotNull、NotBlank 等约束。
  @Size(max = 100)
  private String q;

  @Size(max = 64)
  private String category;

  // 要求非 null 文本匹配指定正则；用户名、状态值等只接受约定格式。
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
