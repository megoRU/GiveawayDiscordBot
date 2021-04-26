FROM maven:3.6.3-openjdk-15 AS build

COPY ./ /tmp

RUN mvn -f /tmp/pom.xml clean install

FROM openjdk:15

COPY --from=build ./tmp/target/ /maven

WORKDIR /maven

ENTRYPOINT ["java", "-jar", "GiveawayDiscord-1.0.4-jar-with-dependencies.jar"]