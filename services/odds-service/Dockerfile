FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl :odds-service -am package

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /sbin/nologin app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/services/odds-service/target/odds-service-0.1.0-SNAPSHOT.jar app.jar
USER app:app
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.io.tmpdir=/tmp", "-jar", "app.jar"]
