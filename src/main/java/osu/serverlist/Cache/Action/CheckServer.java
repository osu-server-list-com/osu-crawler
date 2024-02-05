package osu.serverlist.Cache.Action;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Cache.Action.Action;
import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.GetRequest;
import commons.marcandreher.Commons.MySQL;
import commons.marcandreher.Utils.DelayedPrint;
import osu.serverlist.Cache.Action.Helpers.NewCrawler;
import osu.serverlist.Input.Commands.ExceptionManager;
import osu.serverlist.Models.Server;
import osu.serverlist.Utils.Endpoints.Endpoint;
import osu.serverlist.Utils.Endpoints.EndpointHandler;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class CheckServer implements Action {
    private MySQL mysql;

    @Override
    public void executeAction(Flogger logger) {

        try {
            this.mysql = Database.getConnection();
        } catch (SQLException e) {
            logger.log(Prefix.ERROR, "Failed to get Connection", 0);
        }
        String sql = "SELECT * FROM `un_servers`";
        
        try {
            ResultSet serverCache = mysql.Query(sql);

            while (serverCache.next()) {
                
                Server v = new Server();
                v.setId(serverCache.getInt("id"));
                v.setName(serverCache.getString("name"));
                v.setVotes(serverCache.getInt("votes"));
   
                v.setUrl(serverCache.getString("url"));

                DelayedPrint dp = new DelayedPrint(v.getName());
                NewCrawler nc = new NewCrawler(mysql);
                UpdateAPIKey.executeAction(serverCache, v, mysql);
                UpdateVotes.executeAction(mysql, v);

                EndpointHandler eh = new EndpointHandler(mysql);
                Endpoint p = eh.getEndpoint(v, ServerEndpoints.PLAYERCHECK);
                EndpointType et = EndpointType.valueOf(p.getApitype());

                String apiUrl = p.getUrl();
                String jsonResponse = null;
                JSONObject jsonObject = null;
                long connectedClients = 0;
                boolean noerrors = true;
                
                try {
                    switch (et) {

                        case BANCHOPY:
                            jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                            jsonObject = parseJsonResponse(jsonResponse);

                            JSONObject result = (JSONObject) jsonObject.get("counts");

                            connectedClients = (long) result.get("online");

                            Long registeredPlayers = (long) result.get("total");

                            v.setPlayers((int) connectedClients);
                            dp.FinishPrint(true);
                            nc.updateRegisteredPlayerCount(v, registeredPlayers.intValue());
                            nc.updateExtraBanchoPyStats(v);
                            break;
                        case LISEKAPI:
                            jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                            jsonObject = parseJsonResponse(jsonResponse);

                            connectedClients = (long) jsonObject.get("online");
                            Long totalPlayers = (long) jsonObject.get("users");

                            v.setPlayers((int) connectedClients);
                            dp.FinishPrint(true);

                            nc.updateRegisteredPlayerCount(v, totalPlayers.intValue());

                            
                            break;
                        case RIPPLEAPIV1:
                            jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                            jsonObject = parseJsonResponse(jsonResponse);

                            connectedClients = (long) jsonObject.get("result");

                            v.setPlayers((int) connectedClients);
                            dp.FinishPrint(true);
                            break;
                        case RIPPLEAPIV2:
                        jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                            jsonObject = parseJsonResponse(jsonResponse);

                            connectedClients = (long) jsonObject.get("connected_clients");
                            v.setPlayers((int) connectedClients);
                            dp.FinishPrint(true);
                            break;

                        case GATARIAPI:
                            jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                            jsonObject = parseJsonResponse(jsonResponse);

                            JSONObject js = (JSONObject) jsonObject.get("result");
                            connectedClients = (long) js.get("online");

                            v.setPlayers((int) connectedClients);
                            dp.FinishPrint(true);

                            Long gatTotalPlayers = (long) js.get("users");
                            nc.updateRegisteredPlayerCount(v, gatTotalPlayers.intValue());

                            Long gatBannedPlayers = (long) js.get("banned");
                            nc.updateBannedPlayerCount(v, gatBannedPlayers.intValue());

                            
                    }
                } catch (Exception e) {
                    dp.FinishPrint(false);
                    noerrors = false;
                }

                DelayedPrint.PrintValue("VOTES", String.valueOf(v.getVotes()), noerrors);
                DelayedPrint.PrintValue("PLAYERS", String.valueOf(v.getPlayers()), noerrors);

                mysql.Exec("UPDATE `un_servers` SET `players`=? WHERE `id` = ?", String.valueOf(v.getPlayers()),
                        String.valueOf(v.getId()));

                nc.updatePlayerCount(v);

                updatePlayerStats(v);
              
            }
        } catch (Exception e) {
            mysql.close();
            ExceptionManager.addException(e);
            e.printStackTrace();
        }
        mysql.close();
    }

    private void updatePlayerStats(Server v) {
        LocalDate currentDate = LocalDate.now();
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
        int dayOfMonth = dayOfWeek.getValue();
        ResultSet checkRs = mysql.Query("SELECT * FROM `un_playerstats` WHERE `server_id` = ? AND `day` = ?",
                v.getId() + "", dayOfMonth + "");
        try {
            int i = 0;
            while (checkRs.next()) {
                i++;
                if (v.getPlayers() > checkRs.getInt("players")) {
                    mysql.Exec("UPDATE `un_playerstats` SET `players`=? WHERE `server_id` = ? AND `day` = ?",
                            String.valueOf(v.getPlayers()), String.valueOf(v.getId()), String.valueOf(dayOfMonth));
                }
            }
            if (i == 0) {
                mysql.Exec("INSERT INTO `un_playerstats`(`day`, `server_id`, `players`) VALUES (?,?,?)",
                        dayOfMonth + "", v.getId()
                                + "",
                        v.getPlayers() + "");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ExceptionManager.addException(e);
        }

    }

    
    public static JSONObject parseJsonResponse(String jsonResponse) throws Exception {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(jsonResponse);
    }

}
