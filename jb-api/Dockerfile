FROM openjdk:11.0.4-jre-slim-buster
MAINTAINER Caramel <goldrenard@gmail.com>
RUN mkdir -p /JuniperBot-API/logs /JuniperBot-API/temp
WORKDIR /JuniperBot-API
COPY build/libs/JuniperBot-API.jar JuniperBot-API.jar
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar JuniperBot-API.jar