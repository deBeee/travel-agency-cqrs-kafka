# -----------------------------------------------------------------------------------------------
# STAGE 1: BUILD
# -----------------------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B --no-transfer-progress

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B --no-transfer-progress

RUN java -Djarmode=tools -jar target/*.jar extract --destination target/extracted

# -----------------------------------------------------------------------------------------------
# STAGE 2: RUNTIME
# -----------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -g 10001 -S appgroup && adduser -u 10001 -S appuser -G appgroup && apk add --no-cache libstdc++ su-exec wget

WORKDIR /app

COPY --from=build --chown=appuser:appgroup /build/target/extracted/lib/   ./lib/
COPY --from=build --chown=appuser:appgroup /build/target/extracted/*.jar  ./

RUN mkdir -p /var/lib/travel-agency-query-side/kafka-streams

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "\
  STATE_DIR=\"${KAFKA_STREAMS_STATE_DIR:-/var/lib/travel-agency-query-side/kafka-streams}\"; \
  mkdir -p \"$STATE_DIR\" && chown -R appuser:appgroup \"$STATE_DIR\"; \
  exec su-exec appuser:appgroup java $JAVA_OPTS -jar *.jar \
"]