package osu.serverlist.DiscordBot.helpers;

import java.util.ArrayList;

public class OsuConverter {

    public static String convertFlag(String countryCode) {
        switch (countryCode.toLowerCase()) {
            case "xx":
                return ":flag_white:";
            case "id":
                return ":flag_white:";
            case "fi":
                return ":flag_white:";
            case "ma":
                return ":flag_white:";
            case "xk":
                return ":flag_white:";
            case "id_":
            return ":flag_white:";
            default:
                return ":flag_" + countryCode + ":";
        }
    }

    public static String convertGrade(String grade) {
        switch (grade) {
            case "XH":
                return "<:rankingXH:1239849494393126922>";
            case "X":
                return "<:rankingX:1239849492891697242>";
            case "SH":
                return "<:rankingSH:1239849497375277076>";
            case "S":
                return "<:rankingS:1239849495999807508>";
            case "A":
                return "<:rankingA:1239849498948407366>";
            case "B":
                return "<:rankingB:1239849500424667196>";
            case "C":
                return "<:rankingC:1239849501963849810>";
            case "D":
                return "<:rankingD:1239849503528583259>";
            case "F":
                return "F";
            default:
                return "Unknown";
        }
    }

    public static String convertStatus(String status) {
        switch (status) {
            case "0":
                return "Not submitted";
            case "1":
                return "Pending";
            case "2":
                return "<:mapRanked:1240314560259293204>";
            case "3":
                return "<:mapApproved:1240314561622573147>";
            case "4":
                return "Qualified";
            case "5":
                return "<:mapLoved:1240314558929961111>";
            default:
                return "Unknown";
        }
    }

    public class ModConverter {
        public static String[] convertMods(int mods) {
            ArrayList<String> modList = new ArrayList<>();
            if ((mods & 1) > 0) {
                modList.add("NF");
            }
            if ((mods & 2) > 0) {
                modList.add("EZ");
            }
            if ((mods & 4) > 0) {
                modList.add("TD");
            }
            if ((mods & 8) > 0) {
                modList.add("HD");
            }
            if ((mods & 16) > 0) {
                modList.add("HR");
            }
            if ((mods & 32) > 0) {
                modList.add("SD");
            }
            if ((mods & 64) > 0) {
                modList.add("DT");
            }
            if ((mods & 128) > 0) {
                modList.add("RX");
            }
            if ((mods & 256) > 0) {
                modList.add("HT");
            }
            if ((mods & 512) > 0) {
                modList.add("NC");
            }
            if ((mods & 1024) > 0) {
                modList.add("FL");
            }
            if ((mods & 2048) > 0) {
                modList.add("AT");
            }
            if ((mods & 4096) > 0) {
                modList.add("SO");
            }
            if ((mods & 8192) > 0) {
                modList.add("AP");
            }
            if ((mods & 16384) > 0) {
                modList.add("PF");
            }
            if ((mods & 32768) > 0) {
                modList.add("4K");
            }
            return modList.toArray(new String[modList.size()]);
        }
    }

}
