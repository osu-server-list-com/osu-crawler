package osu.serverlist.DiscordBot.helpers.commands;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.GetRequest;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.DiscordBot.commands.Recent.RecentInformations;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.OsuConverter;
import osu.serverlist.Main.Crawler;
import osu.serverlist.Models.ServerInformations;

public class RecentHelper {

    public class GotRecent {
        public long userId;

        public long scoreId;
        public long score;
        public double pp;
        public double acc;
        public String grade;
        public long status;
        public long mods;
        public String playtime;

        public long mapId;
        public long setId;
        public String mapName;
        public String mapArtist;
        public String creator;
        public double diff;

        public double ar;
        public double bpm;
        public double od;

        public int size;
    }

    public GotRecent requestRecentBanchoPy(RecentInformations infos) throws Exception {
        GotRecent gotRecent = new GotRecent();
        ServerInformations serverInformations = Recent.endpoints.get(infos.server);

        String url = serverInformations.getEndpoint() + "?scope=recent&mode=" + infos.modeId + "&name="
                + infos.name.replaceAll(" ", "_") + "&limit=55";
        Flogger.instance.log(Prefix.API, "GET: " + url, 0);
        String response = new GetRequest(url).send("osu!ListBot");

        // Parse JSON
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(response);

        JSONObject playerObj = (JSONObject) jsonObject.get("player");
        gotRecent.userId = (long) playerObj.get("id");

        JSONArray scores = (JSONArray) jsonObject.get("scores");
        JSONObject scoreObj = (JSONObject) scores.get(infos.offset);
        gotRecent.size = scores.size();

        // Parsing score object
        gotRecent.scoreId = (long) scoreObj.get("id");
        gotRecent.score = (long) scoreObj.get("score");
        gotRecent.pp = (double) scoreObj.get("pp");
        gotRecent.acc = (double) scoreObj.get("acc");
        gotRecent.grade = (String) scoreObj.get("grade");
        gotRecent.status = (long) scoreObj.get("status");
        gotRecent.mods = (long) scoreObj.get("mods");

        gotRecent.playtime = (String) scoreObj.get("play_time");

        // Parsing beatmap object
        JSONObject beatmapObj = (JSONObject) scoreObj.get("beatmap");
        gotRecent.mapId = (long) beatmapObj.get("id");
        gotRecent.setId = (long) beatmapObj.get("set_id");
        gotRecent.mapName = (String) beatmapObj.get("title");
        gotRecent.mapArtist = (String) beatmapObj.get("artist");
        gotRecent.creator = (String) beatmapObj.get("creator");
        gotRecent.diff = (double) beatmapObj.get("diff");

        gotRecent.ar = (double) beatmapObj.get("ar");
        gotRecent.bpm = (double) beatmapObj.get("bpm");
        gotRecent.od = (double) beatmapObj.get("od");

        return gotRecent;
    }

    public GotRecent requestRecentRippleAPIV1(RecentInformations infos) throws Exception {
        GotRecent gotRecent = new GotRecent();
        ServerInformations serverInformations = Recent.endpoints.get(infos.server);
        String rippleAPIMode = ModeHelper.convertModeRippleAPI(infos.mode);
        String mode = "";
        String rx;

        if (rippleAPIMode == null) {
            return null;
        }

        if (rippleAPIMode.length() > 1) {
            String[] modeSplit = rippleAPIMode.split("|");
            mode = modeSplit[0];
            rx = modeSplit[2];
        } else {
            mode = rippleAPIMode;
            rx = "0";
        }

        String url = serverInformations.getEndpoint() + "?m=" + mode + "&p=1&l=55&rx=" + rx + "&name="
                + infos.name.replaceAll(" ", "_");
        Flogger.instance.log(Prefix.API, "GET: " + url, 0);
        String response = new GetRequest(url).send("osu!ListBot");

        // Parse JSON
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(response);

        JSONArray scoresArray = (JSONArray) jsonObject.get("scores");
        JSONObject curScore = (JSONObject) scoresArray.get(infos.offset);
        gotRecent.size = scoresArray.size();
        Object scoreId = curScore.get("id");
        long scoreIdL = 0L;
        if (scoreId instanceof String) {
            scoreIdL = Long.parseLong((String) scoreId);
        } else {
            scoreIdL = (long) scoreId;
        }
        // Parsing score object
        gotRecent.scoreId = scoreIdL;
        gotRecent.score = (long) curScore.get("score");
        gotRecent.pp = ((Number) curScore.get("pp")).doubleValue();
        gotRecent.acc = ((Number) curScore.get("accuracy")).doubleValue();
        gotRecent.mods = (long) curScore.get("mods");
        gotRecent.grade = (String) curScore.get("rank");

        gotRecent.playtime = (String) curScore.get("time");

        JSONObject beatmap = (JSONObject) curScore.get("beatmap");

        // uid ascension
        try {
            JSONObject user = (JSONObject) curScore.get("user");
            gotRecent.userId = (long) user.get("user_id");
        } catch (Exception e) {
            gotRecent.userId = 0;
        }

        gotRecent.setId = (long) beatmap.get("beatmapset_id");
        gotRecent.mapId = (long) beatmap.get("beatmap_id");
        gotRecent.status = (long) beatmap.get("ranked");
        gotRecent.mapName = (String) beatmap.get("song_name");
        gotRecent.creator = null;
        gotRecent.mapArtist = null;
        gotRecent.diff = Double.parseDouble(beatmap.get("difficulty").toString());
        gotRecent.ar = Double.parseDouble(beatmap.get("ar").toString());
        gotRecent.bpm = 0.0;
        gotRecent.od = Double.parseDouble(beatmap.get("od").toString());

        return gotRecent;
    }

