# GiveawayDiscordBot

[![CI/CD](https://github.com/megoRU/GiveawayDiscordBot/actions/workflows/ci_cd.yml/badge.svg?branch=main)](https://github.com/megoRU/GiveawayDiscordBot/actions/workflows/ci_cd.yml)
[![Docker Pulls](https://badgen.net/docker/pulls/megoru/giveaway?icon=docker\&label=pulls)](https://hub.docker.com/r/megoru/giveaway)
[![Docker Image Size](https://badgen.net/docker/size/megoru/giveaway?icon=docker\&label=image%20size)](https://hub.docker.com/r/megoru/giveaway)

A Discord bot for managing giveaways with ease.

---

## ✨ Features

* Create and manage giveaways via slash commands
* Predefined giveaways
* Scheduling giveaways
* Support ZoneId
* Persistent storage with MariaDB
* Docker-ready for quick deployment
* Built with JDA and Spring Boot

---

## 🚀 Quick Start

### Add the bot to your server

[Click here to invite](https://discord.com/oauth2/authorize?client_id=808277484524011531)

### Run with Docker

1. Place `docker-compose.yml` on your VPS (`/root` or another directory).
2. Configure it with your values (tokens, DB credentials, etc.).
3. Import `DiscordBotGiveaway.sql` into your MariaDB instance.
4. Start the container:

```bash
docker-compose up -d
```

5. Update to the latest image:

```bash
docker-compose pull && docker-compose up -d
```

6. Stop the bot:

```bash
docker-compose stop
```

---

## 🛠 Tech Stack

* Java 21
* Spring Boot
* Hibernate
* MariaDB
* Docker
* Maven
* [JDA](https://github.com/DV8FromTheWorld/JDA)

---

## 📄 License

This project is licensed under the [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html).

---

## 🔒 Privacy

Details on data handling are available in the [Privacy Policy](https://github.com/megoRU/GiveawayDiscordBot/tree/main/.github/privacy.md).