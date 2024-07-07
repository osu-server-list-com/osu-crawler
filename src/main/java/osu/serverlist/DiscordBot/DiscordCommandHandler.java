package osu.serverlist.DiscordBot;

import java.util.List;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import osu.serverlist.DiscordBot.commands.Best;
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

        else if(event.getName().equals("best")) { 
            new Best().handleAutoComplete(event);
        }
        


    }

    public void onMessageReceived(MessageReceivedEvent event) {
        Flogger.instance.log(Prefix.API, "Message received from " + event.getAuthor().getAsTag(), 0);
        
        if (event.getMessage().getAttachments().size() > 0) {
            List<Message.Attachment> attachments = event.getMessage().getAttachments();
            Flogger.instance.log(Prefix.API, "Got attachment", 0);
            
            for (Message.Attachment attachment : attachments) {
                Flogger.instance.log(Prefix.API, "Processing attachment: " + attachment.getFileName(), 0);
                
                if (attachment.isVideo() || attachment.isImage()) {
                    Flogger.instance.log(Prefix.API, "Skipping video or image: " + attachment.getFileName(), 0);
                    continue;
                }

                if (!event.getAuthor().getId().equals("307257319266320394")) {
                    Flogger.instance.log(Prefix.API, "Permission denied for " + event.getAuthor().getAsTag(), 0);
                    return;
                }

                String fileUrl = attachment.getUrl();
                String fileName = attachment.getFileName();
                Flogger.instance.log(Prefix.API, "File: " + fileName + " URL: " + fileUrl, 0);

                if (fileName.endsWith(".osr")) {
                    Flogger.instance.log(Prefix.INFO, "OSR Received " + fileName, 0);
                }
            }
        } else {
            Flogger.instance.log(Prefix.API, "No attachments in message", 0);
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
        }else if(event.getName().equals("best")) { 
            new Best().handleCommand(event);
        } else {
            String inviteUrl = event.getJDA().getInviteUrl();
            event.reply("Here's the invite link: " + inviteUrl).queue();
        }
    }


}
