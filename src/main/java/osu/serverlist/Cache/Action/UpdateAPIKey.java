package osu.serverlist.Cache.Action;

import java.sql.ResultSet;

import commons.marcandreher.Commons.MySQL;
import commons.marcandreher.Utils.StringUtils;
import osu.serverlist.Models.Server;


public class UpdateAPIKey {

    public static void executeAction(ResultSet rs, Server v, MySQL mysql) throws Exception {

        if(!(rs.getString("apikey").length() > 2)) {
            mysql.Exec("UPDATE `un_servers` SET `apikey`=? WHERE `id` = ?", StringUtils.generateRandomString(25), v.getId() + "");
        }
    }

}
