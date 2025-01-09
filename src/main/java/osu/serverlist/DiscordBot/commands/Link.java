package osu.serverlist.DiscordBot.commands;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import osu.serverlist.DiscordBot.base.DiscordCommand;
import osu.serverlist.DiscordBot.base.MessageBuilder;
import osu.serverlist.DiscordBot.helpers.LinkService;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.LinkService.LinkResponse;

public class Link implements DiscordCommand {

   
    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
        String server = event.getOption("server").getAsString().toLowerCase();
        String name = event.getOption("name").getAsString().toLowerCase();
        String mode = event.getOption("mode").getAsString().toLowerCase();

        event.deferReply().queue();

        LinkService linkService = new LinkService();

        boolean foundServer = Stream.of(Recent.servers).anyMatch(s -> s.equalsIgnoreCase(server));
        if(!foundServer) {
            event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError("Server not found").build()).queue();
            linkService.close();
            return;
        }

        boolean foundMode = Stream.of(ModeHelper.modeArray).anyMatch(m -> m.equalsIgnoreCase(mode));
        if(!foundMode) {
            event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError("Invalid mode").build()).queue();
            linkService.close();
            return;
        }

        if((server.equals("bancho") ||server.equals("ripple")) && Integer.parseInt(ModeHelper.convertMode(mode))>3) {
            event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError("Invalid mode for Bancho").build()).queue();
            linkService.close();
            return;
        }

        LinkResponse response = linkService.addLinkToUser(event, server, name, mode);
        switch (response) {
            case SUCCESS:
                String successMessage = "<@" + event.getUser().getId() + "> Your link for " + event.getGuild().getName() + " has been set to: " + server + " (" + mode + ") **" + name+ "**\nThis will be active for **/best**, **/recent**, and **/profile** when you use no parameters";
                event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageSuccess(successMessage).build()).queue();
                break;
            case OVERWRITTEN:
                String overwriteMessage = "Your old link for " + event.getGuild().getName() + " has been overwritten \nwith: " + server + " (" + mode + ") **" + name+ "**";
                event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError(overwriteMessage).build()).queue();
                break;
            case ERROR:
                event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError("Internal error when linking occured").build()).queue();
                break;

            default:
                break;
        }
        linkService.close();
    }

    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
       if (event.getFocusedOption().getName().equals("server")) {
            List<Command.Choice> options = Stream.of(Recent.servers)
                    .filter(server -> server.toLowerCase()
                            .startsWith(event.getFocusedOption().getValue().toLowerCase()))
                    .map(server -> new Command.Choice(server, server))
                    .collect(Collectors.toList());
            event.replyChoices(options).queue();
        } else if (event.getFocusedOption().getName().equals("mode")) {

            List<Command.Choice> options = Stream
                    .of(ModeHelper.modeArray)
                    .filter(mode -> mode.toLowerCase()
                            .startsWith(event.getFocusedOption().getValue().toLowerCase()))
                    .map(mode -> new Command.Choice(mode, mode))
                    .collect(Collectors.toList());
            event.replyChoices(options).queue();
        }
    }

    @Override
    public String getName() {
        return "link";
    }
    
}
