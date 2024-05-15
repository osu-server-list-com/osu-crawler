package osu.serverlist.DiscordBot.cache;

import java.sql.ResultSet;
import java.util.ArrayList;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.DiscordBot.commands.Stats;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class UpdateAutocompletions extends DatabaseAction {


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


        Profile.servers = getServersForEndpoint(ServerEndpoints.VOTE, EndpointType.BANCHOPY);

        Leaderboard.servers = getServersForEndpoint(ServerEndpoints.LEADERBOARD, EndpointType.BANCHOPY);

        Recent.servers = getServersForEndpoint(ServerEndpoints.RECENT, EndpointType.BANCHOPY);

    }

    public String[] getServersForEndpoint(ServerEndpoints serverEndpoint, EndpointType endpointType) {
        ArrayList<String> serverList = new ArrayList<>();
        try {
            ResultSet serverRs = mysql.Query("SELECT `name` FROM `un_servers` LEFT JOIN `un_endpoints` ON `un_servers`.`id` = `un_endpoints`.`srv_id` WHERE `visible` = 1 AND `type` = '" + serverEndpoint.name() + "' AND `apitype` = '" + endpointType.name() + "' ORDER BY `votes` DESC;");
            while (serverRs.next()) {
                serverList.add(serverRs.getString("name"));
                if (serverList.size() >= 25)
                    break;
            }

            return serverList.toArray(new String[0]);

        } catch (Exception e) {
            Flogger.instance.error(e);
        }
        
        return new String[0];
    }

}
