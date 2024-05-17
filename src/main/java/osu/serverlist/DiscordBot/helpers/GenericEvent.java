package osu.serverlist.DiscordBot.helpers;

import java.util.HashMap;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class GenericEvent {

    public static void sendEditSendMessage(Event event, HashMap<String, InformationBase> infos, EmbedBuilder embed, ServerEndpoints endpoint, ItemComponent... components) {
         if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(embed.build())
                    .setActionRow(components)
                    .queue(message -> {
                        SlashCommandInteractionEvent event2 = (SlashCommandInteractionEvent) event;
                        String messageIdd = message.getId();

                        infos.get(event2.getUser().getId()).messageId = messageIdd;
                    });
        } else if (event instanceof ButtonInteractionEvent) {
            ((ButtonInteractionEvent) event).editMessageEmbeds(embed.build())
                    .setActionRow(components).queue();
        }
    }
    
}
