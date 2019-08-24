FROM openjdk:11.0.4-jre-slim-buster
MAINTAINER Caramel <goldrenard@gmail.com>
RUN mkdir -p /JuniperBot/logs /JuniperBot/temp
WORKDIR /JuniperBot
COPY build/libs/JuniperBot-Worker.jar JuniperBot-Worker.jar
COPY aiml/ aiml/bots/
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar JuniperBot-Worker.jar