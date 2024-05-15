package osu.serverlist.DiscordBot;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.DiscordBot.commands.Stats;

public class DiscordCommandHandler extends ListenerAdapter {

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("stats") && event.getFocusedOption().getName().equals("server")) {
            new Stats().handleAutoComplete(event);
            return;
        }

        if (event.getName().equals("profile")) {
            new Profile().handleAutoComplete(event);
            return;
        }

        if (event.getName().equals("leaderboard")) {
            new Leaderboard().handleAutoComplete(event);
            return;
        }

        else if(event.getName().equals("recent")) { 
            new Recent().handleAutoComplete(event);
        }
        


    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Flogger.instance.log(Prefix.API, "Slash Command: " + event.getName(), 0);
        if (event.getName().equals("stats")) {
            new Stats().handleCommand(event);
        } else if(event.getName().equals("profile")) { 
            new Profile().handleCommand(event);
        }else if(event.getName().equals("leaderboard")) { 
            new Leaderboard().handleCommand(event);
        }else if(event.getName().equals("recent")) { 
            new Recent().handleCommand(event);
        } else {
            String inviteUrl = event.getJDA().getInviteUrl();
            event.reply("Here's the invite link: " + inviteUrl).queue();
        }
    }


}
