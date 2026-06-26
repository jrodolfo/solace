FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache --upgrade p11-kit p11-kit-trust

WORKDIR /app
COPY --from=build /workspace/target/solace-subscriber-1.0-SNAPSHOT-all.jar /app/solace-subscriber.jar

ENTRYPOINT ["java", "-jar", "/app/solace-subscriber.jar"]
