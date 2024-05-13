package osu.serverlist.Main;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import commons.marcandreher.Cache.CacheTimer;
import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.MySQL;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class DiscordBot {

    public static JDA jdaInstance;

    public Dotenv dotenv;

    public DiscordBot(Flogger logger, CacheTimer cacheTimer) {

        Activity activity = Activity.playing("osu-server-list.com");

        try {
            dotenv = Dotenv.configure().filename("discord.env").load();
            jdaInstance = JDABuilder.createDefault(dotenv.get("DISCORD_BOT_TOKEN"))
                    .addEventListeners(new DiscordCommandHandler()).setActivity(activity).build().awaitReady();
        } catch (Exception e) {
            logger.error(e);
            logger.log(Prefix.ERROR, "Failed to start DiscordBot", 0);
            return;
        }
        cacheTimer.addAction(new UpdateStatusChannel());

    }

    public static void deleteCommandsForAllServers() {
        try {
            List<Guild> guilds = jdaInstance.getGuilds();
            for (Guild guild : guilds) {
                guild.retrieveCommands().queue(commands -> {
                    for (Command command : commands) {
                        // Delete commands by their IDs
                        jdaInstance.deleteCommandById(command.getId()).queue();
                    }
                });
            }
        } catch (Exception e) {
            Flogger.instance.error(e);
        }
    }


    public static void initializeCommand() {
        try {
            // Delete existing commands for all servers
            deleteCommandsForAllServers();
    
            // Add or update commands for all servers
            List<Guild> guilds = jdaInstance.getGuilds();
            for (Guild guild : guilds) {
                guild.upsertCommand("stats", "Get stats of a server")
                    .addOption(OptionType.STRING, "server", "The name of the server", true, true)
                    .queue();
    
                guild.upsertCommand("invite", "Invite the bot")
                    .queue();
            }

            jdaInstance.upsertCommand("stats", "Get stats of a server")
            .addOption(OptionType.STRING, "server", "The name of the server", true, true)
            .queue();
            jdaInstance.upsertCommand("invite", "Invite the bot")
            .queue();
        } catch (Exception e) {
            Flogger.instance.error(e);
        }
    }

    public class UpdateStatusChannel extends DatabaseAction {

        @Override
        public void executeAction(Flogger logger) {
            super.executeAction(logger);
            DiscordCommandHandler.updateServers(mysql);
            try {
                TextChannel channel = jdaInstance.getTextChannelById(dotenv.get("DISCORD_STATS_CHANNEL_ID"));

                channel.getIterableHistory().takeAsync(1).thenAccept(messages -> {
                    try {
                        if (!messages.isEmpty()) {
                            Message lastMessage = messages.get(0);
                            lastMessage.editMessage("").setEmbeds(buildEmbed()).queue();
                        } else {
                            channel.sendMessage("").setEmbeds(buildEmbed()).queue();
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }
                });
            } catch (Exception e) {
                logger.error(e);
            }
        }

        private final String STAT_SQL = "SELECT (SELECT COUNT(`id`) FROM `un_servers`) AS `servers`, (SELECT COUNT(`id`) FROM `un_categories`) AS `categories`, (SELECT COUNT(`id`) FROM `un_votes`) AS `votes`, (SELECT COUNT(`id`) FROM `un_crawler`) AS `crawler`, (SELECT COUNT(`id`) FROM `un_users`) AS `users`, (SELECT SUM(`value`) FROM `un_crawler` WHERE `type` = 'PLAYERCHECK') AS `tplayers`, (SELECT SUM(`value`) FROM `un_crawler` WHERE `type` = 'PLAYERCHECK' AND `date` = CURDATE()) AS `tplayerstoday`, (SELECT ROUND(AVG(`value`), 2) FROM `un_crawler` WHERE `type` = 'PLAYERCHECK') AS `tplayersavg`, (SELECT IFNULL(SUM(`clicks`), 0) FROM `un_analytics` WHERE `date` = CURDATE()) AS `uniqueReqToday`;";

        private MessageEmbed buildEmbed() throws SQLException {
            MySQL mysql = Database.getConnection();

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("OsuServerList");
            builder.setColor(0x5755d9);

            try {
                ResultSet statResult = mysql.Query(STAT_SQL);
                while (statResult.next()) {
                    builder.addField("Servers", statResult.getString("servers"), false);
                    builder.addField("Categories", statResult.getString("categories"), false);
                    builder.addField("Votes", statResult.getString("votes"), false);
                    builder.addField("Users", statResult.getString("users"), false);
                    builder.addField("Total Players", statResult.getString("tplayers"), false);
                    builder.addField("Total Players today", statResult.getString("tplayerstoday"), false);
                    builder.addField("AVG Players on Servers", statResult.getString("tplayersavg"), false);
                    builder.addField("Unique Requests today", statResult.getString("uniqueReqToday"), false);
                    builder.addField("Crawler DB Records", statResult.getString("crawler"), false);
                }

            } catch (Exception e) {
                Flogger.instance.error(e);
                builder.addField("MySQL Error", "Waiting...", false);

            }

            builder.setFooter("Powered by osu-server-list.com");
            mysql.close();
            return builder.build();
        }

    }

}
