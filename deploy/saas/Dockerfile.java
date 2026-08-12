FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S erp \
    && adduser -S -G erp -h /opt/erp erp

WORKDIR /opt/erp

ARG SERVICE_JAR
COPY --chown=erp:erp ${SERVICE_JAR} /opt/erp/app.jar

USER erp

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/erp/app.jar"]
