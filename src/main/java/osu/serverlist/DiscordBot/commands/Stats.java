package osu.serverlist.DiscordBot.commands;

import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.MySQL;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import osu.serverlist.DiscordBot.DiscordCommand;

public class Stats implements DiscordCommand {

    public static String[] servers = { "Loading..." };

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
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
                        .setDescription("\n\n Server ID: " + serverResult.getInt("id") )
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
                    embed.setThumbnail("https://osu-server-list.com" + logoLoc.replaceAll(" ", "%20"));
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
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        List<Command.Choice> options = Stream.of(servers)
                .filter(server -> server.toLowerCase()
                        .startsWith(event.getFocusedOption().getValue().toLowerCase()))
                .map(server -> new Command.Choice(server, server))
                .collect(Collectors.toList());
        event.replyChoices(options).queue();
    }

    @Override
    public String getName() {
        return "stats";
    }

}
