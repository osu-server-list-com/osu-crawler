package osu.serverlist.DiscordBot.helpers.commands;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.GetRequest;
import osu.serverlist.DiscordBot.commands.Best;
import osu.serverlist.DiscordBot.commands.Best.BestInformations;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.OsuConverter;
import osu.serverlist.Main.Crawler;
import osu.serverlist.Models.ServerInformations;

public class BestHelper {

    public class GotBest {
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

    public GotBest requestBestBanchoPy(BestInformations infos) throws Exception {
        GotBest gotBest = new GotBest();
        ServerInformations serverInformations = Best.endpoints.get(infos.server);

        String url = serverInformations.getEndpoint() + "?scope=best&mode=" + infos.modeId + "&name="
                + infos.name.replaceAll(" ", "_") + "&limit=55";
        Flogger.instance.log(Prefix.API, "GET: " + url, 0);
        String response = new GetRequest(url).send("osu!ListBot");

        // Parse JSON
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(response);

        JSONObject playerObj = (JSONObject) jsonObject.get("player");
        gotBest.userId = (long) playerObj.get("id");

        JSONArray scores = (JSONArray) jsonObject.get("scores");
        JSONObject scoreObj = (JSONObject) scores.get(infos.offset);
        gotBest.size = scores.size();

        // Parsing score object
        gotBest.scoreId = (long) scoreObj.get("id");
        gotBest.score = (long) scoreObj.get("score");
        gotBest.pp = (double) scoreObj.get("pp");
        gotBest.acc = (double) scoreObj.get("acc");
        gotBest.grade = (String) scoreObj.get("grade");
        gotBest.status = (long) scoreObj.get("status");
        gotBest.mods = (long) scoreObj.get("mods");

        gotBest.playtime = (String) scoreObj.get("play_time");

        // Parsing beatmap object
        JSONObject beatmapObj = (JSONObject) scoreObj.get("beatmap");
        gotBest.mapId = (long) beatmapObj.get("id");
        gotBest.setId = (long) beatmapObj.get("set_id");
        gotBest.mapName = (String) beatmapObj.get("title");
        gotBest.mapArtist = (String) beatmapObj.get("artist");
        gotBest.creator = (String) beatmapObj.get("creator");
        gotBest.diff = (double) beatmapObj.get("diff");

        gotBest.ar = (double) beatmapObj.get("ar");
        gotBest.bpm = (double) beatmapObj.get("bpm");
        gotBest.od = (double) beatmapObj.get("od");

        return gotBest;
    }

    public GotBest requestBestRippleAPIV1(BestInformations infos) throws Exception {
        GotBest gotBest = new GotBest();
        ServerInformations serverInformations = Best.endpoints.get(infos.server);
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
        gotBest.size = scoresArray.size();
        Object scoreId = curScore.get("id");
        long scoreIdL = 0L;
        if (scoreId instanceof String) {
            scoreIdL = Long.parseLong((String) scoreId);
        } else {
            scoreIdL = (long) scoreId;
        }
        // Parsing score object
        gotBest.scoreId = scoreIdL;
        gotBest.score = (long) curScore.get("score");
        gotBest.pp = ((Number) curScore.get("pp")).doubleValue();
        gotBest.acc = ((Number) curScore.get("accuracy")).doubleValue();
        gotBest.mods = (long) curScore.get("mods");
        gotBest.grade = (String) curScore.get("rank");

        gotBest.playtime = (String) curScore.get("time");

        JSONObject beatmap = (JSONObject) curScore.get("beatmap");

        // uid ascension
        try {
            JSONObject user = (JSONObject) curScore.get("user");
            gotBest.userId = (long) user.get("user_id");
        } catch (Exception e) {
            gotBest.userId = 0;
        }

        gotBest.setId = (long) beatmap.get("beatmapset_id");
        gotBest.mapId = (long) beatmap.get("beatmap_id");
        gotBest.status = (long) beatmap.get("ranked");
        gotBest.mapName = (String) beatmap.get("song_name");
        gotBest.creator = null;
        gotBest.mapArtist = null;
        gotBest.diff = Double.parseDouble(beatmap.get("difficulty").toString());
        gotBest.ar = Double.parseDouble(beatmap.get("ar").toString());
        gotBest.bpm = 0.0;
        gotBest.od = Double.parseDouble(beatmap.get("od").toString());

        return gotBest;
    }

