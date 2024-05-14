package osu.serverlist.DiscordBot.helpers;

public class ModeHelper {

    public static String[] modeArray = { "osu", "osurx", "osuap", "taiko", "catch", "mania", "taikorx", "catchrx" };

    public static String convertMode(String mode) {
        switch (mode) {
            case "osu":
                return "0";
            case "osurx":
                return "4";
            case "osuap":
                return "8";
            case "taiko":
                return "1";
            case "catch":
                return "2";
            case "mania":
                return "3";
            case "taikorx":
                return "5";
            case "catchrx":
                return "6";
            default:
                return null;
        }
        
    }

    public class SortHelper {
        //Available values : tscore, rscore, pp, acc, plays, playtime
        public static String[] sortArray = { "Total Score", "Rated Score", "PP", "ACC", "Plays", "Playtime" };

        public static String convertSort(String sort) {
            switch (sort) {
                case "Total Score":
                    return "tscore";
                case "Rated Score":
                    return "rscore";
                case "PP":
                    return "pp";
                case "ACC":
                    return "acc";
                case "Plays":
                    return "plays";
                case "Playtime":
                    return "playtime";
                default:
                    return null;
            }
        }
    }
}
