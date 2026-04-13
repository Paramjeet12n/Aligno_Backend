#
# Multi-stage build for Render "Docker" deploys.
# - Builder uses Maven + JDK 17
# - Runtime uses a small JRE 17 image
#

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Render sets $PORT; Spring reads server.port from PORT in application.yml
ENV PORT=8080

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

