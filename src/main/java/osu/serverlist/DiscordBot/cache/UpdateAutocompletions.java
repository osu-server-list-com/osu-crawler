package osu.serverlist.DiscordBot.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import osu.serverlist.DiscordBot.commands.Best;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.DiscordBot.commands.Stats;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class UpdateAutocompletions extends DatabaseAction {

    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);

        Map<String, Integer> serverNames = new HashMap<String, Integer>();
        List<String> servers = new ArrayList<>();
        try {
            ResultSet serverRs = mysql
                    .Query("SELECT `name`, `id` FROM `un_servers` WHERE `visible` = 1 ORDER BY votes DESC");
            while (serverRs.next()) {
                serverNames.put(serverRs.getString("name").toLowerCase(), serverRs.getInt("id"));

                if (serverNames.size() <= 25) {
                    servers.add(serverRs.getString("name"));
                }
                continue;
            }
            Stats.servers = servers.toArray(new String[0]);
            Stats.serverNames = serverNames;

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
        ServerInformations informations = new ServerInformations();
        informations.setAvatarServer("https://a.ppy.sh");

        informations.setName("Bancho");
        informations.setUrl("https://osu.ppy.sh");
        informations.setType("BANCHO");

        switch (type) {
            case VOTE:
                serverList.add("Bancho");
                informations.setEndpoint("https://osu.ppy.sh/api/get_user");
                Profile.endpoints.put("bancho", informations);
                break;
            case RECENT:
                serverList.add("Bancho");
                informations.setEndpoint("https://osu.ppy.sh/api/get_user_recent");
                Recent.endpoints.put("bancho", informations);
                break;
            case BEST:
                serverList.add("Bancho");
                informations.setEndpoint("https://osu.ppy.sh/api/get_user_best");
                Best.endpoints.put("bancho", informations);
                break;
            default:
                break;
        }

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
