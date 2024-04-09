FROM maven:3.9.6-amazoncorretto-21-al2023

ENV LANG=C.UTF-8

WORKDIR /app

# Копируем только pom.xml, чтобы установить зависимости
COPY pom.xml .

# Устанавливаем зависимости без кэша и удаляем временные файлы
RUN mvn -B dependency:go-offline && rm -rf /root/.m2

# Копируем исходный код
COPY src/ src/

# Собираем приложение
RUN ["mvn", "install", "-Dmaven.test.skip=true"]

# Указываем точку входа
ENTRYPOINT ["java", "-jar", "GiveawayDiscordBot-0.0.1-SNAPSHOT.jar"]