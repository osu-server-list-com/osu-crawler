package osu.serverlist.DiscordBot.helpers;

import java.sql.ResultSet;
import java.sql.SQLException;

import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.MySQL;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import osu.serverlist.DiscordBot.commands.Best;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.Models.ServerInformations;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class EndpointHelper {

    private static String ENDPOINT_SQL = "SELECT `endpoint`, `devserver`, `url`, `name`, `dcbot`, `apitype` FROM `un_endpoints` LEFT JOIN `un_servers` ON `un_endpoints`.`srv_id` = `un_servers`.`id` WHERE `type` = ? % AND LOWER(`name`) = ?";

    public static void adjustEndpoints(String server, ServerEndpoints type, EndpointType... endpoints) {
        MySQL mysql = null;
        try {
            mysql = Database.getConnection();
        } catch (SQLException e) {
            Flogger.instance.error(e);
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

        ResultSet endpointResult = mysql.Query(ENDPOINT_SQL.replaceAll("%", endpointSql), type.name(), server);
        try {
            while (endpointResult.next()) {
                if (!endpointResult.getBoolean("dcbot"))
                    continue;
                ServerInformations s = new ServerInformations();
                s.setEndpoint(endpointResult.getString("endpoint"));
                s.setAvatarServer("https://a." + endpointResult.getString("devserver"));
                s.setUrl("https://" + endpointResult.getString("url"));
                s.setName(endpointResult.getString("name"));
                s.setType(endpointResult.getString("apitype"));
                switch (type) {
                    case LEADERBOARD:
                        Leaderboard.endpoints.put(server, s);
                        continue;
                    case VOTE:
                        Profile.endpoints.put(server, s);
                        continue;
                    case RECENT:
                        Recent.endpoints.put(server, s);
                        continue;
                    case BEST:
                        Best.endpoints.put(server, s);
                        continue;
                    default:
                        continue;
                }
            }
        } catch (SQLException e) {
            Flogger.instance.error(e);
        }

        mysql.close();

    }

    public static ItemComponent[] getPageButtons(boolean prev, boolean next, String identifier) {
        ItemComponent[] buttons = new ItemComponent[2];

        if (prev) {
            buttons[0] = Button.danger("prev_page_" + identifier, "Previous Page").asDisabled();
        } else {
            buttons[0] = Button.danger("prev_page_" + identifier, "Previous Page");
        }

        if (next) {
            buttons[1] = Button.success("next_page_" + identifier, "Next Page").asDisabled();
        } else {
            buttons[1] = Button.success("next_page_" + identifier, "Next Page");
        }

        return buttons;
    }

}
