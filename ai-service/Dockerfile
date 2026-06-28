# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-17 AS ai-build
WORKDIR /workspace
ARG MAVEN_MIRROR_URL=https://repo.maven.apache.org/maven2
ENV MAVEN_MIRROR_URL=${MAVEN_MIRROR_URL}
COPY docker/maven/settings.xml /root/.m2/settings.xml
COPY ai-service/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests dependency:go-offline
COPY ai-service/src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=ai-build /workspace/target/tianshi-ai-service-0.0.1-SNAPSHOT.jar /app/academic-nexus-ai-service.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/academic-nexus-ai-service.jar"]
