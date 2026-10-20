package com.chunyu.pregnancy.admin.service;

import com.chunyu.pregnancy.admin.dto.request.AdminListQuery;
import com.chunyu.pregnancy.admin.dto.response.PageResponse;
import java.util.*;

/** Web读能力统一契约，查询实现不参与内容状态写事务。 */
public interface AdminReadService {
  PageResponse<Map<String, Object>> knowledge(AdminListQuery q, String mode, Long actor);

  PageResponse<Map<String, Object>> operators(AdminListQuery q);

  Map<String, Object> operator(String uuid);

  PageResponse<Map<String, Object>> audits(AdminListQuery q);

  Map<String, Object> summary(Long actor);

  Map<String, Object> users(AdminListQuery q);

  Map<String, Object> user(String uuid);

  Map<String, Object> userStats(int days);

  Map<String, Object> health();
}
