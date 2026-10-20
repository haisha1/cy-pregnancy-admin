FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
RUN groupadd --system pregnancy && useradd --system --gid pregnancy pregnancy
COPY --from=build --chown=pregnancy:pregnancy /workspace/target/cy-pregnancy-admin-server-0.1.0-SNAPSHOT.jar /app/app.jar
USER pregnancy
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 18732
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
