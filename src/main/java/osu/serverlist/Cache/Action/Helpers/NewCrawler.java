package osu.serverlist.Cache.Action.Helpers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import commons.marcandreher.Commons.GetRequest;
import commons.marcandreher.Commons.MySQL;
import commons.marcandreher.Utils.DelayedPrint;
import osu.serverlist.Cache.Action.CheckServer;
import osu.serverlist.Input.Commands.ExceptionManager;
import osu.serverlist.Models.Server;

public class NewCrawler {

    private MySQL mysql;

    public NewCrawler(MySQL mysql) {
        this.mysql = mysql;
    }

    public void updateBannedPlayerCount(Server v, int banned_players) {
        updateAnyCount(CrawlerType.BANNED_PLAYERS, v, banned_players);
        DelayedPrint.PrintValue("BANNED", String.valueOf(banned_players), true);

    }

    public void updatePlayerCount(Server v) {
        updateAnyCount(CrawlerType.PLAYERCHECK, v, v.getPlayers());
    }

    public void updateRegisteredPlayerCount(Server v, int registered_players) {
        updateAnyCount(CrawlerType.REGISTERED_PLAYERS, v, registered_players);
        DelayedPrint.PrintValue("REGISTERED", String.valueOf(registered_players), true);
    }

    public void updateExtraBanchoPyStats(Server v) {
        JSONObject jsonObject = null;
        try {
            String jsonResponse = new GetRequest("https://osu-server-list.com/api/v1/banchopy/stats?id=" + v.getId()).send("osu!ListBot");
            jsonObject = CheckServer.parseJsonResponse(jsonResponse);
            updateAnyCount(CrawlerType.MAPS, v, ((Long) jsonObject.get("maps")).intValue());
            updateAnyCount(CrawlerType.CLANS, v, ((Long) jsonObject.get("clans")).intValue());
            updateAnyCount(CrawlerType.PLAYS, v, ((Long) jsonObject.get("plays")).intValue());
            DelayedPrint.PrintValue("MAPS", String.valueOf(jsonObject.get("maps")), true);
            DelayedPrint.PrintValue("CLANS", String.valueOf(jsonObject.get("clans")), true);
            DelayedPrint.PrintValue("PLAYS", String.valueOf(jsonObject.get("plays")), true);
        } catch (Exception e) {
            DelayedPrint.PrintValue("MAPS", "0", false);
            DelayedPrint.PrintValue("CLANS", "0", false);
            DelayedPrint.PrintValue("PLAYS", "0", false);
        }
    }

    public void updateAnyCount(CrawlerType crawlerType, Server v, int value) {
        ResultSet checkRs = mysql.Query(
                "SELECT * FROM `un_crawler` WHERE `srv_id` = ? AND `date` = CURDATE() AND `type` = ?",
                String.valueOf(v.getId()), crawlerType.toString());

        try {
            int i = 0;
            while (checkRs.next()) {
                i++;
                if (value > checkRs.getInt("value")) {
                    mysql.Exec("UPDATE `un_crawler` SET `value`=?,`srv_id`=? WHERE `id` = ?",
                            String.valueOf(value), String.valueOf(v.getId()), String.valueOf(checkRs.getInt("id")));
                }
            }

            if (i == 0) {
                mysql.Exec("INSERT INTO `un_crawler` (`type`, `value`, `date`, `srv_id`) VALUES (?,?,CURDATE(),?)",
                        crawlerType.toString(), String.valueOf(value), String.valueOf(v.getId()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ExceptionManager.addException(e);
        }

    }

}
