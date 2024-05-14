package osu.serverlist.DiscordBot.cache;

import java.sql.ResultSet;
import java.util.ArrayList;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.DiscordBot.commands.Stats;

public class UpdateAutocompletions extends DatabaseAction {

    private final String BPY_PROFILE_SQL = "SELECT `name` FROM `un_servers` LEFT JOIN `un_endpoints` ON `un_servers`.`id` = `un_endpoints`.`srv_id` WHERE `visible` = 1 AND `type` = 'VOTE' AND `apitype` = 'BANCHOPY' ORDER BY `votes` DESC;";
    private final String BPY_LEADERBOARD_SQL = "SELECT `name` FROM `un_servers` LEFT JOIN `un_endpoints` ON `un_servers`.`id` = `un_endpoints`.`srv_id` WHERE `visible` = 1 AND `type` = 'LEADERBOARD' AND `apitype` = 'BANCHOPY' ORDER BY `votes` DESC;";


    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);

        ArrayList<String> serverList = new ArrayList<>();
        try {
            ResultSet serverRs = mysql.Query("SELECT `name` FROM `un_servers` WHERE `visible` = 1 ORDER BY votes DESC");
            while (serverRs.next()) {
                serverList.add(serverRs.getString("name"));
                if (serverList.size() >= 25)
                    break;
            }

            Stats.servers = serverList.toArray(new String[0]);

        } catch (Exception e) {
            Flogger.instance.error(e);
        }

        ArrayList<String> serverProfile = new ArrayList<>();
        try {
            ResultSet serverRs = mysql.Query(BPY_PROFILE_SQL);
            while (serverRs.next()) {
                serverProfile.add(serverRs.getString("name"));
                if (serverProfile.size() >= 25)
                    break;
            }

            Profile.servers = serverProfile.toArray(new String[0]);

        } catch (Exception e) {
            Flogger.instance.error(e);
        }

        ArrayList<String> serverLeaderboard = new ArrayList<>();
        try {
            ResultSet serverRs = mysql.Query(BPY_LEADERBOARD_SQL);
            while (serverRs.next()) {
                serverLeaderboard.add(serverRs.getString("name"));
                if (serverLeaderboard.size() >= 25)
                    break;
            }

            Leaderboard.servers = serverLeaderboard.toArray(new String[0]);

        } catch (Exception e) {
            Flogger.instance.error(e);
        }


    }

}
