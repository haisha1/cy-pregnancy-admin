package com.chunyu.pregnancy.admin.mapper;

import com.chunyu.pregnancy.admin.dto.response.RoleResponse;
import java.util.List;
import org.apache.ibatis.annotations.*;

/** 角色关联的显式SQL。所有参数通过绑定传入，不能把角色或排序字符串拼进SQL。 */
public interface AdminAccessMapper {
  // 声明查询 SQL；#{参数} 使用参数绑定，不把外部输入直接拼成 SQL 结构。
  @Select("SELECT id FROM admin_write_lock WHERE id=1 FOR UPDATE")
  int lockWrites();

  @Select("SELECT role_code FROM admin_operator_role WHERE operator_id=#{id} ORDER BY role_code")
  List<String> roles(@Param("id") Long operatorId);

  @Select(
      "SELECT DISTINCT p.permission_code FROM admin_operator_role r "
          + "JOIN admin_role_permission p ON p.role_code=r.role_code WHERE r.operator_id=#{id} ORDER BY p.permission_code")
  List<String> permissions(@Param("id") Long operatorId);

  @Select("SELECT code,name FROM admin_role ORDER BY code")
  List<RoleResponse> allRoles();

  @Select(
      "SELECT permission_code FROM admin_role_permission WHERE role_code=#{role} ORDER BY permission_code")
  List<String> rolePermissions(@Param("role") String role);

  @Select("SELECT COUNT(*) FROM admin_role WHERE code=#{role}")
  int roleExists(@Param("role") String role);

  // 声明删除 SQL；是否允许删除以及事务范围由业务层控制。
  @Delete("DELETE FROM admin_operator_role WHERE operator_id=#{id}")
  void clearRoles(@Param("id") Long operatorId);

  // 声明插入 SQL；参数通过 MyBatis 绑定，事务由调用它的业务层控制。
  @Insert("INSERT INTO admin_operator_role(operator_id,role_code) VALUES(#{id},#{role})")
  void assignRole(@Param("id") Long operatorId, @Param("role") String role);

  @Select(
      "SELECT COUNT(*) FROM admin_operator o JOIN admin_operator_role r ON o.id=r.operator_id "
          + "WHERE o.state='active' AND r.role_code='security_admin'")
  int activeSecurityAdmins();

  @Select(
      "SELECT COUNT(*) FROM admin_session s JOIN admin_operator o ON o.id=s.operator_id "
          + "WHERE s.client_uuid=#{sid} AND s.operator_id=#{id} AND s.revoked_at IS NULL "
          + "AND s.expires_at>UTC_TIMESTAMP(6) AND o.state='active'")
  int activeSession(@Param("id") Long operatorId, @Param("sid") String sid);

  // 声明更新 SQL；需要结合 WHERE 条件理解具体更新的记录范围。
  @Update(
      "UPDATE admin_session SET revoked_at=UTC_TIMESTAMP(6) WHERE operator_id=#{id} AND revoked_at IS NULL")
  void revokeAll(@Param("id") Long operatorId);

  @Update(
      "UPDATE admin_session SET revoked_at=UTC_TIMESTAMP(6) "
          + "WHERE operator_id=#{id} AND client_uuid=#{sid} AND revoked_at IS NULL")
  void revokeSession(@Param("id") Long operatorId, @Param("sid") String sid);
}
