# GiveawayDiscordBot
[![CI/CD](https://github.com/megoRU/GiveawayDiscordBot/actions/workflows/ci_cd.yml/badge.svg?branch=main)](https://github.com/megoRU/GiveawayDiscordBot/actions/workflows/ci_cd.yml)
[![Discord](https://img.shields.io/discord/779317239722672128?label=Discord)](https://discord.gg/UrWG3R683d)
[![Docker Pulls](https://badgen.net/docker/pulls/megoru/giveaway?icon=docker&label=pulls)](https://hub.docker.com/r/megoru/giveaway)
[![Docker Image Size](https://badgen.net/docker/size/megoru/giveaway?icon=docker&label=image%20size)](https://hub.docker.com/r/megoru/giveaway)

## LICENSE

This work is licensed under a [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html)

## Running on your server

1. Move `docker-compose.yml` at the root `/root` VPS server.
2. Fill it with your data.
3. Import tables to your MariaDB: `DiscordBotGiveaway.sql`
4. Launch the container: `docker-compose up -d`
5. If you need to update the repository: `docker-compose pull`
6. If you need to stop: `docker-compose stop`

## Add bot to your guild
[Add Giveaway](https://discord.com/oauth2/authorize?client_id=808277484524011531&permissions=277025647680&scope=applications.commands+bot)

## Copyright Notice

1.  The bot is made using the library: [JDA](https://github.com/DV8FromTheWorld/JDA)

## Privacy Policy

Here you can read more about what we store and how we store it. [Privacy Policy](https://github.com/megoRU/GiveawayDiscordBot/tree/main/.github/privacy.md)
