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

    public static String convertModeRippleAPI(String mode) {
        switch (mode) {
            case "osu":
                return "0";
            case "osurx":
                return "0|1";
            case "osuap":
                return "0|2";
            case "taiko":
                return "3";
            case "catch":
                return "4";
            case "mania":
                return "5";
            case "taikorx":
                return null;
            case "catchrx":
                return null;
            default:
                return null;
        }
    }

    public class SortHelper {
        // Available values : tscore, rscore, pp, acc, plays, playtime
        public static String[] sortArray = { "PP", "ACC", "Total Score", "Rated Score", "Plays", "Playtime" };

        public static String convertSort(String sort) {
            switch (sort.toLowerCase()) {
                case "total score":
                    return "tscore";
                case "rated score":
                    return "rscore";
                case "pp":
                    return "pp";
                case "acc":
                    return "acc";
                case "plays":
                    return "plays";
                case "playtime":
                    return "playtime";
                default:
                    return null;
            }
        }
    }
}
