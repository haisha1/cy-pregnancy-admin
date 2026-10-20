package com.chunyu.pregnancy.admin.common.ratelimit;

import com.chunyu.pregnancy.admin.common.exception.BizException;
import com.chunyu.pregnancy.admin.common.util.DigestUtils;
import java.util.Collections;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** 原子窗口限流。连接IP和账号分别计数，Redis故障时拒绝登录，不静默放开保护。 */
@Component
public class AdminLoginRateLimiter {
  private final StringRedisTemplate redis;
  private final DefaultRedisScript<Long> script =
      new DefaultRedisScript<>(
          "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],300) end; return n",
          Long.class);

  public AdminLoginRateLimiter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void check(String ip, String username) {
    checkKey("ip:" + DigestUtils.sha256(ip), 60);
    checkKey("user:" + DigestUtils.sha256(username), 10);
  }

  private void checkKey(String suffix, int limit) {
    Long count;
    try {
      count = redis.execute(script, Collections.singletonList("cy:admin:login:" + suffix));
    } catch (RuntimeException ex) {
      throw new BizException(503, "AUTH_UNAVAILABLE", "登录服务暂不可用");
    }
    if (count == null) {
      throw new BizException(503, "AUTH_UNAVAILABLE", "登录服务暂不可用");
    }
    if (count > limit) {
      throw new BizException(429, "RATE_LIMITED", "登录操作频繁，请五分钟后重试");
    }
  }
}
