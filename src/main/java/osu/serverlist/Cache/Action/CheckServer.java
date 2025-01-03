package osu.serverlist.Cache.Action;

import java.sql.ResultSet;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.GetRequest;
import commons.marcandreher.Utils.DelayedPrint;
import osu.serverlist.Cache.Action.Helpers.CrawlerType;
import osu.serverlist.Cache.Action.Helpers.NewCrawler;
import osu.serverlist.Handlers.AlertHandler;
import osu.serverlist.Input.Commands.Crawlerlog;
import osu.serverlist.Input.Commands.ExceptionManager;
import osu.serverlist.Models.Server;
import osu.serverlist.Utils.Endpoints.Endpoint;
import osu.serverlist.Utils.Endpoints.EndpointHandler;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class CheckServer extends DatabaseAction {

    private String lastDescSend = "";

    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);

        String sql = "SELECT * FROM `un_servers`";

        try {
            ResultSet serverCache = mysql.Query(sql);
            while (serverCache.next()) {
                Server v = new Server();
                v.setId(serverCache.getInt("id"));
                v.setName(serverCache.getString("name"));
                v.setVotes(serverCache.getInt("votes"));

                v.setUrl(serverCache.getString("url"));

                DelayedPrint dp;

                if (Crawlerlog.enabled) {
                    dp = new DelayedPrint(v.getName(), Prefix.ACTION);
                } else {
                    dp = null;
                }

                NewCrawler nc = new NewCrawler(mysql);
                try {

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
                                if (dp != null)
                                    dp.FinishPrint(true);
                                nc.updateRegisteredPlayerCount(v, registeredPlayers.intValue());
                                nc.updateExtraBanchoPyStats(v);
                                nc.updateExtraOkayuAPI(v);
                                nc.setOnline(v);
                                break;
                            case LISEKAPI:
                                jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                                jsonObject = parseJsonResponse(jsonResponse);

                                JSONObject data = (JSONObject) jsonObject.get("data");

                                connectedClients = (long) data.get("online");
                                Long totalPlayers = (long) data.get("users");

                                v.setPlayers((int) connectedClients);
                                if (dp != null)
                                    dp.FinishPrint(true);
                                nc.setOnline(v);
                                nc.updateRegisteredPlayerCount(v, totalPlayers.intValue());

                                break;
                            case RIPPLEAPIV1:
                                jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                                jsonObject = parseJsonResponse(jsonResponse);

                                connectedClients = (long) jsonObject.get("result");

                                v.setPlayers((int) connectedClients);
                                if (dp != null)
                                    dp.FinishPrint(true);
                                nc.setOnline(v);
                                break;
                            case RIPPLEAPIV2:
                                jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                                jsonObject = parseJsonResponse(jsonResponse);

                                connectedClients = (long) jsonObject.get("connected_clients");
                                v.setPlayers((int) connectedClients);
                                if (dp != null)
                                    dp.FinishPrint(true);
                                nc.setOnline(v);
                                break;

                            case GATARIAPI:
                                jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                                jsonObject = parseJsonResponse(jsonResponse);

                                JSONObject js = (JSONObject) jsonObject.get("result");
                                connectedClients = (long) js.get("online");

                                v.setPlayers((int) connectedClients);
                                if (dp != null)
                                    dp.FinishPrint(true);

                                Long gatTotalPlayers = (long) js.get("users");
                                nc.updateRegisteredPlayerCount(v, gatTotalPlayers.intValue());

                                Long gatBannedPlayers = (long) js.get("banned");
                                nc.updateBannedPlayerCount(v, gatBannedPlayers.intValue());
                                nc.setOnline(v);
                                break;
                            case HEIAAPI:
                                jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                                jsonObject = parseJsonResponse(jsonResponse);

                                JSONObject heiaData = (JSONObject) jsonObject.get("data");

                                JSONObject heiaDataUsers = (JSONObject) heiaData.get("users");

                                int heiaOnline = Integer.parseInt(heiaDataUsers.get("online").toString());
                                int heiaRegistered = Integer.parseInt(heiaDataUsers.get("total").toString());
                                int heiaBanned = Integer.parseInt(heiaDataUsers.get("restricted").toString());

                                v.setPlayers(heiaOnline);
                                if (dp != null)
                                    dp.FinishPrint(true);

                                nc.updateBannedPlayerCount(v, heiaBanned);

                                nc.updateRegisteredPlayerCount(v, heiaRegistered);

                                JSONObject heiaDataSquads = (JSONObject) heiaData.get("squads");

                                int heiaSquads = Integer.parseInt(heiaDataSquads.get("total").toString());

                                nc.updateAnyCount(CrawlerType.CLANS, v, heiaSquads);

                                JSONObject heiaDataMaps = (JSONObject) heiaData.get("beatmaps");

                                int heiaMaps = Integer.parseInt(heiaDataMaps.get("total").toString());

                                nc.updateAnyCount(CrawlerType.MAPS, v, heiaMaps);

                                JSONObject heiaDataPlays = (JSONObject) heiaData.get("scores");

                                int heiaPlays = Integer.parseInt(heiaDataPlays.get("total").toString());

                                nc.updateAnyCount(CrawlerType.PLAYS, v, heiaPlays);
                                nc.setOnline(v);
                                break;

                            case ICEBERG:
                                jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                                jsonObject = parseJsonResponse(jsonResponse);

                                int iceScores = Integer.parseInt(jsonObject.get("total_scores").toString());
                                int iceUsers = Integer.parseInt(jsonObject.get("total_users").toString());
                                int iceOnline = Integer.parseInt(jsonObject.get("online_users").toString());

                                v.setPlayers(iceOnline);

                                if (dp != null)
                                dp.FinishPrint(true);

                                nc.updateRegisteredPlayerCount(v, iceUsers);

                                nc.updateAnyCount(CrawlerType.PLAYS, v, iceScores);

                                nc.setOnline(v);

                                break;

                            case RAGNAROKAPI:
                                jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                                jsonObject = parseJsonResponse(jsonResponse);

                                int ragOnline = Integer.parseInt(jsonObject.get("online_players").toString());
                                int ragUsers = Integer.parseInt(jsonObject.get("registered_players").toString());
                                int ragScores = Integer.parseInt(jsonObject.get("total_scores").toString());

                                v.setPlayers(ragOnline);

                                if (dp != null)
                                dp.FinishPrint(true);

                                nc.updateRegisteredPlayerCount(v, ragUsers);
                                nc.updateAnyCount(CrawlerType.PLAYS, v, ragScores);
                                nc.setOnline(v);

                                break;

                                case SUNRISE:
                                jsonResponse = new GetRequest(apiUrl).send("osu!ListBot");
                                jsonObject = parseJsonResponse(jsonResponse);

                                int sunOnline = Integer.parseInt(jsonObject.get("users_online").toString());
                                int sunUsers = Integer.parseInt(jsonObject.get("total_users").toString());
                                int sunScores = Integer.parseInt(jsonObject.get("total_scores").toString());

                                v.setPlayers(sunOnline);

                                if (dp != null)
                                dp.FinishPrint(true);

                                nc.updateRegisteredPlayerCount(v, sunUsers);
                                nc.updateAnyCount(CrawlerType.PLAYS, v, sunScores);
                                nc.setOnline(v);
                                
                                break;

                        }
                    } catch (Exception e) {
                        nc.setOffline(v);
                        if (dp != null)
                            dp.FinishPrint(false);
                        noerrors = false;
                    }

                    if (dp != null) {
                        DelayedPrint.PrintValue("VOTES", String.valueOf(v.getVotes()), noerrors);
                        DelayedPrint.PrintValue("PLAYERS", String.valueOf(v.getPlayers()), noerrors);
                    }
                    mysql.Exec("UPDATE `un_servers` SET `players`=? WHERE `id` = ?", String.valueOf(v.getPlayers()),
                            String.valueOf(v.getId()));

                    if(v.getPlayers() >= 1000) {
                        AlertHandler ah = new AlertHandler(mysql);
                        String newDesc = lastDescSend = v.getName() + " has reached " + v.getPlayers() + " players";
                        if(!lastDescSend.equals(newDesc)) {
                            ah.createSystemAlert("Suspicous playercount detected", newDesc);
                            lastDescSend = newDesc;
                        }
                        
                    }

                    nc.updatePlayerCount(v);

                } catch (Exception e) {
                    if (dp != null)
                        dp.FinishPrint(false);

                }
            }

        } catch (Exception e) {

            ExceptionManager.addException(e);
            e.printStackTrace();
        }

    }

    public static JSONObject parseJsonResponse(String jsonResponse) throws Exception {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(jsonResponse);
    }

}
