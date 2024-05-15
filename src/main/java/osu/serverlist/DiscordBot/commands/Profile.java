package osu.serverlist.DiscordBot.commands;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.GetRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import osu.serverlist.DiscordBot.DiscordCommand;
import osu.serverlist.DiscordBot.helpers.EndpointHelper;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
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
            EndpointHelper.adjustEndpoints(server, ServerEndpoints.VOTE, EndpointType.BANCHOPY);
        }
        
        if (!endpoints.containsKey(server)) {
            event.getHook().sendMessage("Server not found").queue();
            return;
        }

        String url = endpoints.get(server).getEndpoint() + "?name=" + name.replaceAll(" ", "_") + "&scope=all";
        Flogger.instance.log(Prefix.API, "Request: " + url, 0);
        String response;
        try {
            response = new GetRequest(url).send("osu!ListBot");
        } catch (Exception e) {
            event.getHook().sendMessage("User not found on " + endpoints.get(server).getName()).queue();
            return;
        }

        JSONParser parser = new JSONParser();
        try {
            JSONObject json = (JSONObject) parser.parse(response);
            JSONObject player = (JSONObject) json.get("player");
            JSONObject info = (JSONObject) player.get("info");

            Long id = (Long) info.get("id");
            String realName = (String) info.get("name");
            String country = (String) info.get("country");

            if (id == null) {
                event.getHook().sendMessage("Player not found").queue();
                return;
            }

            // Get the "stats" object within the "player" object
            JSONObject stats = (JSONObject) player.get("stats");

            // Get the mode object based on the mode selected
            JSONObject modeObject = (JSONObject) stats.get(mode);

            // Now you can access individual properties within the modeObject
            Long tscore = (Long) modeObject.get("tscore");
            Long rscore = (Long) modeObject.get("rscore");
            Long pp = (Long) modeObject.get("pp");
            Long plays = (Long) modeObject.get("plays");
            Long playtime = (Long) modeObject.get("playtime");

            Double acc = (Double) modeObject.get("acc");
            Long max_combo = (Long) modeObject.get("max_combo");

            Long xh_count = (Long) modeObject.get("xh_count");
            Long x_count = (Long) modeObject.get("x_count");
            Long sh_count = (Long) modeObject.get("sh_count");
            Long s_count = (Long) modeObject.get("s_count");
            Long a_count = (Long) modeObject.get("a_count");

            Long rank = (Long) modeObject.get("rank");
            Long country_rank = (Long) modeObject.get("country_rank");
            Long total_hits = (Long) modeObject.get("total_hits");
            Long replay_views = (Long) modeObject.get("replay_views");

            double playtimeHr = Math.floor(playtime / 3600 * 100) / 100;

            String numberCount = "<:rankingA:1239849498948407366> " + a_count + " <:rankingS:1239849495999807508> "
                    + s_count + " <:rankingSH:1239849497375277076> " + sh_count + " <:rankingX:1239849492891697242> "
                    + x_count + " <:rankingXH:1239849494393126922> " + xh_count;

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(realName + " (" + modeSafe + ")", endpoints.get(server).getUrl() + "/u/" + id)
                    .setDescription(realName + " from :flag_" + country.toLowerCase() + ": \n\n")
                    .setThumbnail(endpoints.get(server).getAvatarServer() + "/" + id)
                    .addField("Total Score", tscore.toString(), true)
                    .addField("Ranked Score", rscore.toString(), true)
                    .addField("Performance Points", pp.toString() + "pp", true)
                    .addField("Plays", plays.toString(), true)
                    .addField("Playtime", playtimeHr + "hours", true)
                    .addField("Accuracy", acc.toString() + "%", true)
                    .addField("Max Combo", max_combo.toString(), true)
                    .addField("Total Hits", total_hits.toString(), true)
                    .addField("Replay Views", replay_views.toString(), true).addField("Rankings", numberCount, false)
                    .addField("Rank", "#" + rank.toString(), true)
                    .addField("Country Rank", "#" + country_rank.toString(), true)
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
