package osu.serverlist.DiscordBot.helpers.commands;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.GetRequest;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.Models.ServerInformations;

public class ProfileHelper {

    public class GotProfile {

        public Long playerId;
        public String username;
        public String country;

        public Long totalScore;
        public Long rankedScore;
        public Long pp;
        public Long playtime;
        public Long plays;

        public double acc;
        public Long maxCombo;

        public Long XHCount;
        public Long XCount;
        public Long SHCount;
        public Long SCount;
        public Long ACount;

        public Long totalHits;
        public Long replayViews;

        public Long rank;
        public Long countryRank;

    }

    public GotProfile getProfileBanchoPy(String name, String mode, String serverName) throws Exception {
        GotProfile profile = new GotProfile();
        ServerInformations serverInformations = Profile.endpoints.get(serverName);

        String url = serverInformations.getEndpoint() + "?name=" + name.replaceAll(" ", "_") + "&scope=all";
        String response = new GetRequest(url).send("osu!ListBot");

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response);
        JSONObject player = (JSONObject) json.get("player");
        JSONObject info = (JSONObject) player.get("info");

        profile.playerId = (Long) info.get("id");
        profile.username = (String) info.get("name");
        profile.country = (String) info.get("country");

        JSONObject stats = (JSONObject) player.get("stats");
        JSONObject modeObject = (JSONObject) stats.get(mode);

        profile.totalScore = (Long) modeObject.get("tscore");
        profile.rankedScore = (Long) modeObject.get("rscore");
        profile.pp = (Long) modeObject.get("pp");
        profile.plays = (Long) modeObject.get("plays");
        profile.playtime = (Long) modeObject.get("playtime");

        profile.acc = (Double) modeObject.get("acc");
        profile.maxCombo = (Long) modeObject.get("max_combo");

        profile.XHCount = (Long) modeObject.get("xh_count");
        profile.XCount = (Long) modeObject.get("x_count");
        profile.SHCount = (Long) modeObject.get("sh_count");
        profile.SCount = (Long) modeObject.get("s_count");
        profile.ACount = (Long) modeObject.get("a_count");

        profile.rank = (Long) modeObject.get("rank");
        profile.countryRank = (Long) modeObject.get("country_rank");
        profile.totalHits = (Long) modeObject.get("total_hits");
        profile.replayViews = (Long) modeObject.get("replay_views");

        return profile;
    }

}
