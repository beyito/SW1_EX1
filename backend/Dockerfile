# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY backend/.mvn/ backend/.mvn/
COPY backend/mvnw backend/pom.xml backend/
RUN cd backend && chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY backend/ backend/
RUN cd backend && ./mvnw -q -DskipTests package

FROM amazoncorretto:21-alpine
WORKDIR /app

COPY --from=build /workspace/backend/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
