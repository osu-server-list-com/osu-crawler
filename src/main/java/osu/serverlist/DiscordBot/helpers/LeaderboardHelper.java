package osu.serverlist.DiscordBot.helpers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import commons.marcandreher.Commons.GetRequest;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Leaderboard.LeaderboardInformations;
import osu.serverlist.Models.ServerInformations;

public class LeaderboardHelper {


    public class GotLeaderboard {
        public String leaderboard = "";
        public int size;
    }

    public GotLeaderboard getLeaderboardBanchoPy(int offset, LeaderboardInformations infos) throws Exception {
        GotLeaderboard gotLeaderboard = new GotLeaderboard();
        ServerInformations serverInformations = Leaderboard.endpoints.get(infos.server);

        String response = new GetRequest(serverInformations.getEndpoint() + "?sort=" + infos.sortId + "&mode=" + infos.modeId
        + "&limit=25&offset=" + (infos.offset) * 25).send("osu!ListBot");

          try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(response);
            JSONArray leaderboard = (JSONArray) jsonObject.get("leaderboard");

            int rank = infos.offset * 25;
            for (Object obj : leaderboard) {
                JSONObject player = (JSONObject) obj;
                rank++;
                String name = (String) player.get("name");
                long playerId = (long) player.get("player_id");
                String country = (String) player.get("country");
                String countryFlag = ":flag_" + country + ":";
                if(country.equalsIgnoreCase("XX")) countryFlag = ":flag_white:";
                long pp = (long) player.get("pp");
                double acc = (double) player.get("acc");
                long playtime = (long) player.get("playtime");
                double playtimeHr = Math.floor(playtime / 3600 * 100) / 100;
                
                gotLeaderboard.leaderboard += countryFlag + " [" + name + "](" + serverInformations.getUrl() + "/u/"
                        + playerId + ") #" + rank  + " (" + pp + "pp, " + acc + "%, " + playtimeHr + "h)" + "\n";
            }
            gotLeaderboard.size = leaderboard.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
        return gotLeaderboard;
    }
    
}
