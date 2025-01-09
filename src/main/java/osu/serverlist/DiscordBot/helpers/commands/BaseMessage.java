package osu.serverlist.DiscordBot.helpers.commands;

import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import osu.serverlist.DiscordBot.base.MessageBuilder;

public class BaseMessage {

    public enum Messages {
        INVALID_SERVER("Server not found"),
        INVALID_MODE("Invalid mode"),
        INVALID_MODE_SERVER("Invalid mode for server %server%"),
        USER_SCORES_NOT_FOUND("No scores found for user %name% on %server%");

        public final String value;

        Messages(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class Placeholder {
        public String placeholder;
        public String value;

        public Placeholder(String placeholder, String value) {
            this.placeholder = placeholder;
            this.value = value;
        }
    }

    public static void sendMessageOnSlash(Event event, Messages message, Placeholder... placeholders) {
        if (!(event instanceof SlashCommandInteractionEvent))
            return;

        String messageStr = message.getValue();

        for (Placeholder placeholder : placeholders) {
            messageStr = messageStr.replace(placeholder.placeholder , placeholder.value);
        }

        ((SlashCommandInteractionEvent) event).getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError(messageStr).build()).queue();

    }

}
