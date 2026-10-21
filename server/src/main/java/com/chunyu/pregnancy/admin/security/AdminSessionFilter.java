package com.chunyu.pregnancy.admin.security;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.common.result.ApiResponse;
import com.chunyu.pregnancy.admin.service.AdminSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.*;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** JWT验签后加载权威会话和实时权限；数据库不可用返回503，不能跳过安全校验。 */
public class AdminSessionFilter extends OncePerRequestFilter {
  private final AdminSessionService sessions;
  private final ObjectMapper json;

  public AdminSessionFilter(AdminSessionService sessions, ObjectMapper json) {
    this.sessions = sessions;
    this.json = json;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken) {
      Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
      try {
        // 权限从数据库会话服务实时读取，不依赖长期 JWT 里可能过期的角色信息。
        List<String> permissions =
            sessions.permissions(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("sid"));
        // 把权限码转换为 Spring Security 的 authority，供下游 PreAuthorize 检查。
        SecurityContextHolder.getContext()
            .setAuthentication(
                new JwtAuthenticationToken(
                    jwt,
                    permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList())));
      } catch (BizException ex) {
        error(response, ex.getStatus(), ex.getCode(), ex.getMessage());
        return;
      } catch (IllegalArgumentException ex) {
        error(response, 401, "AUTH_EXPIRED", "管理凭据无效");
        return;
      } catch (DataAccessException ex) {
        error(response, 503, "AUTH_UNAVAILABLE", "会话校验暂不可用");
        return;
      }
    }
    // 身份及会话检查通过后继续执行；前面的错误分支已写入响应并返回，不再进入业务接口。
    chain.doFilter(request, response);
  }

  private void error(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    SecurityContextHolder.clearContext();
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    json.writeValue(response.getOutputStream(), ApiResponse.error(code, message));
  }
}
