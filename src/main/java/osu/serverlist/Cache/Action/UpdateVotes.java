package osu.serverlist.Cache.Action;

import java.sql.ResultSet;

import commons.marcandreher.Commons.MySQL;
import osu.serverlist.Models.Server;

public class UpdateVotes {

    public static void executeAction(MySQL mysql, Server v) throws Exception {
        ResultSet voteRs = mysql.Query("SELECT `id` FROM `un_votes` WHERE `srv_id` = ? AND `expired` = 0", v.getId() + "");
        Integer votes = 0;
        while (voteRs.next()) {
            votes++;
        }

        ResultSet expiredVotesRs = mysql.Query("SELECT `id` FROM `un_votes` WHERE `srv_id` = ? AND `expired` = 1", v.getId() + "");
        Integer expiredVotes = 0;
        while (expiredVotesRs.next()) {
            expiredVotes++;
        }
        mysql.Exec("UPDATE `un_servers` SET `expired`=? WHERE `id` = ?", String.valueOf(expiredVotes), String.valueOf(v.getId()));

        mysql.Exec("UPDATE `un_servers` SET `votes`=? WHERE `id` = ?", String.valueOf(votes), String.valueOf(v.getId()));
    }

}
