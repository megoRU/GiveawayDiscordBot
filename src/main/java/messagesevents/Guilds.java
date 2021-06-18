package messagesevents;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;

public class Guilds {

    private final Guild guild;
    private final List<TextChannel> channelList;

    public Guilds(Guild guild, List<TextChannel> channelList) {
        this.guild = guild;
        this.channelList = channelList;
    }

    public Guild getGuild() {
        return guild;
    }

    public List<TextChannel> getChannelList() {
        return channelList;
    }
}
