FROM maven:3.9.4-eclipse-temurin-20 as build

WORKDIR /app

COPY . .

RUN ["mvn", "install"]

ENTRYPOINT ["java", "-jar", "./target/GiveawayDiscordBot-0.0.1-SNAPSHOT.jar"]