FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl :betting-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/betting-service/target/betting-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
