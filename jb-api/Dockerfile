FROM openjdk:10.0.2-jre-slim
MAINTAINER Caramel <goldrenard@gmail.com>
RUN mkdir -p /JuniperBot-API/logs /JuniperBot-API/temp
WORKDIR /JuniperBot-API
COPY build/libs/JuniperBot-API.jar JuniperBot-API.jar
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar JuniperBot-API.jar