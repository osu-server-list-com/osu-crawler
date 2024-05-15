package osu.serverlist.DiscordBot.helpers;

public class GradeConverter {
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

}
