package osu.serverlist.DiscordBot.base;

import net.dv8tion.jda.api.EmbedBuilder;

public class MessageBuilder {

    public static EmbedBuilder buildMessageError(String message) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Error");
        embed.setDescription(":x: " + message);
        embed.setColor(0xFF0000);
        return embed;
    }

    public static EmbedBuilder buildMessageSuccess(String message) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Success");
        embed.setDescription(":white_check_mark: " + message);
        embed.setColor(0x00FF00);
        return embed;
    }
    
}
