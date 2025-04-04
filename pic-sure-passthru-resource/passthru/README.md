# Passthru Resource

This service houses the long term credentials for remote PIC-SUREs, and routes traffic to them.
It also protects those PIC-SUREs during outages by avoiding sending requests to them while they're
down.

## Requirements

JDK 24
PIC-SURE Resource API artifacts

## Setup

1. Build the PIC-SURE API (https://github.com/hms-dbmi/pic-sure). We use artifacts from that
2. Set up an `application.properties` file in the root of this repo.
See `src/test/resources/application.properties` for a working example.
3. Run `mvn clean install`
4. Run `java -jar target/passthru-0.0.1-SNAPSHOT.jar`

## Docker Setup
1. Build the PIC-SURE API
2. Set up an `application.properties` file in the root of this repo.
   See `src/test/resources/application.properties` for a working example.
3. Copy your m2 to the root of this project: `cp -r ~/.m2 /`
4. Run `docker build . -t passthru`
5. Run `docker run --rm -v application.properties:/application.properties passthru`
