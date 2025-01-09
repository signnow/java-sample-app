FROM maven:3.9.9-eclipse-temurin-23 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn clean install package -DskipTests

FROM openjdk:23-jdk-slim
VOLUME /tmp
COPY --from=build /app/target/java-sample-app-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
