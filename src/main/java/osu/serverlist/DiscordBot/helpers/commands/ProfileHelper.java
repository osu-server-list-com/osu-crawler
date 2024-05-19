package osu.serverlist.DiscordBot.helpers.commands;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.GetRequest;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.DiscordBot.helpers.ModeHelper;
import osu.serverlist.DiscordBot.helpers.exceptions.InvalidModeException;
import osu.serverlist.DiscordBot.helpers.exceptions.InvalidScorePlayerException;
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

        public boolean counts;
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

        profile.counts = true;
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

    public GotProfile getProfileRippleAPIV1(String name, String mode, String serverName)
            throws Exception, InvalidModeException, InvalidScorePlayerException {
        GotProfile profile = new GotProfile();
        ServerInformations serverInformations = Profile.endpoints.get(serverName);
        String rippleMode = ModeHelper.convertModeRippleAPI(mode);
        String rx = "";

        if (rippleMode == null) {
            throw new InvalidModeException("Invalid mode: " + mode + " for Server " + serverName);
        }

        if (rippleMode.length() > 1) {
            String[] modeSplit = rippleMode.split("|");
            mode = modeSplit[0];
            rx = modeSplit[2];
        } else {
            mode = rippleMode;
            rx = "0";
        }

        String url = serverInformations.getEndpoint() + "/full?name=" + name.replaceAll(" ", "_");
        Flogger.instance.log(Prefix.API, "GET: " + url, 0);
        String response = new GetRequest(url).send("osu!ListBot");

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response);

        profile.playerId = (Long) json.get("id");
        profile.username = (String) json.get("username");
        profile.country = (String) json.get("country");

        JSONArray stats = (JSONArray) json.get("stats");
        JSONObject modeObject;
        JSONObject modeStatsObject;
        try {
            modeObject = (JSONObject) stats.get(Integer.parseInt(rx));
            modeStatsObject = (JSONObject) modeObject.get(ModeHelper.convertModeForRippleAPIString(mode));
        } catch (Exception e) {
            throw new InvalidScorePlayerException(
                    "No score found for player " + name + " on mode " + mode + " for Server " + serverName);
        }

        profile.totalScore = (Long) modeStatsObject.get("total_score");
        profile.rankedScore = (Long) modeStatsObject.get("ranked_score");
        profile.pp = (Long) modeStatsObject.get("pp");
        profile.plays = (Long) modeStatsObject.get("playcount");
        
        profile.playtime = (Long) modeStatsObject.get("playtime");
        
        // osu!ascension fix
        if(profile.playtime == null)
            profile.playtime = (Long) modeStatsObject.get("play_time");
        

        profile.acc = (Double) modeStatsObject.get("accuracy");
        profile.maxCombo = (Long) modeStatsObject.get("max_combo");

        // rippleapi fix
        profile.counts = false;
        try {
            profile.rank = (Long) modeStatsObject.get("global_leaderboard_rank");
        }catch(Exception e) {
            profile.rank = 0L;
        }

        try {
            profile.countryRank = (Long) modeStatsObject.get("country_leaderboard_rank");
        }catch(Exception e) {
            profile.countryRank = 0L;
        }

        profile.totalHits = (Long) modeStatsObject.get("total_hits");
        profile.replayViews = (Long) modeStatsObject.get("replays_watched");

        return profile;
    }

}
