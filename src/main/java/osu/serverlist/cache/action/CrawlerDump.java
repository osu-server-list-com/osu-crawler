package osu.serverlist.cache.action;

import commons.marcandreher.Commons.MySQL;
import commons.marcandreher.Utils.StringUtils;
import osu.serverlist.Models.Server;

import java.sql.ResultSet;

public class CrawlerDump {
    private final MySQL mysql;
    private final Server server;

    public CrawlerDump(MySQL mysql, Server server) {
        this.mysql = mysql;
        this.server = server;
    }

    private final String CHECK_EXISTING_ENTRY_SQL = "SELECT `srv_id` FROM `un_crawler` WHERE `type` = ? AND `srv_id` = ? AND `date` = CURDATE();";
    private final String INSERT_STAT_SQL = "INSERT INTO `un_crawler`(`type`, `value`, `date`, `srv_id`) VALUES (?, ?, CURDATE(), ?);";
    private final String UPDATE_STAT_SQL = "UPDATE `un_crawler` SET `value` = ? WHERE `srv_id` = ? AND `type` = ? AND `date` = CURDATE();";

    public void dumpStat(String name, CrawlerType type, Integer stat) {
        try {
            ResultSet resultSet = mysql.Query(CHECK_EXISTING_ENTRY_SQL, type.name(), String.valueOf(server.getId()));
            if (resultSet.next()) {
                mysql.Exec(UPDATE_STAT_SQL, String.valueOf(stat), String.valueOf(server.getId()), type.name());
            } else {
                mysql.Exec(INSERT_STAT_SQL, type.name(), String.valueOf(stat), String.valueOf(server.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final String UPDATE_PLAYERS_SQL = "UPDATE `un_servers` SET `players` = ? WHERE `id` = ?;";

    public void updatePlayers(Integer players) {
        mysql.Exec(UPDATE_PLAYERS_SQL, String.valueOf(players), String.valueOf(server.getId()));
    }

    private final String UPDATE_VOTES_SQL = "UPDATE `un_servers` SET `votes` = (SELECT COUNT(`id`) FROM `un_votes` WHERE `expired` = 0 AND `srv_id` = ?) WHERE `id` = ?;";

    public void updateVotes() {
        mysql.Exec(UPDATE_VOTES_SQL, String.valueOf(server.getId()), String.valueOf(server.getId()));
    }

    private final String UPDATE_APIKEY_SQL = "UPDATE `un_servers` SET `apikey` = ? WHERE `id` = ?;";

    public void updateApiKey() {
        if (!(server.getApiKey().length() > 2)) {
            mysql.Exec(UPDATE_APIKEY_SQL, StringUtils.generateRandomString(25), String.valueOf(server.getId()));
        }
    }

    private static final String SET_SERVER_STATUS_SQL = "UPDATE `un_servers` SET `online` = ? WHERE `id` = ?;";

    public static void setServerStatus(MySQL mysql, Server server, Boolean status) {
        mysql.Exec(SET_SERVER_STATUS_SQL, String.valueOf(status ? 1 : 0), String.valueOf(server.getId()));
    }

    private static final String SET_SERVER_PING_SQL = "UPDATE `un_servers` SET `ping` = ? WHERE `id` = ?;";

    public static void setServerPing(MySQL mysql, Server server, long ping) {
        mysql.Exec(SET_SERVER_PING_SQL, String.valueOf(ping), String.valueOf(server.getId()));
    }
}