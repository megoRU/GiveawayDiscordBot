# Этап сборки приложения
FROM maven:3.9.9-amazoncorretto-21-al2023 AS builder
WORKDIR /app
COPY . .
RUN mvn install

# Этап запуска приложения
FROM openjdk:24
ENV LANG=C.UTF-8
WORKDIR /app
COPY --from=builder /app/target/GiveawayDiscordBot-0.0.1-SNAPSHOT.jar .
ENTRYPOINT ["java", "-jar", "./GiveawayDiscordBot-0.0.1-SNAPSHOT.jar"]