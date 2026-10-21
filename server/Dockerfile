# Jenkins 使用 Java 8 和 Maven 构建 JAR；此处只生成运行镜像。
FROM eclipse-temurin:8-jre
WORKDIR /app
RUN command -v curl >/dev/null \
    && groupadd --system pregnancy && useradd --system --gid pregnancy pregnancy
COPY --chown=pregnancy:pregnancy target/cy-pregnancy-admin-server-0.1.0-SNAPSHOT.jar /app/app.jar
USER pregnancy
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 18732
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl --fail --silent --show-error --max-time 4 http://127.0.0.1:18732/actuator/health || exit 1
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
