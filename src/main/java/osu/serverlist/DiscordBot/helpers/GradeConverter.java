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
}
