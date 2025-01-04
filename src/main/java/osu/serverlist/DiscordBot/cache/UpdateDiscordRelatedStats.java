package osu.serverlist.DiscordBot.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import osu.serverlist.DiscordBot.DiscordBot;

public class UpdateDiscordRelatedStats extends DatabaseAction {
    public enum BotType {
        SERVERS, PLAYERS
    }

    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);
        JDA jda = DiscordBot.jdaInstance;
        List<Guild> guilds = jda.getGuilds();
        
        updateBotCount(BotType.SERVERS, guilds.size());

        int totalUsers = 0;
        
        for (Guild guild : guilds) {
            totalUsers += guild.getMemberCount();
        }
        updateBotCount(BotType.PLAYERS, totalUsers);
    }

     public void updateBotCount(BotType botType, int value) {
        ResultSet checkRs = mysql.Query(
                "SELECT * FROM `un_crawler` WHERE `srv_id` = ? AND `date` = CURDATE() AND `type` = ? ",
                String.valueOf(0), botType.toString());

        try {
            int i = 0;
            while (checkRs.next()) {
                i++;
                if (value > checkRs.getInt("value")) {
                    mysql.Exec("UPDATE `un_crawler` SET `value`=?,`srv_id`=? WHERE `id` = ?",
                            String.valueOf(value), String.valueOf(0), String.valueOf(checkRs.getInt("id")));
                }
            }

            if (i == 0) {
                mysql.Exec("INSERT INTO `un_crawler` (`type`, `value`, `date`, `srv_id`) VALUES (?,?,CURDATE(),?)",
                botType.toString(), String.valueOf(value), String.valueOf(0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Flogger.instance.error(e);
        }

    }
}
