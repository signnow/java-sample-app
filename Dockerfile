FROM openjdk:23-jdk-slim
VOLUME /tmp
ARG JAR_FILE=target/java-sample-app-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
