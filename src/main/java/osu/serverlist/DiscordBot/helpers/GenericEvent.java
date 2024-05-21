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
         if (event instanceof SlashCommandInteractionEvent slashEvent) {
            slashEvent.getHook().sendMessageEmbeds(embed.build())
                    .setActionRow(components)
                    .queue(message -> {
                        String messageId = message.getId();

                        infos.get(slashEvent.getUser().getId()).messageId = messageId;
                    });
        } else if (event instanceof ButtonInteractionEvent buttonEvent) {
            buttonEvent.editMessageEmbeds(embed.build())
                    .setActionRow(components).queue();
        }
    }


    
}
