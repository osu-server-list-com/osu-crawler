package osu.serverlist.Main;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.MySQL;
import commons.marcandreher.Commons.Flogger.Prefix;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

public class DiscordCommandHandler extends ListenerAdapter {
    private static String[] servers = { "Loading..." };

    public static void updateServers(MySQL mysql) {
        ArrayList<String> serverList = new ArrayList<>();
        try {
            ResultSet serverRs = mysql.Query("SELECT `name` FROM `un_servers` WHERE `visible` = 1 ORDER BY votes DESC");
            while (serverRs.next()) {
                serverList.add(serverRs.getString("name"));
                if (serverList.size() >= 25)
                    break;
            }

            servers = serverList.toArray(new String[0]);

        } catch (Exception e) {
            Flogger.instance.error(e);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("stats") && event.getFocusedOption().getName().equals("server")) {
            List<Command.Choice> options = Stream.of(servers)
                    .filter(server -> server.toLowerCase()
                            .startsWith(event.getFocusedOption().getValue().toLowerCase()))
                    .map(server -> new Command.Choice(server, server))
                    .collect(Collectors.toList());
            event.replyChoices(options).queue();
        }
    }

    private void handleStatsCommand(SlashCommandInteractionEvent event) {
        String server = event.getOption("server").getAsString().toLowerCase();
        event.deferReply().queue();

        MySQL mySQL = null;
        try {
            mySQL = Database.getConnection();

            ResultSet serverResult = mySQL.Query(
                    "SELECT `id`,`name`,`players`,`votes`,`url`,`logo_loc` FROM `un_servers` WHERE LOWER(`name`) = ?",
                    server);
            if (!serverResult.next()) {
                event.getHook().sendMessage("No server found with the name: " + server).queue();
                return;
            }

            do {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(serverResult.getString("name"), "https://osu-server-list.com/server/" + server)
                        .setDescription("Server ID: " + serverResult.getInt("id"))
                        .addField("Players", String.valueOf(serverResult.getInt("players")), true)
                        .addField("Votes", String.valueOf(serverResult.getInt("votes")), true);

                ResultSet serverCrawlerInfos = mySQL.Query(
                        "SELECT `type`, `value` FROM `un_crawler` WHERE `srv_id` = ? AND `date` = CURDATE() AND `type` != 'PLAYERCHECK';",
                        serverResult.getString("id"));
                while (serverCrawlerInfos.next()) {
                    String type = "";
                    switch (serverCrawlerInfos.getString("type")) {
                        case "MAPS":
                            type = "Maps";
                            break;
                        case "REGISTERED_PLAYERS":
                            type = "Registered";
                            break;
                        case "PLAYS":
                            type = "Plays";
                            break;
                        case "BANNED_PLAYERS":
                            type = "Banned";
                            break;
                        case "CLANS":
                            type = "Clans";
                            break;
                    }
                    embed = embed.addField(type, serverCrawlerInfos.getString("value"), true);
                }
                embed = embed
                        .addField("URL",
                                "[" + serverResult.getString("url") + "](https://osu-server-list.com/server/" + server
                                        + "/play)",
                                false)
                        .addField("", "[[Vote]](https://osu-server-list.com/server/" + server
                                + "/vote) [[View]](https://osu-server-list.com/server/" + server + ")", false);

                String logoLoc = serverResult.getString("logo_loc");
                if (logoLoc != null && !logoLoc.isEmpty() && !logoLoc.startsWith("http://")) {
                    embed.setThumbnail("https://osu-server-list.com" + logoLoc);
                }
                embed.setColor(0x5755d9);

                event.getHook().sendMessage("").setEmbeds(embed.build()).queue();
            } while (serverResult.next());
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("An error occurred while fetching server stats.").queue();
        } finally {

            if (mySQL != null) {
                mySQL.close();
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Flogger.instance.log(Prefix.API, "Slash Command: " + event.getName(), 0);
        if (event.getName().equals("stats")) {
            handleStatsCommand(event);
        } else {
            String inviteUrl = event.getJDA().getInviteUrl();
            event.reply("Here's the invite link: " + inviteUrl).queue();
        }
    }

}
