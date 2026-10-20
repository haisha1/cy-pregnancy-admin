package com.chunyu.pregnancy.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chunyu.pregnancy.admin.entity.AuditEvent;

/** AuditEvent基础持久化；业务判断和事务放在Service，Controller不直接调用Mapper。 */
public interface AuditEventMapper extends BaseMapper<AuditEvent> {}
