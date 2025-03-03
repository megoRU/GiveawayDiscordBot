package main.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

@Service
public class SlashService {

    private final static Logger LOGGER = LoggerFactory.getLogger(SlashService.class.getName());
    private final static Map<String, Long> commandMap = new HashMap<>();

    public void updateSlash(JDA jda) {
        try {
            CommandListUpdateAction commands = jda.updateCommands();

            //Get participants
            List<OptionData> participants = new ArrayList<>();
            participants.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Stop
            List<OptionData> optionsStop = new ArrayList<>();
            optionsStop.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to value set at start of Giveaway")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию установленное в Giveaway"));

            optionsStop.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            //Set language
            List<OptionData> optionsSettings = new ArrayList<>();
            optionsSettings.add(new OptionData(STRING, "language", "Set the bot language")
                    .addChoice("\uD83C\uDDEC\uD83C\uDDE7 English Language", "eng")
                    .addChoice("\uD83C\uDDF7\uD83C\uDDFA Russian Language", "rus")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "язык")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройка языка бота"));

            optionsSettings.add(new OptionData(STRING, "color", "Set the embed color. Example usage: #ff00ff")
                    .setName("color")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "цвет")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить цвет embed. Пример использования: #ff00ff"));

            //Scheduling Giveaway
            List<OptionData> optionsScheduling = new ArrayList<>();

            optionsScheduling.add(new OptionData(STRING, "start-time", "Set start time in UTC ±0 form. Example usage: 2023.04.29 17:00")
                    .setName("start-time")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "время-начала")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить время начала в формате UTC ±0. Пример использования: 2023.04.29 17:00"));

