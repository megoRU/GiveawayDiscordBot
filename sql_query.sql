CREATE TABLE `guildId`
(
    `id`           bigint(30) NOT NULL,
    `user_long_id` bigint(30) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ActiveGiveaways`
(
    `guild_long_id`     bigint(30) NOT NULL,
    `message_id_long`   bigint(30) NOT NULL,
    `channel_id_long`   bigint(30) NOT NULL,
    `count_winners`     varchar(255),
    `date_end_giveaway` varchar(255),
    `giveaway_title`    varchar(255),
    PRIMARY KEY (`guild_long_id`),
    UNIQUE KEY `guild_long_id` (`guild_long_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;