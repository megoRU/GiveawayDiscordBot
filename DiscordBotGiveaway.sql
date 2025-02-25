CREATE TABLE `active_giveaways`
(
    `count_winners`        int(11) DEFAULT NULL,
    `finish`               bit(1) NOT NULL,
    `is_for_specific_role` bit(1)       DEFAULT NULL,
    `min_participants`     int(11) DEFAULT NULL,
    `channel_id`           bigint(20) NOT NULL,
    `created_user_id`      bigint(20) NOT NULL,
    `date_end`             datetime(6) DEFAULT NULL,
    `guild_id`             bigint(20) NOT NULL,
    `message_id`           bigint(20) NOT NULL,
    `role_id`              bigint(20) DEFAULT NULL,
    `title`                varchar(255) DEFAULT NULL,
    `url_image`            varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `language`
(
    `server_id` varchar(255) NOT NULL,
    `language`  varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

CREATE TABLE `list_users`
(
    `created_user_id` bigint(20) NOT NULL,
    `giveaway_id`     bigint(20) NOT NULL,
    `guild_id`        bigint(20) NOT NULL,
    `id`              bigint(20) NOT NULL,
    `user_id`         bigint(20) NOT NULL,
    `nick_name`       varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notification`
(
    `user_id_long`        varchar(255) NOT NULL,
    `notification_status` enum('ACCEPT','DENY') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `participants`
(
    `id`         bigint(20) NOT NULL,
    `message_id` bigint(20) NOT NULL,
    `user_id`    bigint(20) NOT NULL,
    `nick_name`  varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `scheduling`
(
    `count_winners`        int(11) DEFAULT NULL,
    `is_for_specific_role` bit(1)       DEFAULT NULL,
    `min_participants`     int(11) DEFAULT NULL,
    `channel_id`           bigint(20) NOT NULL,
    `create_giveaway`      datetime(6) NOT NULL,
    `created_user_id`      bigint(20) NOT NULL,
    `date_end`             datetime(6) DEFAULT NULL,
    `guild_id`             bigint(20) NOT NULL,
    `role_id`              bigint(20) DEFAULT NULL,
    `id_salt`              varchar(255) NOT NULL,
    `title`                varchar(255) DEFAULT NULL,
    `url_image`            varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE SEQUENCE sequence_id_auto_gen START WITH 1 INCREMENT BY 100;

CREATE TABLE `settings`
(
    `server_id` bigint(20) NOT NULL,
    `color_hex` varchar(255) DEFAULT NULL,
    `language`  varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE `active_giveaways`
    ADD PRIMARY KEY (`message_id`);


ALTER TABLE `language`
    ADD PRIMARY KEY (`server_id`);

ALTER TABLE `list_users`
    ADD PRIMARY KEY (`id`);

ALTER TABLE `notification`
    ADD PRIMARY KEY (`user_id_long`),
  ADD UNIQUE KEY `user_id_long` (`user_id_long`);


ALTER TABLE `participants`
    ADD PRIMARY KEY (`id`),
  ADD KEY `FK5wwgegod4ejelbpml5lgnic9b` (`message_id`);

ALTER TABLE `scheduling`
    ADD PRIMARY KEY (`id_salt`);

ALTER TABLE `settings`
    ADD PRIMARY KEY (`server_id`);

ALTER TABLE `list_users`
    MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;


ALTER TABLE `participants`
    MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2004;

ALTER TABLE `participants`
    ADD CONSTRAINT `FK5wwgegod4ejelbpml5lgnic9b` FOREIGN KEY (`message_id`) REFERENCES `active_giveaways` (`message_id`);
COMMIT;
