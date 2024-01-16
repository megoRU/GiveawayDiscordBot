package main.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

@Service
public class UpdateSlashService {

    private final static Logger LOGGER = LoggerFactory.getLogger(UpdateSlashService.class.getName());

    public void updateSlash(JDA jda) {
        try {
            CommandListUpdateAction commands = jda.updateCommands();

            //Get participants
            List<OptionData> participants = new ArrayList<>();
            participants.add(new OptionData(STRING, "giveaway_id", "Giveaway ID")
                    .setName("giveaway_id")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Stop
            List<OptionData> optionsStop = new ArrayList<>();
            optionsStop.add(new OptionData(INTEGER, "count", "Examples: 1, 2... If not specified -> default value at startup")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 1, 2... Если не указано -> стандартное значение при запуске"));

            //Set language
            List<OptionData> optionsSettings = new ArrayList<>();
            optionsSettings.add(new OptionData(STRING, "language", "Setting the bot language")
                    .addChoice("\uD83C\uDDEC\uD83C\uDDE7 English Language", "eng")
                    .addChoice("\uD83C\uDDF7\uD83C\uDDFA Russian Language", "rus")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота"));

            optionsSettings.add(new OptionData(STRING, "color", "Embed color: #00FF00")
                    .setName("color")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Embed цвет: #00FF00"));

            //Scheduling Giveaway
            List<OptionData> optionsScheduling = new ArrayList<>();

            optionsScheduling.add(new OptionData(STRING, "start_time", "Examples: 2023.04.29 16:00. Only in this style and UTC ±0")
                    .setName("start_time")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 2023.04.29 16:00. Только в этом стиле и UTC ±0"));

            optionsScheduling.add(new OptionData(CHANNEL, "channel", "Choose #TextChannel")
                    .setName("textchannel")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Выбрать #TextChannel"));

            optionsScheduling.add(new OptionData(STRING, "end_time", "Examples: 2023.04.29 17:00. Only in this style and UTC ±0")
                    .setName("end_time")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 2023.04.29 17:00. Только в этом стиле и UTC ±0"));

            optionsScheduling.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "count", "Set count winners. Default 1")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsScheduling.add(new OptionData(ROLE, "mention", "Mentioning a specific @Role")
                    .setName("mention")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsScheduling.add(new OptionData(STRING, "role", "Giveaway is only for a specific role? Don't forget to specify the Role in the previous choice.")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway предназначен только для определенной роли? Не забудьте указать роль в предыдущем выборе."));

            optionsScheduling.add(new OptionData(ATTACHMENT, "image", "Set Image for Giveaway")
                    .setName("image")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "min_participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min_participants")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            optionsScheduling.add(new OptionData(ROLE, "forbidden_role", "Prohibit participation for a specific @Role")
                    .setName("forbidden_role")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Запретить участвовать для определенной @Роли"));

            //Start Giveaway
            List<OptionData> optionsStart = new ArrayList<>();
            optionsStart.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "count", "Set count winners. Default 1")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsStart.add(new OptionData(STRING, "duration", "Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style and UTC ±0")
                    .setName("duration")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            optionsStart.add(new OptionData(ROLE, "mention", "Mentioning a specific @Role")
                    .setName("mention")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsStart.add(new OptionData(STRING, "role", "Giveaway is only for a specific role? Don't forget to specify the Role in the previous choice.")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway предназначен только для определенной роли? Не забудьте указать роль в предыдущем выборе."));

            optionsStart.add(new OptionData(ATTACHMENT, "image", "Set Image for Giveaway")
                    .setName("image")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "min_participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min_participants")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            List<OptionData> predefined = new ArrayList<>();
            predefined.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway")
                    .setRequired(true));
            predefined.add(new OptionData(INTEGER, "count", "Set count winners")
                    .setName("count")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей")
                    .setRequired(true));
            predefined.add(new OptionData(ROLE, "role", "Installing a @Role for collecting")
                    .setName("role")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установка @Роли для сбора")
                    .setRequired(true));

            //change
            List<OptionData> change = new ArrayList<>();
            change.add(new OptionData(STRING, "duration", "Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style and UTC ±0")
                    .setName("duration")
                    .setRequired(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            List<OptionData> reroll = new ArrayList<>();
            reroll.add(new OptionData(STRING, "giveaway_id", "Giveaway ID")
                    .setName("giveaway_id")
                    .setRequired(true).setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            List<OptionData> botPermissions = new ArrayList<>();
            botPermissions.add(new OptionData(CHANNEL, "textchannel", "Checking the permissions of a specific channel")
                    .setName("textchannel")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений определенного канала"));

            commands.addCommands(Commands.slash("check-bot-permission", "Checking the permission bot")
                    .addOptions(botPermissions)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений бота"));

            commands.addCommands(Commands.slash("settings", "Bot settings")
                    .addOptions(optionsSettings)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройки бота")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)));

            commands.addCommands(Commands.slash("start", "Create Giveaway")
                    .addOptions(optionsStart)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создание Giveaway"));

            commands.addCommands(Commands.slash("scheduling", "Create Scheduling Giveaway")
                    .addOptions(optionsScheduling)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создать Giveaway по расписанию"));

            commands.addCommands(Commands.slash("stop", "Stop the Giveaway")
                    .addOptions(optionsStop)
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Остановить Giveaway"));

            commands.addCommands(Commands.slash("help", "Bot commands")
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота"));

            commands.addCommands(Commands.slash("list", "List of participants")
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Список участников"));

            commands.addCommands(Commands.slash("patreon", "Support us on Patreon")
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Поддержите нас на Patreon"));

            commands.addCommands(Commands.slash("participants", "Get file with all participants")
                    .addOptions(participants)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Получить файл со всеми участниками"));

            commands.addCommands(Commands.slash("reroll", "Reroll one winner")
                    .addOptions(reroll)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Перевыбрать одного победителя"));

            commands.addCommands(Commands.slash("change", "Change the time")
                    .addOptions(change)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Изменить время")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

            commands.addCommands(Commands.slash("predefined", "Gather participants and immediately hold a drawing for a certain @Role")
                    .addOptions(predefined)
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Собрать участников и сразу провести розыгрыш для определенной @Роли")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)));

            commands.addCommands(Commands.slash("cancel", "Cancel Giveaway")
                    .setGuildOnly(true)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Отменить Giveaway"));

            commands.queue();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}