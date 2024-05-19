package osu.serverlist.DiscordBot.commands;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import osu.serverlist.DiscordBot.base.DiscordCommand;
import osu.serverlist.DiscordBot.helpers.EndpointHelper;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.OsuConverter;
import osu.serverlist.DiscordBot.helpers.commands.ProfileHelper;
import osu.serverlist.DiscordBot.helpers.commands.ProfileHelper.GotProfile;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class Profile implements DiscordCommand {

    public static String[] servers = { "Loading..." };

    public static HashMap<String, ServerInformations> endpoints = new HashMap<>();

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
        String server = event.getOption("server").getAsString().toLowerCase();
        String name = event.getOption("name").getAsString().toLowerCase();
        String mode = event.getOption("mode").getAsString().toLowerCase();
        event.deferReply().queue();
        String modeSafe = mode;

        mode = ModeHelper.convertMode(mode);
        if (mode == null) {
            event.getHook().sendMessage("Invalid mode").queue();
            return;
        }

        if (!endpoints.containsKey(server)) {
            EndpointHelper.adjustEndpoints(server, ServerEndpoints.VOTE, EndpointType.BANCHOPY, EndpointType.RIPPLEAPIV1);
        }

        if (!endpoints.containsKey(server)) {
            event.getHook().sendMessage("Server not found").queue();
            return;
        }
        ProfileHelper profileHelper = new ProfileHelper();

        GotProfile gotProfile;
        try {
            switch (endpoints.get(server).getType()) {
                case "BANCHOPY":
                    gotProfile = profileHelper.getProfileBanchoPy(name, mode, server);
                    break;
                case "RIPPLEAPIV1":
                    gotProfile = profileHelper.getProfileRippleAPIV1(name, mode, server);
                    break;
                default:
                    Flogger.instance.log(Prefix.ERROR, "Issue finding endpoint at handleCommand() /profile", 0);
                    return;
            }
            gotProfile = profileHelper.getProfileBanchoPy(name, mode, server);
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("User not Found").queue();
            return;
        }

        try {
            double playtimeHr = Math.floor(gotProfile.playtime / 3600 * 100) / 100;

            String numberCount = "<:rankingA:1239849498948407366> " + gotProfile.ACount
                    + " <:rankingS:1239849495999807508> "
                    + gotProfile.SCount + " <:rankingSH:1239849497375277076> " + gotProfile.SHCount
                    + " <:rankingX:1239849492891697242> "
                    + gotProfile.XCount + " <:rankingXH:1239849494393126922> " + gotProfile.XHCount;

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder
                    .setTitle(gotProfile.username + " (" + modeSafe.toUpperCase() + ")",
                            endpoints.get(server).getUrl() + "/u/" + gotProfile.playerId)
                    .setDescription("\n\n" + gotProfile.username + " from "
                            + OsuConverter.convertFlag(gotProfile.country) + "\n\n ")
                    .setThumbnail(endpoints.get(server).getAvatarServer() + "/" + gotProfile.playerId)
                    .addField("Total Score", gotProfile.totalScore.toString(), true)
                    .addField("Ranked Score", gotProfile.rankedScore.toString(), true)
                    .addField("Performance Points", gotProfile.pp.toString() + "pp", true)
                    .addField("Plays", gotProfile.plays.toString(), true)
                    .addField("Playtime", playtimeHr + "hours", true)
                    .addField("Accuracy", gotProfile.acc + "%", true)
                    .addField("Max Combo", gotProfile.maxCombo.toString(), true)
                    .addField("Total Hits", gotProfile.totalHits.toString(), true);
            embedBuilder.addField("Replay Views", gotProfile.replayViews.toString(), true);
            if (gotProfile.counts == true)
                embedBuilder.addField("Rankings", numberCount, false);
            embedBuilder.addField("Rank", "#" + gotProfile.rank.toString(), true)
                    .addField("Country Rank", "#" + gotProfile.countryRank.toString(), true)
                    .setFooter("Pulled from " + endpoints.get(server).getName())
                    .setColor(0x5755d9);

            MessageEmbed embed = embedBuilder.build();
            event.getHook().sendMessageEmbeds(embed).queue();
        } catch (Exception e) {
            Flogger.instance.error(e);
            event.getHook().sendMessage("Internal error | Older versions of bancho.py don't work").queue();
            return;
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
        return "profile";
    }

}
