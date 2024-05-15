package osu.serverlist.DiscordBot.commands;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import commons.marcandreher.Commons.Flogger;
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
import osu.serverlist.DiscordBot.helpers.GradeConverter;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.RecentHelper;
import osu.serverlist.DiscordBot.helpers.RecentHelper.GotRecent;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class Recent extends ListenerAdapter implements DiscordCommand {

    public static String[] servers = { "Loading..." };

    public static HashMap<String, ServerInformations> endpoints = new HashMap<>();

    public static HashMap<String, RecentInformations> userOffsets = new HashMap<>();

    public class RecentInformations {
        public String server;
        public String mode;
        public String modeId;
        public String messageId;
        public String name;
        public int offset;
    }

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        String server = event.getOption("server").getAsString().toLowerCase();
        String mode = event.getOption("mode").getAsString().toLowerCase();
        String name = event.getOption("name").getAsString().toLowerCase();
        String modeId = ModeHelper.convertMode(mode);

        event.deferReply().queue();

        if (modeId == null) {
            event.getHook().sendMessage("Invalid mode").queue();
            return;
        }

        if (!endpoints.containsKey(server)) {
            EndpointHelper.adjustEndpoints(server, ServerEndpoints.RECENT, EndpointType.BANCHOPY);
        }

        if (!endpoints.containsKey(server)) {
            event.getHook().sendMessage("Server not found").queue();
            return;
        }

        RecentInformations recentInformations = new RecentInformations();
        recentInformations.server = server;
        recentInformations.mode = mode;
        recentInformations.modeId = modeId;
        recentInformations.offset = 0;
        recentInformations.name = name;

        userOffsets.put(userId, recentInformations);
        requestRecent(recentInformations, event);
    }

    public void requestRecent(RecentInformations infos, Event event) {
        RecentHelper recentHelper = new RecentHelper();
        GotRecent gotRecent = null;
        try {
            gotRecent = recentHelper.requestRecentBanchoPy(infos);
        } catch (Exception e) {
            Flogger.instance.error(e);

            if(event instanceof SlashCommandInteractionEvent) {
                ((SlashCommandInteractionEvent) event).getHook().sendMessage("User not found").queue();
            }
            return;
        }

        Button nextPageButton;
        if(gotRecent.size == (infos.offset - 1)) {
            nextPageButton = Button.success("next_page_rec", "Next Page").asDisabled();
        }else {
            nextPageButton = Button.success("next_page_rec", "Next Page");
        }

        Button prevPageButton;
        if (infos.offset == 0) {
            prevPageButton = Button.danger("prev_page_rec", "Previous Page").asDisabled();
        } else {
            prevPageButton = Button.danger("prev_page_rec", "Previous Page");
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Recent plays on " + Recent.endpoints.get(infos.server).getName() + " for " + infos.name);
        embed.setDescription(GradeConverter.convertStatus(String.valueOf(gotRecent.status)) + " ▪ " + GradeConverter.convertGrade(gotRecent.grade) + " ▪ [" + infos.name + "](" + Recent.endpoints.get(infos.server).getUrl() + "/u/" + infos.name + ") on \n" + 
        "[" + gotRecent.mapArtist + " | " +  gotRecent.mapName + "](" + endpoints.get(infos.server).getUrl() + "/b/"+ gotRecent.mapId + ")\n" + 
        "Map by " + gotRecent.creator) ;
    
        embed.addField("Score ID", String.valueOf(gotRecent.scoreId), true);
        embed.addField("Score", String.valueOf(gotRecent.score), true);
        embed.addField("Performance Points (PP)", String.valueOf(gotRecent.pp), true);
        embed.addField("Accuracy", String.valueOf(gotRecent.acc) + "%", true);

        embed.addField("Mods", String.valueOf(gotRecent.mods), true);
        embed.addField("Submitted", convertToDiscordTimestamp(gotRecent.playtime), true);

        embed.addField("Difficulty", String.valueOf(gotRecent.diff) + "*", true);

        embed.addField("Approach Rate (AR)", String.valueOf(gotRecent.ar), true);
        embed.addField("Beats per Minute (BPM)", String.valueOf(gotRecent.bpm), true);
        embed.addField("Overall Difficulty (OD)", String.valueOf(gotRecent.od), true);
        embed.addField("", "[View Score](" + Recent.endpoints.get(infos.server).getUrl() + "/score/" + gotRecent.score + ") [osu.direct](https://osu.direct/beatmapsets/" + gotRecent.setId + "/" + gotRecent.mapId + ")", true);
        embed.setImage("https://assets.ppy.sh/beatmaps/" + gotRecent.setId + "/covers/cover.jpg");

        embed.setColor(0x5755d9);
        embed.setFooter("Data from " + Recent.endpoints.get(infos.server).getName());

        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(embed.build())
                    .setActionRow(prevPageButton, nextPageButton)
                    .queue(message -> {
                        SlashCommandInteractionEvent event2 = (SlashCommandInteractionEvent) event;
                        String messageId = message.getId();
                        userOffsets.get(event2.getUser().getId()).messageId = messageId;
                    });
        } else if (event instanceof ButtonInteractionEvent) {
            ((ButtonInteractionEvent) event).editMessageEmbeds(embed.build())
                    .setActionRow(prevPageButton, nextPageButton).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();

        if (event.getComponentId().equals("next_page_rec")) {
            RecentInformations infos = userOffsets.get(userId);
            if (infos != null && event.getMessage().getId().equals(infos.messageId)) {
                infos.offset += 1;
                userOffsets.put(userId, infos);
                requestRecent(infos, event);
                scheduleOffsetRemoval(userId);
            } else {
                return;
            }
        } else if (event.getComponentId().equals("prev_page_rec")) {
            RecentInformations infos = userOffsets.get(userId);
            if (infos != null) {
                if (infos.offset > 0) {
                    infos.offset -= 1;
                    userOffsets.put(userId, infos);
                    requestRecent(infos, event);
                    scheduleOffsetRemoval(userId);
                } else {
                    return;
                }
            } else {
                return;
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
        }
    }

    @Override
    public String getName() {
        return "recent";
    }

    public String convertToDiscordTimestamp(String timestamp) {
        Instant instant = Instant.parse(timestamp);
        long epochSeconds = instant.getEpochSecond();
        return "<t:" + epochSeconds + ":R>";
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
