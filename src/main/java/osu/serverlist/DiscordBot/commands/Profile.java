package osu.serverlist.DiscordBot.commands;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.GetRequest;
import commons.marcandreher.Commons.MySQL;
import commons.marcandreher.Commons.Flogger.Prefix;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import osu.serverlist.DiscordBot.DiscordCommand;

public class Profile implements DiscordCommand{

    public static String[] servers = { "Loading..." };

    public static HashMap<String, String> endpoints = new HashMap<>();

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
        String server = event.getOption("server").getAsString().toLowerCase();
        String name = event.getOption("name").getAsString().toLowerCase();
        String mode = event.getOption("mode").getAsString().toLowerCase();
        event.deferReply().queue();

        switch(mode) {
            case "osu": 
                mode = "0";
                break;
            case "osurx":
                mode = "4";
                break;
            case "osuap":
                mode = "8";
                break;
            case "taiko":
                mode = "1";
                break;
            case "catch":
                mode = "2";
                break;
            case "mania":
                mode = "3";
                break;
            case "taikorx":
                mode = "5";
                break;
            case "catchrx":
                mode = "6";
                break;
            default:
                event.getHook().sendMessage("Invalid mode").queue();
                return;
        }

        MySQL mysql = null;
        try {
            mysql = Database.getConnection();
            // Get endpoint from db
            if(!endpoints.containsKey(server)) {
                
                ResultSet endpointResult = mysql.Query("SELECT `endpoint` FROM `un_endpoints` LEFT JOIN `un_servers` ON `un_endpoints`.`srv_id` = `un_servers`.`id` WHERE `type` = 'VOTE' AND `apitype` = 'BANCHOPY' AND LOWER(`name`) = ?", server);
                while (endpointResult.next()) {
                    endpoints.put(server, endpointResult.getString("endpoint"));
                }
            }
        } catch (Exception e) {
            Flogger.instance.error(e);
            event.getHook().sendMessage("Internal error").queue();
            return;
        }finally{
            mysql.close();
        }

        if(!endpoints.containsKey(server)) {
            event.getHook().sendMessage("Server not found").queue();
            return;
        }

        String url = endpoints.get(server) + "?name=" + name.replaceAll(" ", "_") + "&scope=all";
        Flogger.instance.log(Prefix.API, "Request: " + url, 0);
        String response;
        try {
            response = new GetRequest(url).send("osu!ListBot");
        } catch (Exception e) {
            Flogger.instance.error(e);
            event.getHook().sendMessage("Internal error").queue();
            return;
        }

        JSONParser parser = new JSONParser();
        try {
            JSONObject json = (JSONObject) parser.parse(response);
        
            // Get the "player" object
            JSONObject player = (JSONObject) json.get("player");
        
            // Get the "info" object within the "player" object
            JSONObject info = (JSONObject) player.get("info");
        
            // Get the "id" from the "info" object
            Long id = (Long) info.get("id");
        
            // Check if id is null, indicating player not found
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
            Long total_hits = (Long) modeObject.get("total_hits");
            Long replay_views = (Long) modeObject.get("replay_views");
            Long xh_count = (Long) modeObject.get("xh_count");
            Long x_count = (Long) modeObject.get("x_count");
            Long sh_count = (Long) modeObject.get("sh_count");
            Long s_count = (Long) modeObject.get("s_count");
            Long a_count = (Long) modeObject.get("a_count");
            Long rank = (Long) modeObject.get("rank");
            Long country_rank = (Long) modeObject.get("country_rank");
        
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Player Stats")
                    .setDescription("Stats for the selected mode")
                    .addField("ID", id.toString(), true)
                    .addField("Total Score", tscore.toString(), true)
                    .addField("Ranked Score", rscore.toString(), true)
                    .addField("Performance Points", pp.toString(), true)
                    .addField("Plays", plays.toString(), true)
                    .addField("Playtime", playtime.toString(), true)
                    .addField("Accuracy", acc.toString(), true)
                    .addField("Max Combo", max_combo.toString(), true)
                    .addField("Total Hits", total_hits.toString(), true)
                    .addField("Replay Views", replay_views.toString(), true)
                    .addField("XH Count", xh_count.toString(), true)
                    .addField("X Count", x_count.toString(), true)
                    .addField("SH Count", sh_count.toString(), true)
                    .addField("S Count", s_count.toString(), true)
                    .addField("A Count", a_count.toString(), true)
                    .addField("Rank", rank.toString(), true)
                    .addField("Country Rank", country_rank.toString(), true)
                    .setColor(0x00ff00);
        
            MessageEmbed embed = embedBuilder.build();
            event.getHook().sendMessageEmbeds(embed).queue();
        } catch (Exception e) {
            Flogger.instance.error(e);
            event.getHook().sendMessage("Internal error").queue();
            return;
        }
        




    }

    

    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        if(event.getFocusedOption().getName().equals("server") ) {
            List<Command.Choice> options = Stream.of(servers)
            .filter(server -> server.toLowerCase()
                    .startsWith(event.getFocusedOption().getValue().toLowerCase()))
            .map(server -> new Command.Choice(server, server))
            .collect(Collectors.toList());
            event.replyChoices(options).queue();
        }else if(event.getFocusedOption().getName().equals("mode")) {
            List<Command.Choice> options = Stream.of("osu", "osurx", "osuap", "taiko", "catch", "mania", "taikorx", "catchrx")
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