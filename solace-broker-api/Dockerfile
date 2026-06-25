FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/solace.broker.api-0.0.1-SNAPSHOT.war /app/app.war

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.war"]
