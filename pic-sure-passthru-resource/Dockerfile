FROM amazoncorretto:24.0.0-alpine3.21

# Copy jar and access token from maven build
COPY --from=build target/passthru-*.jar /passthru.jar

# Time zone
ENV TZ="US/Eastern"

ENTRYPOINT java $DEBUG_VARS $PROXY_VARS -Xmx8192m -jar /passthru.jar