            optionsScheduling.add(new OptionData(CHANNEL, "channel", "Установить канал для Giveaway. По умолчанию канал в котором была запущена команда")
                    .setName("text-channel")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текстовый-канал")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "По умолчанию текстовый канал, в котором была выполнена команда."));

            optionsScheduling.add(new OptionData(STRING, "end-time", "Set end time in UTC ±0 form. Example usage: 2023.04.29 17:00")
                    .setName("end-time")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "время-окончания")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Укажите время окончания в формате UTC ±0. Пример использования: 2023.04.29 17:00"));

            optionsScheduling.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsScheduling.add(new OptionData(ROLE, "mention", "Mention a specific @Role")
                    .setName("select")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "выбрать")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsScheduling.add(new OptionData(STRING, "role", "Set whether Giveaway is for a specific role. Role is set in previous selection")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway предназначен для определенной роли? Установите роль в предыдущем выборе"));

            optionsScheduling.add(new OptionData(ATTACHMENT, "image", "Set image used in Giveaway embed")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsScheduling.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is lesser than this number")
                    .setName("min-participants")
                    .setMinValue(2)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            //Start Giveaway
            List<OptionData> optionsStart = new ArrayList<>();
            optionsStart.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "winners", "Set number of winners, if not set defaults to 1")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей. По умолчанию 1"));

            optionsStart.add(new OptionData(STRING, "duration", "Set the duration. Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. UTC ±0")
                    .setName("duration")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "продолжительность")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить продолжительность. Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. UTC ±0"));

            optionsStart.add(new OptionData(ROLE, "mention", "Mentioning a specific @Role")
                    .setName("mention")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "упомянуть")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Упоминание определенной @Роли"));

            optionsStart.add(new OptionData(STRING, "role", "Is Giveaway only for a specific role? Specify the role in the previous selection")
                    .addChoice("yes", "yes")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway только для определенной роли? Установите роль в предыдущем выборе"));

            optionsStart.add(new OptionData(ATTACHMENT, "image", "Set Image for Giveaway")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            optionsStart.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min-participants")
                    .setMinValue(2)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            List<OptionData> predefined = new ArrayList<>();

            predefined.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway")
                    .setRequired(true));

            predefined.add(new OptionData(INTEGER, "winners", "Set number of winners")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей")
                    .setRequired(true));

            predefined.add(new OptionData(ROLE, "role", "Set @Role from which all participants are selected from")
                    .setName("role")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "роль")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установите @Роль, из которой будут выбраны все участники")
                    .setRequired(true));

            //endmessage
            List<OptionData> endMessageDate = new ArrayList<>();

            endMessageDate.add(new OptionData(STRING, "text", "Set text. Must contain @winner to properly parse")
                    .setName("text")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текст")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Задать текст. Должен содержать @winner для правильного разбора"));

            //giveaway-edit
            List<OptionData> giveawayEditData = new ArrayList<>();

            giveawayEditData.add(new OptionData(STRING, "duration", "Examples: 5s, 20m, 10h, 1d. Or: 2021.11.16 16:00. Only in this style and UTC ±0")
                    .setName("duration")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "продолжительность")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Примеры: 5s, 20m, 10h, 1d. Или: 2021.11.16 16:00. Только в этом стиле и UTC ±0"));

            giveawayEditData.add(new OptionData(INTEGER, "winners", "Set number of winners")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей."));

            giveawayEditData.add(new OptionData(STRING, "title", "Title for Giveaway")
                    .setName("title")
                    .setMaxLength(255)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "название")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Название для Giveaway"));

            giveawayEditData.add(new OptionData(ATTACHMENT, "image", "Set Image for Giveaway")
                    .setName("image")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "изображение")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить изображение для Giveaway"));

            giveawayEditData.add(new OptionData(INTEGER, "min-participants", "Delete Giveaway if the number of participants is less than this number")
                    .setName("min-participants")
                    .setMinValue(1)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "минимум-участников")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Удалить Giveaway если участников меньше этого числа"));

            giveawayEditData.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            List<OptionData> reroll = new ArrayList<>();
            reroll.add(new OptionData(STRING, "giveaway-id", "Giveaway ID or message ID with user mentions")
                    .setName("giveaway-id")
                    .setRequired(true)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID или message ID с упоминаниями пользователей"));

            reroll.add(new OptionData(INTEGER, "winners", "Set count winners.")
                    .setName("winners")
                    .setMinValue(1)
                    .setMaxValue(30)
                    .setNameLocalization(DiscordLocale.RUSSIAN, "победителей")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Установить количество победителей."));

            List<OptionData> botPermissions = new ArrayList<>();
            botPermissions.add(new OptionData(CHANNEL, "text-channel", "Check permissions of a specific channel")
                    .setName("text-channel")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "текстовой-канал")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений определенного канала"));

            List<OptionData> cancelData = new ArrayList<>();
            cancelData.add(new OptionData(STRING, "giveaway-id", "Giveaway ID")
                    .setName("giveaway-id")
                    .setNameLocalization(DiscordLocale.RUSSIAN, "id-розыгрыша")
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Giveaway ID"));

            /*
             * Команды
             */

            CommandData checkCommand = Commands.slash("check", "Check permissions of bot")
                    .addOptions(botPermissions)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Проверка разрешений бота");

            CommandData settingsCommand = Commands.slash("settings", "Change bot settings")
                    .addOptions(optionsSettings)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройки бота")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData startCommand = Commands.slash("start", "Create Giveaway")
                    .addOptions(optionsStart)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создание Giveaway");

            CommandData schedulingCommand = Commands.slash("scheduling", "Create scheduled Giveaway")
                    .addOptions(optionsScheduling)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Создание запланированного Giveaway");

            CommandData stopCommand = Commands.slash("stop", "Stop the Giveaway and announce winners")
                    .addOptions(optionsStop)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Остановить Giveaway и определить победителей");

            CommandData helpCommand = Commands.slash("help", "Bot commands")
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Команды бота");

            CommandData participantsCommand = Commands.slash("participants", "Get file with all participants")
                    .addOptions(participants)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Получить файл со всеми участниками");

            CommandData endMessage = Commands.slash("endmessage", "Set message announcing the winners")
                    .addOptions(endMessageDate)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Настройте сообщение с объявлением победителей, заменяя его на указанный текст.");

            CommandData rerollCommand = Commands.slash("reroll", "Reroll winners")
                    .addOptions(reroll)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Перевыбрать победителей");

            CommandData giveawayEdit = Commands.slash("edit", "Change Giveaway settings")
                    .addOptions(giveawayEditData)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Изменить настройки Giveaway")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData predefinedCommand = Commands.slash("predefined", "Gather participants and immediately hold a drawing for a specific @Role")
                    .addOptions(predefined)
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Собрать участников и сразу провести розыгрыш для определенной @Роли")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData listCommand = Commands.slash("list", "List of all active and scheduled Giveaways")
                    .setContexts(InteractionContextType.GUILD)
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Список всех активных и запланированных Giveaway")
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

            CommandData cancelCommand = Commands.slash("cancel", "Cancel Giveaway")
                    .addOptions(cancelData)
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                    .setDescriptionLocalization(DiscordLocale.RUSSIAN, "Отменить Giveaway");

            commands.addCommands(
                            listCommand,
                            checkCommand,
                            endMessage,
                            settingsCommand,
                            startCommand,
                            schedulingCommand,
                            stopCommand,
                            helpCommand,
                            participantsCommand,
                            rerollCommand,
                            giveawayEdit,
                            predefinedCommand,
                            cancelCommand)
                    .queue();

            List<Command> commandsList = jda.retrieveCommands().submit().get();

            for (Command command : commandsList) {
                String name = command.getName();
                long id = command.getIdLong();
                System.out.printf("%s [%s]%n", id, name);
                commandMap.put(name, id);
            }

            System.out.println("Готово");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Nullable
    public static Long getCommandId(String commandName) {
        return commandMap.get(commandName);
    }
}