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
            case "kr":
                return ":flag_white:";
            default:
                return ":flag_" + countryCode.toLowerCase() + ":";
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

    public static String[] convertMods(int mods) {
        ArrayList<String> modList = new ArrayList<>();

        if ((mods & Mods.NoFail.getValue()) > 0) {
            modList.add("NF");
        }
        if ((mods & Mods.Easy.getValue()) > 0) {
            modList.add("EZ");
        }
        if ((mods & Mods.TouchDevice.getValue()) > 0) {
            modList.add("TD");
        }
        if ((mods & Mods.Hidden.getValue()) > 0) {
            modList.add("HD");
        }
        if ((mods & Mods.HardRock.getValue()) > 0) {
            modList.add("HR");
        }
        if ((mods & Mods.SuddenDeath.getValue()) > 0) {
            modList.add("SD");
        }
        if ((mods & Mods.DoubleTime.getValue()) > 0) {
            modList.add("DT");
        }
        if ((mods & Mods.Relax.getValue()) > 0) {
            modList.add("RX");
        }
        if ((mods & Mods.HalfTime.getValue()) > 0) {
            modList.add("HT");
        }
        if ((mods & Mods.Nightcore.getValue()) > 0) {
            modList.add("NC");
        }
        if ((mods & Mods.Flashlight.getValue()) > 0) {
            modList.add("FL");
        }
        if ((mods & Mods.Autoplay.getValue()) > 0) {
            modList.add("AT");
        }
        if ((mods & Mods.SpunOut.getValue()) > 0) {
            modList.add("SO");
        }
        if ((mods & Mods.Relax2.getValue()) > 0) {
            modList.add("AP");
        }
        if ((mods & Mods.Perfect.getValue()) > 0) {
            modList.add("PF");
        }
        if ((mods & Mods.Key4.getValue()) > 0) {
            modList.add("4K");
        }
        if ((mods & Mods.Key5.getValue()) > 0) {
            modList.add("5K");
        }
        if ((mods & Mods.Key6.getValue()) > 0) {
            modList.add("6K");
        }
        if ((mods & Mods.Key7.getValue()) > 0) {
            modList.add("7K");
        }
        if ((mods & Mods.Key8.getValue()) > 0) {
            modList.add("8K");
        }
        if ((mods & Mods.FadeIn.getValue()) > 0) {
            modList.add("FI");
        }
        if ((mods & Mods.Random.getValue()) > 0) {
            modList.add("RD");
        }
        if ((mods & Mods.Cinema.getValue()) > 0) {
            modList.add("CN");
        }
        if ((mods & Mods.Target.getValue()) > 0) {
            modList.add("TP");
        }
        if ((mods & Mods.Key9.getValue()) > 0) {
            modList.add("9K");
        }
        if ((mods & Mods.KeyCoop.getValue()) > 0) {
            modList.add("KC");
        }
        if ((mods & Mods.Key1.getValue()) > 0) {
            modList.add("1K");
        }
        if ((mods & Mods.Key3.getValue()) > 0) {
            modList.add("3K");
        }
        if ((mods & Mods.Key2.getValue()) > 0) {
            modList.add("2K");
        }
        if ((mods & Mods.ScoreV2.getValue()) > 0) {
            modList.add("SV2");
        }
        if ((mods & Mods.Mirror.getValue()) > 0) {
            modList.add("MR");
        }

        // Handle specific conditions
        if (modList.contains("PF") && modList.contains("SD")) {
            modList.remove("SD");
        }
        if (modList.contains("NC") && modList.contains("DT")) {
            modList.remove("DT");
        }

        return modList.toArray(new String[modList.size()]);
    }


}
