# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS app-build
WORKDIR /workspace
ARG MAVEN_MIRROR_URL=https://repo.maven.apache.org/maven2
ENV MAVEN_MIRROR_URL=${MAVEN_MIRROR_URL}
COPY docker/maven/settings.xml /root/.m2/settings.xml
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests dependency:go-offline
COPY src ./src
COPY --from=frontend-build /workspace/frontend/dist ./frontend/dist
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=app-build /workspace/target/tianshiwebside-0.0.1-SNAPSHOT.jar /app/academic-nexus-web.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/academic-nexus-web.jar"]
