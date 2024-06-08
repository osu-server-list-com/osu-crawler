package osu.serverlist.DiscordBot.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import osu.serverlist.DiscordBot.commands.Best;
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


        Profile.servers = getServersForEndpoint(ServerEndpoints.VOTE, EndpointType.BANCHOPY, EndpointType.RIPPLEAPIV1);

        Leaderboard.servers = getServersForEndpoint(ServerEndpoints.LEADERBOARD, EndpointType.BANCHOPY);

        Recent.servers = getServersForEndpoint(ServerEndpoints.RECENT, EndpointType.BANCHOPY, EndpointType.RIPPLEAPIV1);

        Best.servers = getServersForEndpoint(ServerEndpoints.BEST, EndpointType.BANCHOPY, EndpointType.RIPPLEAPIV1);

    }


    private static String ENDPOINT_SQL = "SELECT `name`, `dcbot` FROM `un_endpoints` LEFT JOIN `un_servers` ON `un_endpoints`.`srv_id` = `un_servers`.`id` WHERE `type` = ? AND `visible` = 1 % ORDER BY `votes` DESC";

    public String[] getServersForEndpoint(ServerEndpoints type, EndpointType... endpoints) {
        ArrayList<String> serverList = new ArrayList<>();

        String endpointSql = "";
        for (int i = 0; i < endpoints.length; i++) {
            if (i == 0) {
                endpointSql += " AND (`apitype` = '" + endpoints[i].name() + "'";
            } else {
                endpointSql += " OR `apitype` = '" + endpoints[i].name() + "'";
            }
        }
        endpointSql += ")";

        ResultSet endpointResult = mysql.Query(ENDPOINT_SQL.replaceAll("%", endpointSql), type.name());
        try {
            while (endpointResult.next()) {
                if (!endpointResult.getBoolean("dcbot"))
                    continue;
                serverList.add(endpointResult.getString("name"));
               
            }
            return serverList.toArray(new String[0]);
        } catch (SQLException e) {
            Flogger.instance.error(e);
        }

        return new String[0];
    }

}
