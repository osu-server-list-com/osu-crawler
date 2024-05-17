package osu.serverlist.DiscordBot.helpers;

import com.google.inject.internal.Nullable;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class GenericEvent {

    public static void sendEditSendMessage(Event event, @Nullable String messageId, EmbedBuilder embed, ServerEndpoints endpoint, ItemComponent... components) {
         if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(embed.build())
                    .setActionRow(components)
                    .queue(message -> {
                        SlashCommandInteractionEvent event2 = (SlashCommandInteractionEvent) event;
                        String messageIdd = message.getId();

                        switch(endpoint) {
                            case LEADERBOARD:
                            Leaderboard.userOffsets.get(event2.getUser().getId()).messageId = messageIdd;
                                break;

                            case RECENT:
                            Recent.userOffsets.get(event2.getUser().getId()).messageId = messageIdd;
                                break;

                            default:
                                break;
                        }
                       
                    });
        } else if (event instanceof ButtonInteractionEvent) {
            ((ButtonInteractionEvent) event).editMessageEmbeds(embed.build())
                    .setActionRow(components).queue();
        }
    }
    
}
