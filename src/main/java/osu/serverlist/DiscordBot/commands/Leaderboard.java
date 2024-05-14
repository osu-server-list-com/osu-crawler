package osu.serverlist.DiscordBot.commands;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.GetRequest;
import commons.marcandreher.Commons.MySQL;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import osu.serverlist.DiscordBot.DiscordCommand;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.ModeHelper.SortHelper;
import osu.serverlist.Models.ServerInformations;

public class Leaderboard implements DiscordCommand {

    public static String[] servers = { "Loading..." };
    
    public static HashMap<String, ServerInformations> endpoints = new HashMap<>();

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
        String server = event.getOption("server").getAsString().toLowerCase();
        String mode = event.getOption("mode").getAsString().toLowerCase();
        String sort = event.getOption("sort").getAsString();
        String modeId = ModeHelper.convertMode(mode);
        String sortId = SortHelper.convertSort(sort);
        event.deferReply().queue();
        
        if(modeId == null ||sortId == null) {
          
            event.getHook().sendMessage("Invalid mode or sort").queue();
            return;
        }

         MySQL mysql = null;
        try {
            mysql = Database.getConnection();
            if (!endpoints.containsKey(server)) {
                ResultSet endpointResult = mysql.Query(
                        "SELECT `endpoint`, `devserver`, `url`, `name`, `dcbot` FROM `un_endpoints` LEFT JOIN `un_servers` ON `un_endpoints`.`srv_id` = `un_servers`.`id` WHERE `type` = 'LEADERBOARD' AND `apitype` = 'BANCHOPY' AND LOWER(`name`) = ?",
                        server);
                while (endpointResult.next()) {
                    if (!endpointResult.getBoolean("dcbot"))
                        continue;
                    ServerInformations s = new ServerInformations();
                    s.setEndpoint(endpointResult.getString("endpoint"));
                    s.setAvatarServer("https://a." + endpointResult.getString("devserver"));
                    s.setUrl("https://" + endpointResult.getString("url"));
                    s.setName(endpointResult.getString("name"));
                    endpoints.put(server, s);
                }
            }
        } catch (Exception e) {
            Flogger.instance.error(e);
            event.getHook().sendMessage("Internal error").queue();
            return;
        } finally {
            mysql.close();
        }

        if (!endpoints.containsKey(server)) {
            event.getHook().sendMessage("Server not found").queue();
            return;
        }

        String url = endpoints.get(server).getEndpoint() + "?sort=" + sortId + "&mode=" + modeId + "&limit=25&offset=0";
        Flogger.instance.log(Prefix.API, "Request: " + url, 0);
        String response;
        try {
            response = new GetRequest(url).send("osu!ListBot");
        } catch (Exception e) {
            event.getHook().sendMessage("User not found on " + endpoints.get(server).getName()).queue();
            return;
        }

         try {
            // Parse the JSON string
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(response);

            // Get the "leaderboard" array
            JSONArray leaderboard = (JSONArray) jsonObject.get("leaderboard");
            String description = "";
            int rank = 0;
            for (Object obj : leaderboard) {
                JSONObject player = (JSONObject) obj;
                rank++;
                // Extract required fields
                String name = (String) player.get("name");
                long playerId = (long) player.get("player_id");
                String country = (String) player.get("country");
                long pp = (long) player.get("pp");
                double acc = (double) player.get("acc");
                long playtime = (long) player.get("playtime");
                double playtimeHr =  Math.floor(playtime / 3600 * 100) / 100;

                description += ":flag_" + country + ": [" + name + "](" + endpoints.get(server).getUrl() + "/u/"+playerId+ ") #"+rank + " (" + pp + "pp, " + acc + "%, " + playtimeHr + "h)" + "\n";
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Leaderboard for " + server + " - " + mode.toUpperCase() + " - " + sort);
            embed.setDescription(description);
            embed.setColor(0x5755d9);
            embed.setFooter("Data from " + endpoints.get(server).getName());
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            e.printStackTrace();
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
    
}
