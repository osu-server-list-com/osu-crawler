package osu.serverlist.cache.action;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedField;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedFooter;
import club.minnced.discord.webhook.send.WebhookEmbed.EmbedTitle;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Utils.Color;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import osu.serverlist.Main.Crawler;
import osu.serverlist.Models.Server;
import osu.serverlist.Utils.Endpoints.Endpoint;
import osu.serverlist.Utils.Endpoints.EndpointHandler;
import osu.serverlist.Utils.Endpoints.EndpointType;
import osu.serverlist.Utils.Endpoints.ServerEndpoints;

public class CrawlerAction extends DatabaseAction {

    private static final String BASE_SQL = "SELECT * FROM `un_servers` WHERE `locked` = 0";
    private final OkHttpClient client = new OkHttpClient();
    private static final String USER_AGENT = "osu!ListBot";
    private static final long INCIDENT_COOLDOWN = TimeUnit.HOURS.toMillis(4);
    private final CrawlerLog crawlerLog = new CrawlerLog();
    private Flogger logger;
    private final Map<Server, Long> incidentCooldownMap = new HashMap<>();
    private final List<Server> incidentServerList = new ArrayList<>();


    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);
        this.logger = logger;
        crawlerLog.logln("Starting Crawler v2.0");

        try {
            ResultSet crawlerResultSet = mysql.Query(BASE_SQL);
            while (crawlerResultSet.next()) {
                Server server = populateServer(crawlerResultSet);
                Incident incident = prepareCrawlServer(server);

                if (incident != null) {
                    CrawlerDump.setServerOffline(mysql, server);
                    handleIncident(server, incident);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void handleIncident(Server server, Incident incident) {
        long currentTime = System.currentTimeMillis();
        long lastIncidentTime = incidentCooldownMap.getOrDefault(server, 0L);
        if (currentTime - lastIncidentTime >= INCIDENT_COOLDOWN) {

            crawlerLog.logln("Incident: " + incident.getMessage() + " (" + Color.RED + incident.getResponseCode()
                    + Color.RESET + ")");
            if (incident.getUrl() != null) {
                crawlerLog.logln("at -> " + Color.RED + incident.getUrl());
            }
            crawlerLog.logln("time -> " + incident.getTime());

            if(incident.getUrl() == null) {
                mysql.Exec("INSERT INTO `un_incidents`(`message`, `response_code`) VALUES (?,?);", incident.getMessage(), String.valueOf(incident.getResponseCode()));
            }else {
                mysql.Exec("INSERT INTO `un_incidents`(`message`, `response_code`, `url`) VALUES (?,?,?);", incident.getMessage(), String.valueOf(incident.getResponseCode()), incident.getUrl());
            }

            incidentServerList.add(server);

            String incidentsWebhook = Crawler.env.get("INCIDENTS_WEBHOOK");
            if (!incidentsWebhook.isEmpty()) {
                sendWebhookNotification(server, incident, incidentsWebhook);
            }

            incidentCooldownMap.put(server, currentTime);
        }else {
            crawlerLog.logln("Incident suppressed for " + server.getName() + " (cooldown active)");
        }

    }

    private void sendWebhookNotification(Server server, Incident incident, String webhookUrl) {
        WebhookClientBuilder builder = new WebhookClientBuilder(webhookUrl);
        builder.setWait(true);
        WebhookClient client = builder.build();

        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                .setColor(0xFF0000)
                .setTitle(new EmbedTitle(incident.getMessage(), ""))
                .setDescription("Server: " + server.getName() + " (" + server.getId() + ")")
                .addField(new EmbedField(true, "Response Code", String.valueOf(incident.getResponseCode())))
                .addField(new EmbedField(true, "Time", incident.getTime()))
                .setThumbnailUrl("https://osu-server-list.com/" + server.getLogo_loc())
                .setFooter(new EmbedFooter("osu-server-list.com - Crawler v.2.0", null));

        if (incident.getUrl() != null) {
            embed.addField(new EmbedField(false, "URL", incident.getUrl()));
        }

        WebhookEmbed builtEmbed = embed.build();
        client.send(builtEmbed).join();
        client.close();
    }

    private void sendResolutionWebhook(Server server) {
        String incidentsWebhook = Crawler.env.get("INCIDENTS_WEBHOOK");
        if (incidentsWebhook.isEmpty()) {
            return;
        }
    
        WebhookClientBuilder builder = new WebhookClientBuilder(incidentsWebhook);
        builder.setWait(true);
        WebhookClient client = builder.build();
    
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                .setColor(0x00FF00) 
                .setTitle(new EmbedTitle("Incident Resolved", ""))
                .setDescription("Server: " + server.getName() + " (" + server.getId() + ") is now crawlable again.")
                .setThumbnailUrl("https://osu-server-list.com/" + server.getLogo_loc())
                .setFooter(new EmbedFooter("osu-server-list.com - Crawler v.2.0", null));
    
        WebhookEmbed builtEmbed = embed.build();
        client.send(builtEmbed).join();
        client.close();
    }
    

    public Server populateServer(ResultSet crawlerResultSet) {
        Server server = new Server();
        try {
            server.setName(crawlerResultSet.getString("name"));
            server.setId(crawlerResultSet.getInt("id"));
            server.setLogo_loc(crawlerResultSet.getString("logo_loc"));
            server.setPlayers(crawlerResultSet.getInt("players"));
            server.setVotes(crawlerResultSet.getInt("votes"));
            server.setCreated(crawlerResultSet.getString("created"));
            server.setUrl(crawlerResultSet.getString("url"));
            server.setSafe_name(server.getName().toLowerCase().replaceAll(" ", ""));
            server.setApiKey(crawlerResultSet.getString("apikey"));
        } catch (Exception e) {
            logger.error(e);
        }
        return server;
    }

    public Incident prepareCrawlServer(Server server) {
        try {
            EndpointHandler endpointHandler = new EndpointHandler(mysql);
            Endpoint endpoint = getEndpoint(endpointHandler, server);
            EndpointType endpointType = EndpointType.valueOf(endpoint.getApitype());

            String apiUrl = endpoint.getUrl();

            if (endpointType == null) {
                return createIncident("No endpoint type found for server: " + server.getName(), 500, null);
            }

            if (apiUrl == null || apiUrl.isEmpty()) {
                return createIncident("No API URL found for endpoint: " + endpoint.getId(), 500, null);
            }

            return crawlServer(server, endpointType, apiUrl);
        } catch (Exception e) {
            return createIncident("Failed to get Endpoints of server: " + server.getName(), 500, null);
        }
    }

    private Incident createIncident(String message, int responseCode, String url) {
        Incident incident = new Incident();
        incident.setMessage(message);
        incident.setResponseCode(responseCode);
        incident.setTime(getCurrentTimestamp());
        incident.setUrl(url);
        crawlerLog.fail();
        return incident;
    }

    public Incident crawlServer(Server server, EndpointType endpointType, String apiUrl) {
        Request request = new Request.Builder().url(apiUrl).header("User-Agent", USER_AGENT).build();
        crawlerLog.log("Crawling " + server.getName());

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return createIncident("Failed to crawl server: " + server.getName(), response.code(), apiUrl);
            }

            if (!incidentCooldownMap.containsKey(server) && incidentServerList.contains(server)) {
                sendResolutionWebhook(server);
                incidentServerList.remove(server);
            }    

            String responseBody = response.body().string();
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

            Integer clients = null;
            Integer registered = null;
            Integer maps = null;
            Integer clans = null;
            Integer plays = null;
            Integer banned = null;

            switch (endpointType) {
                case BANCHOPY:
                    JsonElement countsElement = jsonObject.get("counts");
                    clients = countsElement.getAsJsonObject().get("online").getAsInt();
                    registered = countsElement.getAsJsonObject().get("total").getAsInt();

                    String url = Crawler.env.get("LOCALHOST").length() > 2
                            ? Crawler.env.get("LOCALHOST") + "/api/v1/banchopy/stats?id=" + server.getId()
                            : Crawler.env.get("DOMAIN") + "/api/v1/banchopy/stats?id=" + server.getId();

                    EndpointHandler endpointHandler = new EndpointHandler(mysql);
                    Endpoint endpoint = endpointHandler.getEndpoint(server, ServerEndpoints.CUSTOM);
                    if (endpoint.getUrl() != null) {
                        Request extraBpyRequest = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
                        try (Response extraBpyResponse = client.newCall(extraBpyRequest).execute()) {
                            if (!extraBpyResponse.isSuccessful()) {
                                return createIncident("Failed to get extra bpy stats for: " + server.getName(),
                                        extraBpyResponse.code(), url);
                            }

                            String extraBpyResponseBody = extraBpyResponse.body().string();
                            JsonObject extraBpyJsonObject = JsonParser.parseString(extraBpyResponseBody)
                                    .getAsJsonObject();

                            maps = extraBpyJsonObject.get("maps").getAsInt();
                            clans = extraBpyJsonObject.get("clans").getAsInt();
                            plays = extraBpyJsonObject.get("plays").getAsInt();
                        } catch (Exception e) {
                            return createIncident("Failed to get extra bpy stats for:: " + server.getName(), 500, null);
                        }
                    }
                    break;

                case LISEKAPI:
                    JsonElement dataElement = jsonObject.get("data");
                    clients = dataElement.getAsJsonObject().get("online").getAsInt();
                    registered = dataElement.getAsJsonObject().get("users").getAsInt();
                    
                    break;

                case RIPPLEAPIV1:
                    clients = jsonObject.get("result").getAsInt();

                    break;

                case RIPPLEAPIV2:
                    clients = jsonObject.get("connected_clients").getAsInt();

                    break;

                case GATARIAPI:
                    JsonElement resultElement = jsonObject.get("result");
                    clients = resultElement.getAsJsonObject().get("online").getAsInt();
                    registered = resultElement.getAsJsonObject().get("users").getAsInt();
                    banned = resultElement.getAsJsonObject().get("banned").getAsInt();

                   
                    break;

                case HEIAAPI:
                    JsonElement heiaData = jsonObject.get("data");
                    JsonElement heiaDataUsers = heiaData.getAsJsonObject().get("users");
                    clients = heiaDataUsers.getAsJsonObject().get("online").getAsInt();
                    registered = heiaDataUsers.getAsJsonObject().get("total").getAsInt();
                    banned = heiaDataUsers.getAsJsonObject().get("restricted").getAsInt();

                    clans = heiaData.getAsJsonObject().get("squads").getAsJsonObject().get("total").getAsInt();
                    maps = heiaData.getAsJsonObject().get("beatmaps").getAsJsonObject().get("total").getAsInt();
                    plays = heiaData.getAsJsonObject().get("scores").getAsJsonObject().get("total").getAsInt();
                    break;

                case ICEBERG:
                    plays = jsonObject.get("total_scores").getAsInt();
                    registered = jsonObject.get("total_users").getAsInt();
                    clients = jsonObject.get("online_users").getAsInt();
                    break;

                case RAGNAROKAPI:
                    clients = jsonObject.get("online_players").getAsInt();
                    registered = jsonObject.get("registered_players").getAsInt();
                    plays = jsonObject.get("total_scores").getAsInt();
                    break;

                case SUNRISE:
                    clients = jsonObject.get("users_online").getAsInt();
                    registered = jsonObject.get("total_users").getAsInt();
                    plays = jsonObject.get("total_scores").getAsInt();
                    break;

                default:
                    break;
            }

            crawlerLog.success();
            CrawlerDump dump = new CrawlerDump(mysql, server);
            dump.updateVotes();
            dump.updateApiKey();
            if (clients != null) {
                server.setPlayers(clients);
                crawlerLog.logStat("PLAYERS", clients);
                dump.dumpStat("PLAYERCHECK", CrawlerType.PLAYERCHECK, clients);
                dump.updatePlayers(clients);
            }

            if (registered != null) {
                crawlerLog.logStat("REGISTERED", registered);
                dump.dumpStat("REGISTERED_PLAYERS", CrawlerType.REGISTERED_PLAYERS, registered);
            }

            if (maps != null) {
                crawlerLog.logStat("MAPS", maps);
                dump.dumpStat("MAPS", CrawlerType.MAPS, maps);
            }

            if (clans != null) {
                crawlerLog.logStat("CLANS", clans);
                dump.dumpStat("CLANS", CrawlerType.CLANS, clans);
            }

            if (plays != null) {
                crawlerLog.logStat("PLAYS", plays);
                dump.dumpStat("PLAYS", CrawlerType.PLAYS, plays);
            }

            if (banned != null) {
                crawlerLog.logStat("BANNED", banned);
                dump.dumpStat("BANNED_PLAYERS", CrawlerType.BANNED_PLAYERS, banned);
            }
        } catch (Exception e) {
            return createIncident("Failed to parse JSON for: " + server.getName(), 500, apiUrl);
        }

        return null;
    }

    public Endpoint getEndpoint(EndpointHandler endpointHandler, Server server) {
        try {
            return endpointHandler.getEndpoint(server, ServerEndpoints.PLAYERCHECK);
        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new java.util.Date());
    }
}