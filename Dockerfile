FROM openjdk:10.0.2-jre-slim
MAINTAINER Renard Gold <goldrenard@gmail.com>
WORKDIR /usr/share/JuniperBot
COPY jb-web/build/libs/JuniperBot.jar /JuniperBot.jar
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /JuniperBot.jar