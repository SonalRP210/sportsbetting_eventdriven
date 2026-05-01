FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl :risk-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/risk-service/target/risk-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
