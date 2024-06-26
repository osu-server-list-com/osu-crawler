package osu.serverlist.DiscordBot.cache;

import java.sql.ResultSet;
import java.sql.SQLException;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.MySQL;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import osu.serverlist.DiscordBot.DiscordBot;

public class UpdateStatusChannel extends DatabaseAction {

    private String channelId = "";

    public UpdateStatusChannel(String channelId) {
        this.channelId = channelId;
    }

    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);
        
        try {
            TextChannel channel = DiscordBot.jdaInstance.getTextChannelById(channelId);

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

    private final String STAT_SQL = "SELECT (SELECT COUNT(`id`) FROM `un_servers`) AS `servers`, (SELECT COUNT(`id`) FROM `un_servers` WHERE `visible` = 1) AS `svisible`, (SELECT SUM(`players`) FROM `un_servers` WHERE `visible` = 1) AS `playersOnline`, (SELECT COUNT(`id`) FROM `un_categories`) AS `categories`, (SELECT COUNT(`id`) FROM `un_votes`) AS `votes`, (SELECT COUNT(`id`) FROM `un_crawler`) AS `crawler`, (SELECT COUNT(`id`) FROM `un_users`) AS `users`, (SELECT SUM(`value`) FROM `un_crawler` WHERE `type` = 'PLAYERCHECK') AS `tplayers`, (SELECT SUM(`value`) FROM `un_crawler` WHERE `type` = 'PLAYERCHECK' AND `date` = CURDATE()) AS `tplayerstoday`, (SELECT ROUND(AVG(`value`), 2) FROM `un_crawler` WHERE `type` = 'PLAYERCHECK') AS `tplayersavg`, (SELECT IFNULL(SUM(`clicks`), 0) FROM `un_analytics` WHERE `date` = CURDATE()) AS `uniqueReqToday`;";

    private MessageEmbed buildEmbed() throws SQLException {
        MySQL mysql = Database.getConnection();

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("OsuServerList");
        builder.setColor(0x5755d9);

        try {
            ResultSet statResult = mysql.Query(STAT_SQL);
            while (statResult.next()) {
                builder.addField("Servers", statResult.getString("servers") + " (" + statResult.getString("svisible") + " Visible)", false);
                builder.addField("Categories", statResult.getString("categories"), false);
                builder.addField("Votes", statResult.getString("votes"), false);
                builder.addField("Users", statResult.getString("users"), false);
              

                builder.addField("Max Players today", statResult.getString("tplayerstoday"), true);
                builder.addField("Players online", statResult.getString("playersOnline"), true);


                builder.addField("Total Players recorded", statResult.getString("tplayers"), false);
                builder.addField("Avg Players on Servers", statResult.getString("tplayersavg"), false);
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