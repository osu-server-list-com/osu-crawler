package osu.serverlist.DiscordBot.helpers;

public class ModeHelper {

    public static String[] modeArray = { "OSU", "OSURX", "OSUAP", "TAIKO", "CATCH", "MANIA", "TAIKORX", "CATCHRX" };

    public static String convertMode(String mode) {
        switch (mode.toLowerCase()) {
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

    public static String convertModeBancho(String mode) {
        switch(mode.toLowerCase()) {
            case "osu":
                return "0";
            case "taiko":
                return "1";
            case "catch":
                return "2";
            case "mania":
                return "3";
            default:
                return null;
        }
    }

    public static String convertModeRippleAPI(String mode) {
        switch (mode.toLowerCase()) {
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

    public static String convertModeForRippleAPIString(String mode) {
        switch (mode) {
            case "0":
                return "std";
            case "1":
                return "taiko";
            case "2":
                return "catch";
            case "3":
                return "mania";
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
