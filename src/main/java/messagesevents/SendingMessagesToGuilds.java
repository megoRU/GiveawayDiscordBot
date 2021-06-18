package messagesevents;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import startbot.BotStart;
import java.util.ArrayList;
import java.util.List;

public class SendingMessagesToGuilds extends ListenerAdapter {

    private final static List<Guilds> guildsList = new ArrayList<>();
    protected final String MAIN_GUILD_ID = "772388035944906793";
    protected final String MAIN_USER_ID = "250699265389625347"; //250699265389625347
    protected final String MSG = "!msg";

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) return;

        String message = event.getMessage().getContentRaw().trim();
        String[] messages = message.split(" ", 2);
        String idGuild = event.getGuild().getId();
        String idUser = event.getAuthor().getId();

        if (messages[0].equals(MSG) && !idGuild.equals(MAIN_GUILD_ID) && !idUser.equals(MAIN_USER_ID)) {
            return;
        }

        if (messages[0].equals(MSG) && idGuild.equals(MAIN_GUILD_ID) && idUser.equals(MAIN_USER_ID)) {
            BotStart.getJda().getGuilds().forEach(guild -> {
                guildsList.add(new Guilds(guild, guild.getTextChannels()));
            });


            for (int i = 0; i < guildsList.size(); i++) {
                System.out.println("Guild id: " + guildsList.get(i).getGuild().getId());
                System.out.println("His chats: ");
                for (int j = 0; j < guildsList.get(i).getChannelList().size(); j++) {
                    System.out.println("    " + guildsList.get(i).getChannelList().get(j).getName());
                }
            }

            for (Guilds list : guildsList) {

                if (list.getGuild().getDefaultChannel() != null)
                if (BotStart.getJda().getGuildById(list.getGuild().getId())
                        .getSelfMember()
                        .hasPermission(list.getGuild().getDefaultChannel(), Permission.MESSAGE_WRITE))

                BotStart.getJda().getGuildById(list.getGuild().getId())
                        .getDefaultChannel()
                        .sendMessage(messages[1])
                        .queue();
            }
            guildsList.clear();
        }
    }
}