    public GotBest requestBestBancho(BestInformations infos) throws Exception {
        GotBest gotBest = new GotBest();
        ServerInformations serverInformations = Best.endpoints.get(infos.server);
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
        gotBest.size = scoreArray.size();
        gotBest.score = Long.parseLong((String) scoreObj.get("score"));
        gotBest.grade = (String) scoreObj.get("rank");
        gotBest.mods = Long.parseLong((String) scoreObj.get("enabled_mods"));
        gotBest.mapId = Long.parseLong((String) scoreObj.get("beatmap_id"));
        gotBest.userId = Long.parseLong((String) scoreObj.get("user_id"));
        if((String) scoreObj.get("score_id") != null) {
            gotBest.scoreId = Long.parseLong((String) scoreObj.get("score_id"));
        } else {
            gotBest.scoreId = 0;
        }
        String beatmapUrl = "https://osu.ppy.sh/api/get_beatmaps" + "?k=" + Crawler.env.get("OSU_API_KEY") + "&b=" + gotBest.mapId;
        Flogger.instance.log(Prefix.API, "GET: " + beatmapUrl, 0);
        String beatmapResponse = new GetRequest(beatmapUrl).send("osu!ListBot");

        JSONArray beatmapArray = (JSONArray) parser.parse(beatmapResponse);
        JSONObject beatmapObj = (JSONObject) beatmapArray.get(0);
        gotBest.mapName = (String) beatmapObj.get("title");
        gotBest.setId  = Long.parseLong((String) beatmapObj.get("beatmapset_id"));
        gotBest.mapArtist = (String) beatmapObj.get("artist");
        gotBest.creator = (String) beatmapObj.get("creator");
        gotBest.diff = Double.parseDouble(beatmapObj.get("difficultyrating").toString());
        gotBest.ar = Double.parseDouble(beatmapObj.get("diff_approach").toString());
        gotBest.bpm = Double.parseDouble(beatmapObj.get("bpm").toString());
        gotBest.od = Double.parseDouble(beatmapObj.get("diff_overall").toString());
        gotBest.status = Long.parseLong((String) beatmapObj.get("approved"));

        Object ppObj = scoreObj.get("pp");
        if(ppObj != null) {
            gotBest.pp = Double.parseDouble((String) ppObj);
        } else {
            gotBest.pp = 0.0;
        }

        if(((String) scoreObj.get("perfect")).equals("1")) {
            gotBest.acc = 100.0;
        }


        return gotBest;
    }

    public String convertDescription(GotBest gotBest, String nameW, BestInformations infos) {
        String description = OsuConverter.convertStatus(String.valueOf(gotBest.status)) + " ▪ "
                + OsuConverter.convertGrade(gotBest.grade) + " ▪ [" + (nameW) + "]";

        if (gotBest.userId != 0) {
            description += "(" + Best.endpoints.get(infos.server).getUrl() + "/u/" + gotBest.userId + ")";
        }

        description += " on \n";

        if (gotBest.mapArtist != null) {
            description += "[" + gotBest.mapArtist + " | " + gotBest.mapName + "]";
        } else {
            description += "[" + gotBest.mapName + "]";
        }

        description += "(" + Best.endpoints.get(infos.server).getUrl()
                + "/b/" + gotBest.mapId + ")\n";

        if (gotBest.creator != null) {
            description += "Map by " + gotBest.creator;
        }
        return description;
    }
}
