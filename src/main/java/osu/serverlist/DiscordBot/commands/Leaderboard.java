package osu.serverlist.DiscordBot.commands;

import java.util.HashMap;
import java.util.List;
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
import osu.serverlist.DiscordBot.base.DiscordCommand;
import osu.serverlist.DiscordBot.helpers.EndpointHelper;
import osu.serverlist.DiscordBot.helpers.GenericEvent;
import osu.serverlist.DiscordBot.helpers.InformationBase;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.ModeHelper.SortHelper;
import osu.serverlist.DiscordBot.helpers.commands.LeaderboardHelper;
import osu.serverlist.DiscordBot.helpers.commands.LeaderboardHelper.GotLeaderboard;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class Leaderboard extends ListenerAdapter implements DiscordCommand {

    public static String[] servers = { "Loading..." };

    public static HashMap<String, ServerInformations> endpoints = new HashMap<>();
    public static HashMap<String, InformationBase> userOffsets = new HashMap<>();

    public class LeaderboardInformations extends InformationBase {
        public String mode;
        public String sort;
        public String modeId;
        public String sortId;
        public String description;
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

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Leaderboard for " + endpoints.get(infos.server).getName() + " | " + infos.mode.toUpperCase() + " | " + infos.sort.toUpperCase() + " | (Page "
                + (infos.offset + 1) + ")");
        embed.setDescription(gotLeaderboard.leaderboard);
        embed.setColor(0x5755d9);
        embed.setFooter("Data from " + endpoints.get(infos.server).getName());
        embed.build();

        GenericEvent.sendEditSendMessage(event, userOffsets, embed, ServerEndpoints.LEADERBOARD, EndpointHelper.getPageButtons(infos.offset == 0, gotLeaderboard.size == 25, "ld"));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
    
        if (event.getComponentId().equals("next_page_ld")) {
            InformationBase infos = userOffsets.get(userId);
            if (infos != null && event.getMessage().getId().equals(infos.messageId)) {
                infos.offset += 1;
                userOffsets.put(userId, infos);
                requestLeaderboard((LeaderboardInformations) infos, event);
                scheduleOffsetRemoval(userId);
            } 
        } else if (event.getComponentId().equals("prev_page_ld")) {
            InformationBase infos = userOffsets.get(userId);
            if (infos != null && event.getMessage().getId().equals(infos.messageId)) {
                if (infos.offset > 0) {
                    infos.offset -= 1;
                    userOffsets.put(userId, infos);
                    requestLeaderboard((LeaderboardInformations) infos, event);
                    scheduleOffsetRemoval(userId);
                } 
            }
        }
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


}
