# syntax=docker/dockerfile:1.4
# Requires compose build.additional_contexts.mtls → sibling mtls-secret-rotation repo.
FROM maven:3.9.9-eclipse-temurin-21 AS mtls-install
WORKDIR /mtls-src
COPY --from=mtls . .
RUN mvn -q install -DskipTests

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY --from=mtls-install /root/.m2 /root/.m2
COPY . .
RUN mvn -q -DskipTests -pl :api-gateway -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/api-gateway/target/api-gateway-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
