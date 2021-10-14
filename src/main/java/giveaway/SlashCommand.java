package giveaway;

import db.DataBase;
import jsonparser.JSONParsers;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
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
                    TextChannel textChannel = null;
                    String title = null;
                    String count = null;
                    String time = null;

                    for (int i = 0; i < event.getOptions().size(); i++) {

                        if (title == null
                                && !event.getOptions().get(i).getAsString().matches("[0-9]{1,2}[mмhчdд]")
                                && !event.getOptions().get(i).getAsString().matches("\\d{18}")
                                && !event.getOptions().get(i).getAsString().matches("[0-9]{1,2}")
                                && event.getOptions().get(i).getAsString().matches(".{0,255}")) {
                            title = event.getOptions().get(i).getAsString();
                        }

                        if (event.getOptions().get(i).getAsString().matches("[0-9]{1,2}")) {
                            count = event.getOptions().get(i).getAsString();
                        }

                        if (event.getOptions().get(i).getAsString().matches("[0-9]{1,2}[mмhчdд]")) {
                            time = event.getOptions().get(i).getAsString();
                        }

                        if (event.getOptions().get(i).getAsString().matches("\\d{18}")) {
                            textChannel = event.getOptions().get(i).getAsGuildChannel().getGuild()
                                    .getTextChannelById(event.getOptions().get(i).getAsGuildChannel().getId());
                        }

                    }

                    GiveawayRegistry.getInstance().setGift(
                            event.getGuild().getIdLong(),
                            new Gift(event.getGuild().getIdLong(),
                                    textChannel == null ? event.getTextChannel().getIdLong() : textChannel.getIdLong()));

                    if (!event.getGuild().getSelfMember()
                            .hasPermission(textChannel == null ? event.getTextChannel() : textChannel, Permission.MESSAGE_WRITE) ||
                            !event.getGuild().getSelfMember()
                                    .hasPermission(textChannel == null ? event.getTextChannel() : textChannel, Permission.MESSAGE_EMBED_LINKS)) {
                        return;
                    }

                    GiveawayRegistry.getInstance()
                            .getActiveGiveaways().get(event.getGuild().getIdLong()).startGift(event,
                                    event.getGuild(),
                                    textChannel == null ? event.getTextChannel() : textChannel,
                                    title,
                                    count,
                                    time);

                } catch (Exception e) {
                    e.printStackTrace();
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