FROM openjdk:10.0.2-jre-slim
MAINTAINER Caramel <goldrenard@gmail.com>
RUN mkdir -p /JuniperBot-Worker/logs /JuniperBot-Worker/temp
WORKDIR /JuniperBot-Worker
COPY build/libs/JuniperBot-Worker.jar JuniperBot-Worker.jar
COPY aiml/ aiml/bots/
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar JuniperBot-Worker.jar