FROM maven:3.6.3-openjdk-15

WORKDIR /app

COPY . .

RUN ["mvn", "install"]

ENTRYPOINT ["java", "-jar", "./target/giveaway.discord.bot-0.0.1-SNAPSHOT.jar"]