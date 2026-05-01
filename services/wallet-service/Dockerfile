FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -DskipTests -pl :wallet-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/services/wallet-service/target/wallet-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
