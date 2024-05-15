package osu.serverlist.DiscordBot.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import osu.serverlist.DiscordBot.DiscordCommand;
import osu.serverlist.DiscordBot.helpers.EndpointHelper;
import osu.serverlist.DiscordBot.helpers.LeaderboardHelper;
import osu.serverlist.DiscordBot.helpers.LeaderboardHelper.GotLeaderboard;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.ModeHelper.SortHelper;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class Leaderboard extends ListenerAdapter implements DiscordCommand {

    public static String[] servers = { "Loading..." };

    public static HashMap<String, ServerInformations> endpoints = new HashMap<>();
    public static Map<String, LeaderboardInformations> userOffsets = new HashMap<>();

    public class LeaderboardInformations {
        public String server;
        public String mode;
        public String sort;
        public String modeId;
        public String sortId;
        public String description;
        private String messageId;
        public int offset;
    }

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {

        String userId = event.getUser().getId();

        String server = event.getOption("server").getAsString().toLowerCase();
        String mode = event.getOption("mode").getAsString().toLowerCase();
        String sort = event.getOption("sort").getAsString();
        String modeId = ModeHelper.convertMode(mode);
        String sortId = SortHelper.convertSort(sort);

        event.deferReply().queue();

        if (modeId == null || sortId == null) {
            event.getHook().sendMessage("Invalid mode or sort").queue();
            return;
        }

        if (!endpoints.containsKey(server)) {
            EndpointHelper.adjustEndpoints(server, ServerEndpoints.LEADERBOARD, EndpointType.BANCHOPY);
        }

        if (!endpoints.containsKey(server)) {
            event.getHook().sendMessage("Server not found").queue();
            return;
        }

        LeaderboardInformations infosS = new LeaderboardInformations();

        infosS.server = server;
        infosS.mode = mode;
        infosS.sort = sort;
        infosS.modeId = modeId;
        infosS.sortId = sortId;
        infosS.offset = 0;
    

        userOffsets.put(userId, infosS);

        requestLeaderboard(infosS, event);

    }

    public void requestLeaderboard(LeaderboardInformations infos, Event event) {
        LeaderboardHelper leaderboardHelper = new LeaderboardHelper();

        GotLeaderboard gotLeaderboard = null;
        try {
            switch(endpoints.get(infos.server).getType()) {
                case "BANCHOPY":
                    gotLeaderboard = leaderboardHelper.getLeaderboardBanchoPy(infos.offset, infos);
                    break;
                default:
                    Flogger.instance.log(Prefix.ERROR, "Issue finding endpoint at requestLeaderboard()", 0);
                    return;
            }
        } catch (Exception e) {
            Flogger.instance.error(e);
            return;
        }
        
        Button nextPageButton;
        if(gotLeaderboard.size == 25) {
            nextPageButton = Button.success("next_page", "Next Page");
        }else {
            nextPageButton = Button.success("next_page", "Next Page").asDisabled();
        }

        Button prevPageButton;
        if(infos.offset == 0) {
            prevPageButton = Button.danger("prev_page", "Previous Page").asDisabled();
        }else {
            prevPageButton = Button.danger("prev_page", "Previous Page");
        }
        

    

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Leaderboard for " + infos.server + " - " + infos.mode.toUpperCase() + " - " + infos.sort + " (Page "
                + (infos.offset + 1) + ")");
        embed.setDescription(gotLeaderboard.leaderboard);
        embed.setColor(0x5755d9);
        embed.setFooter("Data from " + endpoints.get(infos.server).getName());
        embed.build();

        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(embed.build())
            .setActionRow(prevPageButton, nextPageButton)
            .queue(message -> {
                SlashCommandInteractionEvent event2 = (SlashCommandInteractionEvent) event;
                String messageId = message.getId();
               userOffsets.get(event2.getUser().getId()).messageId = messageId;
            });
        } else if (event instanceof ButtonInteractionEvent) {
            ((ButtonInteractionEvent) event).editMessageEmbeds(embed.build()).setActionRow(prevPageButton, nextPageButton).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
    
        if (event.getComponentId().equals("next_page")) {
            LeaderboardInformations infos = userOffsets.get(userId);
            if (infos != null && event.getMessage().getId().equals(infos.messageId)) {
                infos.offset += 1;
                userOffsets.put(userId, infos);
                requestLeaderboard(infos, event);
                scheduleOffsetRemoval(userId);
            } else {
                return; 
            }
        } else if (event.getComponentId().equals("prev_page")) {
            LeaderboardInformations infos = userOffsets.get(userId);
            if (infos != null) {
                if (infos.offset > 0) {
                    infos.offset -= 1;
                    userOffsets.put(userId, infos);
                    requestLeaderboard(infos, event);
                    scheduleOffsetRemoval(userId);
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void scheduleOffsetRemoval(String userId) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                userOffsets.remove(userId);
                timer.cancel();
            }
        }, 5 * 60 * 1500); 
    }

    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        if (event.getFocusedOption().getName().equals("server")) {
            List<Command.Choice> options = Stream.of(servers)
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
        } else if (event.getFocusedOption().getName().equals("sort")) {

            List<Command.Choice> options = Stream
                    .of(SortHelper.sortArray)
                    .filter(sort -> sort.toLowerCase()
                            .startsWith(event.getFocusedOption().getValue().toLowerCase()))
                    .map(sort -> new Command.Choice(sort, sort))
                    .collect(Collectors.toList());
            event.replyChoices(options).queue();
        }
    }

    @Override
    public String getName() {
        return "leaderboard";
    }

}
