package com.chunyu.pregnancy.admin.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 散列工具仅依赖JDK，供身份索引、凭据散列和限流键复用，不反向依赖业务/安全层。 */
public final class DigestUtils {
  private DigestUtils() {}

  public static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder(64);
      for (byte b : digest) {
        result.append(String.format("%02x", b & 0xff));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("JVM缺少SHA-256算法", ex);
    }
  }
}
