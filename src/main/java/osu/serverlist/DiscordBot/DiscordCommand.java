package osu.serverlist.DiscordBot;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface DiscordCommand {

    public void handleCommand(SlashCommandInteractionEvent event);
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event);

    public String getName();
}