    public GotRecent requestRecentBancho(RecentInformations infos) throws Exception {
        GotRecent gotRecent = new GotRecent();
        ServerInformations serverInformations = Recent.endpoints.get(infos.server);
        String url = serverInformations.getEndpoint() + "?k=" + Crawler.env.get("OSU_API_KEY") + "&limit=50&type=string&m=" + ModeHelper.convertModeBancho(infos.mode) + "&u="
                + infos.name.replaceAll(" ", "_");
        Flogger.instance.log(Prefix.API, "GET: " + url, 0);
        String response = new GetRequest(url).send("osu!ListBot");

        JSONParser parser = new JSONParser();
        JSONArray scoreArray = (JSONArray) parser.parse(response);
        if(scoreArray.size() == 0) {
            throw new Exception("No scores found");
        }

        JSONObject scoreObj = (JSONObject) scoreArray.get(infos.offset);
        gotRecent.size = scoreArray.size();
        gotRecent.score = Long.parseLong((String) scoreObj.get("score"));
        gotRecent.grade = (String) scoreObj.get("rank");
        gotRecent.mods = Long.parseLong((String) scoreObj.get("enabled_mods"));
        gotRecent.mapId = Long.parseLong((String) scoreObj.get("beatmap_id"));
        gotRecent.userId = Long.parseLong((String) scoreObj.get("user_id"));
        if((String) scoreObj.get("score_id") != null) {
            gotRecent.scoreId = Long.parseLong((String) scoreObj.get("score_id"));
        } else {
            gotRecent.scoreId = 0;
        }
        String beatmapUrl = "https://osu.ppy.sh/api/get_beatmaps" + "?k=" + Crawler.env.get("OSU_API_KEY") + "&b=" + gotRecent.mapId;
        Flogger.instance.log(Prefix.API, "GET: " + beatmapUrl, 0);
        String beatmapResponse = new GetRequest(beatmapUrl).send("osu!ListBot");

        JSONArray beatmapArray = (JSONArray) parser.parse(beatmapResponse);
        JSONObject beatmapObj = (JSONObject) beatmapArray.get(0);
        gotRecent.mapName = (String) beatmapObj.get("title");
        gotRecent.setId  = Long.parseLong((String) beatmapObj.get("beatmapset_id"));
        gotRecent.mapArtist = (String) beatmapObj.get("artist");
        gotRecent.creator = (String) beatmapObj.get("creator");
        gotRecent.diff = Double.parseDouble(beatmapObj.get("difficultyrating").toString());
        gotRecent.ar = Double.parseDouble(beatmapObj.get("diff_approach").toString());
        gotRecent.bpm = Double.parseDouble(beatmapObj.get("bpm").toString());
        gotRecent.od = Double.parseDouble(beatmapObj.get("diff_overall").toString());
        gotRecent.status = Long.parseLong((String) beatmapObj.get("approved"));

        Object ppObj = scoreObj.get("pp");
        if(ppObj != null) {
            gotRecent.pp = ((Number) ppObj).doubleValue();
        } else {
            gotRecent.pp = 0.0;
        }

        if(((String) scoreObj.get("perfect")).equals("1")) {
            gotRecent.acc = 100.0;
        }


        return gotRecent;
    }

    public String convertDescription(GotRecent gotRecent, String nameW, RecentInformations infos) {

  
        String description = OsuConverter.convertStatus(String.valueOf(gotRecent.status)) + " ▪ "
                + OsuConverter.convertGrade(gotRecent.grade) + " ▪ [" + (nameW) + "]";

        if (gotRecent.userId != 0) {
            description += "(" + Recent.endpoints.get(infos.server).getUrl() + "/u/" + gotRecent.userId + ")";
        }

        description += " on \n";

        if (gotRecent.mapArtist != null) {
            description += "[" + gotRecent.mapArtist + " | " + gotRecent.mapName + "]";
        } else {
            description += "[" + gotRecent.mapName + "]";
        }

        description += "(" + Recent.endpoints.get(infos.server).getUrl()
                + "/b/" + gotRecent.mapId + ")\n";

        if (gotRecent.creator != null) {
            description += "Map by " + gotRecent.creator;
        }
        return description;
    }
}
