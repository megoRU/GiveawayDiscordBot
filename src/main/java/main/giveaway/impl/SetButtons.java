package main.giveaway.impl;

import main.config.BotStartConfig;
import main.giveaway.ReactionsButton;
import main.jsonparser.JSONParsers;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.LinkedList;
import java.util.List;

public interface SetButtons {

    JSONParsers jsonParsers = new JSONParsers();

    static List<Button> getListButtons(String guildIdLong) {
        List<Button> buttons = new LinkedList<>();

        if (BotStartConfig.getMapLanguages().get(guildIdLong) != null) {

            if (BotStartConfig.getMapLanguages().get(guildIdLong).equals("rus")) {
                buttons.add(Button.success(guildIdLong + ":" + ReactionsButton.PRESENT,
                        jsonParsers.getLocale("gift_Press_Me_Button", guildIdLong) + "⠀ "));
            } else {
                buttons.add(Button.success(guildIdLong + ":" + ReactionsButton.PRESENT,
                        jsonParsers.getLocale("gift_Press_Me_Button", guildIdLong) + "⠀⠀⠀⠀⠀⠀⠀"));
            }
        } else {
            buttons.add(Button.success(guildIdLong + ":" + ReactionsButton.PRESENT,
                    jsonParsers.getLocale("gift_Press_Me_Button", guildIdLong) + "⠀⠀⠀⠀⠀⠀⠀"));
        }

        return buttons;
    }
}
