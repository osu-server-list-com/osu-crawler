package osu.serverlist.DiscordBot.commands;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import osu.serverlist.DiscordBot.DiscordCommand;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.ModeHelper.SortHelper;
import osu.serverlist.Models.ServerInformations;

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
        public int offset;
    }

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {

        String userId = event.getUser().getId(); // Get user ID

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
        String response = "";
        String url = "";
        try {
            url = endpoints.get(infos.server).getEndpoint() + "?sort=" + infos.sortId + "&mode=" + infos.modeId
                    + "&limit=25&offset=" + (infos.offset) * 25;
            Flogger.instance.log(Prefix.API, "Request: " + url, 0);

            response = new GetRequest(url).send("osu!ListBot");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(response + " | " + url);
            return;
        }

        String description = "";
        try {
            // Parse the JSON string
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(response);

            // Get the "leaderboard" array
            JSONArray leaderboard = (JSONArray) jsonObject.get("leaderboard");

            int rank = infos.offset * 25;
            for (Object obj : leaderboard) {
                JSONObject player = (JSONObject) obj;
                rank++;
                // Extract required fields
                String name = (String) player.get("name");
                long playerId = (long) player.get("player_id");
                String country = (String) player.get("country");
                String countryFlag = ":flag_" + country + ":";
                if(country == "XX") countryFlag = ":flag_white:";
                long pp = (long) player.get("pp");
                double acc = (double) player.get("acc");
                long playtime = (long) player.get("playtime");
                double playtimeHr = Math.floor(playtime / 3600 * 100) / 100;

                description += countryFlag + " [" + name + "](" + endpoints.get(infos.server).getUrl() + "/u/"
                        + playerId + ") #" + rank  + " (" + pp + "pp, " + acc + "%, " + playtimeHr + "h)" + "\n";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Button nextPageButton = Button.secondary("next_page", "Next Page");

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Leaderboard for " + infos.server + " - " + infos.mode + " - " + infos.sort + " (Page "
                + (infos.offset + 1) + ")");
        embed.setDescription(description);
        embed.setColor(0x5755d9);
        embed.setFooter("Data from " + endpoints.get(infos.server).getName());
        embed.build();
        System.out.println("Reached end");

        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).getHook().sendMessageEmbeds(embed.build())
                    .setActionRow(nextPageButton).queue();
        } else if (event instanceof ButtonInteractionEvent) {
            ((ButtonInteractionEvent) event).editMessageEmbeds(embed.build()).setActionRow(nextPageButton).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
    
        if (event.getComponentId().equals("next_page")) {
            LeaderboardInformations infos = userOffsets.get(userId);
            if (infos != null) {
                infos.offset += 1;
                userOffsets.put(userId, infos);
                requestLeaderboard(infos, event);
                scheduleOffsetRemoval(userId);
            } else {
                event.reply("Not you're leaderboard or session expired");
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
        }, 5 * 60 * 1000); // 5 minutes in milliseconds
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
