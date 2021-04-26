FROM openjdk:15

FROM maven:3.6.3-openjdk-15 AS  clean

FROM maven:3.6.3-openjdk-15 AS  install

COPY ./ /tmp

WORKDIR /tmp

RUN ["mvn", "clean"]

RUN ["mvn", "install"]

COPY ./target/ /tmp

WORKDIR /tmp

ENTRYPOINT ["java","-jar", "GiveawayDiscord-1.0.4-jar-with-dependencies.jar"]