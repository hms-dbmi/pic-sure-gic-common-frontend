FROM maven:3-amazoncorretto-24 AS build

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src src

RUN mvn -B package -DskipTests

FROM amazoncorretto:24.0.0-alpine3.21

# Copy jar and access token from maven build
COPY --from=build target/propertiesupdater-*.jar /propertiesupdater.jar

# Time zone
ENV TZ="US/Eastern"

ENTRYPOINT ["java", "-jar", "/propertiesupdater.jar"]
CMD []
