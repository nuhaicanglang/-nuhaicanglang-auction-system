FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./
COPY auction-common/pom.xml auction-common/pom.xml
COPY auction-framework/pom.xml auction-framework/pom.xml
COPY auction-system/pom.xml auction-system/pom.xml
COPY auction-business/pom.xml auction-business/pom.xml
COPY auction-search/pom.xml auction-search/pom.xml
COPY auction-job/pom.xml auction-job/pom.xml
COPY auction-mq/pom.xml auction-mq/pom.xml
COPY auction-admin/pom.xml auction-admin/pom.xml
RUN mvn -pl auction-admin -am dependency:go-offline -B

COPY . .
RUN mvn -pl auction-admin -am package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/logs /app/uploads && chown -R app:app /app
COPY --from=builder /build/auction-admin/target/auction-admin-*.jar /app/app.jar
RUN chown app:app /app/app.jar

USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
