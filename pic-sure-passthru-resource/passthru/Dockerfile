FROM maven:3-amazoncorretto-24 AS build

COPY pom.xml .

COPY .m2 /root/.m2

RUN mvn -B dependency:go-offline

COPY src src

RUN mvn -B package -DskipTests

FROM amazoncorretto:24.0.0-alpine3.21

# Copy jar and access token from maven build
COPY --from=build target/passthru-*.jar /passthru.jar

# Time zone
ENV TZ="US/Eastern"

ENTRYPOINT java $DEBUG_VARS $PROXY_VARS -Xmx8192m -jar /passthru.jar
