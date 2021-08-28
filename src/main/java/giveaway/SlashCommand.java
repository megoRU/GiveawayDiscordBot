package giveaway;

import db.DataBase;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;

public class SlashCommand extends ListenerAdapter {

    private final JSONParsers jsonParsers = new JSONParsers();

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (event.getUser().isBot()) return;

        if (event.getGuild() == null) return;

        if (event.getMember() == null) return;

        if (event.getName().equals("giveaway-start")) {
            if (GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                event.reply(jsonParsers.getLocale("message_gift_Need_Stop_Giveaway", event.getGuild().getId())).queue();
            } else {
                try {
                    GiveawayRegistry.getInstance().setGift(event.getGuild().getIdLong(), new Gift(event.getGuild().getIdLong(), event.getChannel().getIdLong()));

                    if (event.getOptions().size() == 3
                            && event.getOptions().get(0).getAsString().matches(".{0,255}")
                            && event.getOptions().get(1).getAsString().matches("[0-9]{1,2}")
                            && event.getOptions().get(2).getAsString().matches("[0-9]{1,2}[mмhчdд]")) {

                        GiveawayRegistry.getInstance()
                                .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                        event.getGuild(),
                                        event.getTextChannel(),
                                        event.getOptions().get(0).getAsString(),
                                        event.getOptions().get(1).getAsString(),
                                        event.getOptions().get(2).getAsString());

                    } else if (event.getOptions().size() == 2
                            && event.getOptions().get(0).getAsString().matches(".{0,255}")
                            && event.getOptions().get(1).getAsString().matches("[0-9]{1,2}")) {

                        GiveawayRegistry.getInstance()
                                .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                        event.getGuild(),
                                        event.getTextChannel(),
                                        event.getOptions().get(0).getAsString(),
                                        event.getOptions().get(1).getAsString(),
                                        null);

                    } else if (event.getOptions().size() == 2
                            && event.getOptions().get(0).getAsString().matches(".{0,255}")
                            && event.getOptions().get(1).getAsString().matches("[0-9]{1,2}[mмhчdд]")) {

                        GiveawayRegistry.getInstance()
                                .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                        event.getGuild(),
                                        event.getTextChannel(),
                                        event.getOptions().get(0).getAsString(),
                                        null,
                                        event.getOptions().get(1).getAsString());

                    } else if (event.getOptions().size() == 2
                            && event.getOptions().get(0).getAsString().matches("[0-9]{1,2}")
                            && event.getOptions().get(1).getAsString().matches("[0-9]{1,2}[mмhчdд]")) {

                        GiveawayRegistry.getInstance()
                                .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                        event.getGuild(),
                                        event.getTextChannel(),
                                        null,
                                        event.getOptions().get(0).getAsString(),
                                        event.getOptions().get(1).getAsString());

                    } else if (event.getOptions().size() == 1 && event.getOptions().get(0).getAsString().matches("[0-9]{1,2}")) {

                        GiveawayRegistry.getInstance()
                                .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                        event.getGuild(),
                                        event.getTextChannel(),
                                        null,
                                        event.getOptions().get(0).getAsString(),
                                        null);

                    } else if (event.getOptions().size() == 1 && event.getOptions().get(0).getAsString().matches("[0-9]{1,2}[mмhчdд]")) {

                        GiveawayRegistry.getInstance()
                                .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                        event.getGuild(),
                                        event.getTextChannel(),
                                        null,
                                        null,
                                        event.getOptions().get(0).getAsString());

                    } else if (event.getOptions().size() == 1 && event.getOptions().get(0).getAsString().matches(".{0,255}")) {

                        GiveawayRegistry.getInstance()
                                .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                        event.getGuild(),
                                        event.getTextChannel(),
                                        event.getOptions().get(0).getAsString(),
                                        null,
                                        null);

                    } else if (event.getOptions().isEmpty()) {

                        GiveawayRegistry.getInstance()
                                .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                        event.getGuild(),
                                        event.getTextChannel(),
                                        null,
                                        null,
                                        null);
                    }

                } catch (Exception e) {
                    GiveawayRegistry.getInstance().removeGift(event.getGuild().getIdLong());
                    event.reply(jsonParsers.getLocale("slash_Errors", event.getGuild().getId())).queue();
                }
            }

            return;
        }

        if (event.getName().equals("giveaway-stop")) {
            if (!GiveawayRegistry.getInstance().hasGift(event.getGuild().getIdLong())) {
                event.reply(jsonParsers.getLocale("slash_Stop_No_Has", event.getGuild().getId())).queue();
                return;
            }

            if (!event.getMember().hasPermission(event.getTextChannel(), Permission.ADMINISTRATOR)
                    && !event.getMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                event.reply(jsonParsers.getLocale("message_gift_Not_Admin", event.getGuild().getId())).queue();
                return;
            }

            if (event.getOptions().isEmpty()) {
                event.reply(jsonParsers.getLocale("slash_Stop", event.getGuild().getId())).queue();
                GiveawayRegistry.getInstance()
                        .getActiveGiveaways().get(event.getGuild().getIdLong())
                        .stopGift(event.getGuild().getIdLong(),
                                GiveawayRegistry.getInstance().getCountWinners().get(event.getGuild().getIdLong()) == null ? 1 :
                                        Integer.parseInt(GiveawayRegistry.getInstance().getCountWinners().get(event.getGuild().getIdLong()))
                        );
                return;
            }

            if (!event.getOptions().get(0).getAsString().matches("[0-9]{1,2}")) {
                event.reply(jsonParsers.getLocale("slash_Errors", event.getGuild().getId())).queue();
                return;
            }

            event.reply(jsonParsers.getLocale("slash_Stop", event.getGuild().getId())).queue();
            GiveawayRegistry.getInstance()
                    .getActiveGiveaways().get(event.getGuild().getIdLong())
                    .stopGift(event.getGuild().getIdLong(), Integer.parseInt(event.getOptions().get(0).getAsString()));
            return;
        }

        //0 - bot
        if (event.getName().equals("language")) {

            BotStart.getMapLanguages().put(event.getGuild().getId(), event.getOptions().get(0).getAsString());

            event.reply(jsonParsers.getLocale("button_Language", event.getGuild().getId())
                    .replaceAll("\\{0}", event.getOptions().get(0).getAsString())).queue();

            DataBase.getInstance().addLangToDB(event.getGuild().getId(), event.getOptions().get(0).getAsString());
        }
    }
}