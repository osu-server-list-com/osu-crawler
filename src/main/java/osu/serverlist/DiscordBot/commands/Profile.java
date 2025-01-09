package osu.serverlist.DiscordBot.commands;

import java.text.DecimalFormat;
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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import osu.serverlist.DiscordBot.base.DiscordCommand;
import osu.serverlist.DiscordBot.base.MessageBuilder;
import osu.serverlist.DiscordBot.helpers.EndpointHelper;
import osu.serverlist.DiscordBot.helpers.LinkService;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.OsuConverter;
import osu.serverlist.DiscordBot.helpers.LinkService.LinkResponseObject;
import osu.serverlist.DiscordBot.helpers.commands.ProfileHelper;
import osu.serverlist.DiscordBot.helpers.commands.ProfileHelper.GotProfile;
import osu.serverlist.DiscordBot.helpers.exceptions.InvalidModeException;
import osu.serverlist.DiscordBot.helpers.exceptions.InvalidPlayerException;
import osu.serverlist.DiscordBot.helpers.exceptions.InvalidScorePlayerException;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class Profile implements DiscordCommand {

    public static String[] servers = { "Loading..." };

    public static HashMap<String, ServerInformations> endpoints = new HashMap<>();

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        LinkService service = new LinkService();
        LinkResponseObject response = service.getLink(event);

        String server = response.getServer();
        String name = response.getName();
        String mode = response.getMode();

        service.close();
     
        String modeSafe = mode;

        mode = ModeHelper.convertMode(mode);
        if (mode == null) {
            event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError("Invalid mode").build()).queue();
            return;
        }

        if (!endpoints.containsKey(server)) {
            EndpointHelper.adjustEndpoints(server, ServerEndpoints.VOTE, EndpointType.BANCHOPY,
                    EndpointType.RIPPLEAPIV1);
        }

        if (!endpoints.containsKey(server) && !(server.equals("bancho"))) {
            event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError("Server not found").build()).queue();
            return;
        }
        ProfileHelper profileHelper = new ProfileHelper();

        GotProfile gotProfile = null;
        try {
            switch (endpoints.get(server).getType()) {
                case "BANCHOPY":
                    gotProfile = profileHelper.getProfileBanchoPy(name, mode, server);
                    break;
                case "RIPPLEAPIV1":
                    gotProfile = profileHelper.getProfileRippleAPIV1(name, modeSafe, server);
                    break;
                case "BANCHO":
                    gotProfile = profileHelper.getProfileBancho(name, mode, server);
                    break;
                default:
                    Flogger.instance.log(Prefix.ERROR, "Issue finding endpoint at handleCommand() /profile", 0);
                    return;
            }
        } catch (InvalidModeException | InvalidPlayerException | InvalidScorePlayerException e) {
            event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError(e.getMessage()).build()).queue();
            return;

        } catch (Exception e) {
            Flogger.instance.error(e);
            event.getHook().sendMessage("Internal error").queue();
            return;
        }

        try {

            String numberCount = "<:rankingA:1239849498948407366> " + gotProfile.ACount
                    + " <:rankingS:1239849495999807508> "
                    + gotProfile.SCount + " <:rankingSH:1239849497375277076> " + gotProfile.SHCount
                    + " <:rankingX:1239849492891697242> "
                    + gotProfile.XCount + " <:rankingXH:1239849494393126922> " + gotProfile.XHCount;

            EmbedBuilder embedBuilder = new EmbedBuilder();
            DecimalFormat decimalFormat = new DecimalFormat("#,###");
            String profileUrl = endpoints.get(server).getUrl() + "/u/" + gotProfile.playerId;
            embedBuilder
                    .setTitle(gotProfile.username + " (" + modeSafe.toUpperCase() + ")",
                            profileUrl)
                    .setDescription("\n\n" + gotProfile.username + " from "
                            + OsuConverter.convertFlag(gotProfile.country) + "\n\n ")
                    .setThumbnail(endpoints.get(server).getAvatarServer() + "/" + gotProfile.playerId)
                    .addField("Total Score", decimalFormat.format(gotProfile.totalScore), true)
                    .addField("Ranked Score", decimalFormat.format(gotProfile.rankedScore), true)
                    .addField("Performance Points", decimalFormat.format(gotProfile.pp) + "pp", true)
                    .addField("Plays", decimalFormat.format(gotProfile.plays), true);

            if (gotProfile.playtime != null) {
                double playtimeHr = Math.floor(gotProfile.playtime / 3600 * 100) / 100;
                embedBuilder.addField("Playtime", playtimeHr + " hours", true);
            }

            if(gotProfile.level != null)
                embedBuilder.addField("Level", String.format("%.2f", gotProfile.level), true);

            embedBuilder.addField("Accuracy", String.format("%.2f", gotProfile.acc) + "%", true);
            // Atoka, redstar fix
            if (gotProfile.maxCombo != null)
                embedBuilder.addField("Max Combo", decimalFormat.format(gotProfile.maxCombo), true);

            if (gotProfile.totalHits != null)
                embedBuilder.addField("Total Hits", decimalFormat.format(gotProfile.totalHits), true);

            if (gotProfile.replayViews != null)
                embedBuilder.addField("Replay Views", decimalFormat.format(gotProfile.replayViews), true);

            if (gotProfile.counts == true)
                embedBuilder.addField("Rankings", numberCount, false);
            embedBuilder.addField("Rank", "#" + convertRank(gotProfile.rank), true)
                    .addField("Country Rank", "#" + convertRank(gotProfile.countryRank), true)
                    .setFooter("Data from " + endpoints.get(server).getName())
                    .setColor(0x5755d9);

            MessageEmbed embed = embedBuilder.build();
            event.getHook().sendMessageEmbeds(embed).addActionRow(Button.link(profileUrl, "View Profile")).queue();
        } catch (Exception e) {
            Flogger.instance.error(e);
            event.getHook().sendMessage("Internal error").queue();
            return;
        }

    }

    private String convertRank(Long rank) {
        if (rank == null || rank == 0) {
            return "-";
        } else {
            return rank.toString();
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
