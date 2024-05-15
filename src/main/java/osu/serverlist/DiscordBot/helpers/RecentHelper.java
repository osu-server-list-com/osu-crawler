package osu.serverlist.DiscordBot.helpers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.GetRequest;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.DiscordBot.commands.Recent.RecentInformations;
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

        String url = serverInformations.getEndpoint() + "?scope=recent&mode=" + infos.modeId + "&name=" + infos.name.replaceAll(" ", "_") + "&limit=55";
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

}
