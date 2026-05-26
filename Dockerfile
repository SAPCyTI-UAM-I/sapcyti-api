# SPEC-009 — multi-stage build: Maven → Eclipse Temurin 21 JRE
# Build: docker build -t sapcyti-api:local .

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw -B dependency:go-offline -DskipTests

COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 sapcyti \
    && useradd --system --uid 10001 --gid sapcyti --home-dir /app --no-create-home sapcyti

WORKDIR /app

COPY --from=build --chown=sapcyti:sapcyti /app/target/*.jar /app/app.jar

USER sapcyti

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=docker

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
