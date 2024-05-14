package osu.serverlist.DiscordBot.cache;

import java.sql.ResultSet;
import java.util.ArrayList;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import osu.serverlist.DiscordBot.commands.Stats;

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

    }

}
