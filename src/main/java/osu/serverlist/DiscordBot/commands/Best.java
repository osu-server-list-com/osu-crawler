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
import osu.serverlist.DiscordBot.helpers.commands.BestHelper;
import osu.serverlist.DiscordBot.helpers.commands.BaseMessage.Messages;
import osu.serverlist.DiscordBot.helpers.commands.BaseMessage.Placeholder;
import osu.serverlist.DiscordBot.helpers.commands.BestHelper.GotBest;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class Best extends ListenerAdapter implements DiscordCommand {

    public static String[] servers = { "Loading..." };

    public static HashMap<String, ServerInformations> endpoints = new HashMap<>();

    public static HashMap<String, InformationBase> userOffsets = new HashMap<>();

    public class BestInformations extends InformationBase {
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
            EndpointHelper.adjustEndpoints(server, ServerEndpoints.BEST, EndpointType.BANCHOPY,
                    EndpointType.RIPPLEAPIV1);
        }

        if (!endpoints.containsKey(server)) {
            BaseMessage.sendMessageOnSlash(event, Messages.INVALID_SERVER);
            return;
        }

        BestInformations bestInformations = new BestInformations();
        bestInformations.server = server;
        bestInformations.mode = mode;
        bestInformations.modeId = modeId;
        bestInformations.offset = 0;
        bestInformations.name = name;

        userOffsets.put(userId, bestInformations);
        requestBest(bestInformations, event);
    }

    public void requestBest(BestInformations infos, Event event) {
        BestHelper recentHelper = new BestHelper();
        GotBest gotBest = null;
        try {
            switch (endpoints.get(infos.server).getType()) {
                case "BANCHOPY":
                    gotBest = recentHelper.requestBestBanchoPy(infos);
                    break;
                case "RIPPLEAPIV1":
                    gotBest = recentHelper.requestBestRippleAPIV1(infos);
                    break;
                case "BANCHO":
                    if (Integer.parseInt(infos.modeId) > 3) {
                        BaseMessage.sendMessageOnSlash(event, Messages.INVALID_MODE_SERVER,
                                new Placeholder("%server%", infos.server));
                        return;
                    }

                    gotBest = recentHelper.requestBestBancho(infos);
                    break;
                default:
                    Flogger.instance.log(Prefix.ERROR, "Issue finding endpoint at requestBest()", 0);
                    return;
            }

            if (gotBest == null) {
                BaseMessage.sendMessageOnSlash(event, Messages.INVALID_MODE_SERVER,
                        new Placeholder("%server%", infos.server));
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            BaseMessage.sendMessageOnSlash(event, Messages.USER_SCORES_NOT_FOUND, new Placeholder("%name%", infos.name),
                    new Placeholder("%server%", infos.server));
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        String nameW = infos.name.substring(0, 1).toUpperCase() + infos.name.substring(1);

        embed.setTitle("Best plays on " + Best.endpoints.get(infos.server).getName() + " for " + nameW);
        embed.setDescription(recentHelper.convertDescription(gotBest, nameW, infos));
        embed.addField("Score", String.valueOf(gotBest.score), true);
        if (gotBest.pp != 0.0)
            embed.addField("Performance Points (PP)", String.valueOf(gotBest.pp), true);

        if (gotBest.acc != 0.0)
            embed.addField("Accuracy", String.format("%.2f", gotBest.acc) + "%", true);
        String[] mods = OsuConverter.convertMods(Integer.parseInt(String.valueOf(gotBest.mods)));
        if (mods.length == 0) {
            embed.addField("Mods", "-", true);
        } else {
            embed.addField("Mods", "+" + String.join("", mods), true);
        }

        if (gotBest.playtime != null)
            embed.addField("Submitted", OsuConverter.convertToDiscordTimestamp(gotBest.playtime), true);

        embed.addField("Difficulty", String.format("%.2f*", gotBest.diff), true);

        embed.addField("AR", String.valueOf(gotBest.ar), true);

        if (gotBest.bpm != 0)
            embed.addField("BPM", String.valueOf(gotBest.bpm), true);

        embed.addField("OD", String.valueOf(gotBest.od), true);

        embed.setImage("https://assets.ppy.sh/beatmaps/" + gotBest.setId + "/covers/cover.jpg");

        embed.setColor(0x5755d9);
        embed.setFooter("Data from " + Recent.endpoints.get(infos.server).getName());

        ItemComponent[] pageButtons = EndpointHelper.getPageButtons(infos.offset == 0,
                gotBest.size == (infos.offset + 1), "bes");
        List<ItemComponent> urlButtons = new ArrayList<>();

        if (endpoints.get(infos.server).getType().equals("BANCHO")) {
            urlButtons.add(Button.link("https://osu.ppy.sh/beatmapsets/" + gotBest.setId + "#osu/" + gotBest.mapId,
                    "View Beatmap!"));
        } else {
            urlButtons.add(Button.link(Recent.endpoints.get(infos.server).getUrl() + "/scores/" + gotBest.score,
                    "View Score"));
            urlButtons.add(Button.link("https://osu.direct/beatmapsets/" + gotBest.setId + "/"
                    + gotBest.mapId, "View Beatmap"));
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

        if (event.getComponentId().equals("next_page_bes")) {

            InformationBase infos = userOffsets.get(userId);
            if (infos != null && event.getMessage().getId().equals(infos.messageId)) {
                infos.offset += 1;
                userOffsets.put(userId, infos);
                requestBest((BestInformations) infos, event);
                scheduleOffsetRemoval(userId);
            }

        } else if (event.getComponentId().equals("prev_page_bes")) {

            InformationBase infos = userOffsets.get(userId);
            if (infos != null && event.getMessage().getId().equals(infos.messageId)) {
                if (infos.offset > 0) {
                    infos.offset -= 1;
                    userOffsets.put(userId, infos);
                    requestBest((BestInformations) infos, event);
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
        return "best";
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
