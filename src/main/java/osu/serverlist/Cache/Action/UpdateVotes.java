package osu.serverlist.Cache.Action;

import java.sql.ResultSet;

import commons.marcandreher.Commons.MySQL;
import osu.serverlist.Models.Server;

public class UpdateVotes {

    public static void executeAction(MySQL mysql, Server v) throws Exception {
        ResultSet voteRs = mysql.Query("SELECT `id` FROM `un_votes` WHERE `srv_id` = ?", v.getId() + "");
        Integer votes = 0;
        while (voteRs.next()) {
            votes++;
        }

        mysql.Exec("UPDATE `un_servers` SET `votes`=? WHERE `id` = ?", votes + "", v.getId() + "");
    }

}
