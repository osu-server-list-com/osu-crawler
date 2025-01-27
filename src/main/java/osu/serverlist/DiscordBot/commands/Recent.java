package osu.serverlist.DiscordBot.commands;

import java.util.ArrayList;
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
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import osu.serverlist.DiscordBot.base.DiscordCommand;
import osu.serverlist.DiscordBot.helpers.EndpointHelper;
import osu.serverlist.DiscordBot.helpers.GenericEvent;
import osu.serverlist.DiscordBot.helpers.InformationBase;
import osu.serverlist.DiscordBot.helpers.LinkService;
import osu.serverlist.DiscordBot.helpers.LinkService.LinkResponseObject;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.OsuConverter;
import osu.serverlist.DiscordBot.helpers.commands.BaseMessage;
import osu.serverlist.DiscordBot.helpers.commands.RecentHelper;
import osu.serverlist.DiscordBot.helpers.commands.BaseMessage.Messages;
import osu.serverlist.DiscordBot.helpers.commands.BaseMessage.Placeholder;
import osu.serverlist.DiscordBot.helpers.commands.RecentHelper.GotRecent;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class Recent extends ListenerAdapter implements DiscordCommand {

    public static String[] servers = { "Loading..." };

    public static HashMap<String, ServerInformations> endpoints = new HashMap<>();

    public static HashMap<String, InformationBase> userOffsets = new HashMap<>();

    public class RecentInformations extends InformationBase {
        public String mode;
        public String modeId;
        public String name;
    }

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        event.deferReply().queue();
        LinkService service = new LinkService();
        LinkResponseObject response = service.getLink(event);

        String server = response.getServer();
        String name = response.getName();
        String mode = response.getMode();

        service.close();
        String modeId = ModeHelper.convertMode(mode);

        event.deferReply().queue();

        if (modeId == null) {
            BaseMessage.sendMessageOnSlash(event, Messages.INVALID_MODE);
            return;
        }

        if (!endpoints.containsKey(server)) {
            EndpointHelper.adjustEndpoints(server, ServerEndpoints.RECENT, EndpointType.BANCHOPY,
                    EndpointType.RIPPLEAPIV1);
        }

        if (!endpoints.containsKey(server)) {
            BaseMessage.sendMessageOnSlash(event, Messages.INVALID_SERVER);
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
            switch (endpoints.get(infos.server).getType()) {
                case "BANCHOPY":
                    gotRecent = recentHelper.requestRecentBanchoPy(infos);
                    break;
                case "RIPPLEAPIV1":
                    gotRecent = recentHelper.requestRecentRippleAPIV1(infos);
                    break;
                case "BANCHO":
                    if(Integer.parseInt(infos.modeId) > 3) {
                        BaseMessage.sendMessageOnSlash(event, Messages.INVALID_MODE_SERVER,
                                new Placeholder("%server%", infos.server));
                        return;
                    }

                    gotRecent = recentHelper.requestRecentBancho(infos);
                    break;
                default:
                    Flogger.instance.log(Prefix.ERROR, "Issue finding endpoint at requestRecent()", 0);
                    return;
            }

            if (gotRecent == null) {
                BaseMessage.sendMessageOnSlash(event, Messages.INVALID_MODE_SERVER,
                        new Placeholder("%server%", infos.server));
                return;
            }

        } catch (Exception e) {
            BaseMessage.sendMessageOnSlash(event, Messages.USER_SCORES_NOT_FOUND, new Placeholder("%name%", infos.name),
                    new Placeholder("%server%", infos.server));
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        String nameW = infos.name.substring(0, 1).toUpperCase() + infos.name.substring(1);

        embed.setTitle("Recent plays on " + Recent.endpoints.get(infos.server).getName() + " for " + nameW);
        embed.setDescription(recentHelper.convertDescription(gotRecent, nameW, infos));

        embed.addField("Score", String.valueOf(gotRecent.score), true);
        if (gotRecent.pp != 0.0)
            embed.addField("Performance Points (PP)", String.valueOf(gotRecent.pp), true);

        if (gotRecent.acc != 0.0)
            embed.addField("Accuracy", String.format("%.2f", gotRecent.acc) + "%", true);
        String[] mods = OsuConverter.convertMods(Integer.parseInt(String.valueOf(gotRecent.mods)));
        if (mods.length == 0) {
            embed.addField("Mods", "-", true);
        } else {
            embed.addField("Mods", "+" + String.join("", mods), true);
        }

        if (gotRecent.playtime != null)
            embed.addField("Submitted", OsuConverter.convertToDiscordTimestamp(gotRecent.playtime), true);

        embed.addField("Difficulty", String.format("%.2f*", gotRecent.diff), true);

        embed.addField("AR", String.valueOf(gotRecent.ar), true);

        if (gotRecent.bpm != 0)
            embed.addField("BPM", String.valueOf(gotRecent.bpm), true);

        embed.addField("OD", String.valueOf(gotRecent.od), true);
      
        embed.setImage("https://assets.ppy.sh/beatmaps/" + gotRecent.setId + "/covers/cover.jpg");

        embed.setColor(0x5755d9);
        embed.setFooter("Data from " + Recent.endpoints.get(infos.server).getName());

        ItemComponent[] pageButtons = EndpointHelper.getPageButtons(infos.offset == 0, gotRecent.size == (infos.offset + 1), "rec");
        List<ItemComponent> urlButtons = new ArrayList<>();

        if(endpoints.get(infos.server).getType().equals("BANCHO")) {
            urlButtons.add(Button.link("https://osu.ppy.sh/beatmapsets/" + gotRecent.setId + "#osu/" + gotRecent.mapId, "View Beatmap!"));
        }else {
            urlButtons.add(Button.link(Recent.endpoints.get(infos.server).getUrl() + "/scores/" + gotRecent.score, "View Score"));
            urlButtons.add(Button.link("https://osu.direct/beatmapsets/" + gotRecent.setId + "/"
            + gotRecent.mapId, "View Beatmap"));
        }

        // Combine pageButtons and urlButtons
        ItemComponent[] buttons = new ItemComponent[pageButtons.length + urlButtons.size()];
        System.arraycopy(pageButtons, 0, buttons, 0, pageButtons.length);
        for (int i = 0; i < urlButtons.size(); i++) {
            buttons[pageButtons.length + i] = urlButtons.get(i);
        }


        GenericEvent.sendEditSendMessage(event, userOffsets, embed, ServerEndpoints.RECENT,
                buttons);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();

        if (event.getComponentId().equals("next_page_rec")) {

            InformationBase infos = userOffsets.get(userId);
            if (infos != null && event.getMessage().getId().equals(infos.messageId)) {
                infos.offset += 1;
                userOffsets.put(userId, infos);
                requestRecent((RecentInformations) infos, event);
                scheduleOffsetRemoval(userId);
            }

        } else if (event.getComponentId().equals("prev_page_rec")) {

            InformationBase infos = userOffsets.get(userId);
            if (infos != null && event.getMessage().getId().equals(infos.messageId)) {
                if (infos.offset > 0) {
                    infos.offset -= 1;
                    userOffsets.put(userId, infos);
                    requestRecent((RecentInformations) infos, event);
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
        }
    }

    @Override
    public String getName() {
        return "recent";
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
