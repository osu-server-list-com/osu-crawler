package osu.serverlist.Input.Commands;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Input.Command;
import commons.marcandreher.Utils.ListUtils;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Recent;

public class CheckDcCache implements Command {

    public enum DcCacheType {
        LEADERBOARD,
        RECENT
    }

    @Override
    public void executeAction(String[] args, Flogger logger) {
        DcCacheType curType = null;
        for (DcCacheType type : DcCacheType.values()) {
            if (type.name() == args[0]) {
                curType = type;
            }
        }
        try {

            switch (curType) {
                case LEADERBOARD:
                    ListUtils.printHashMap(Leaderboard.userOffsets, logger);
                    break;

                case RECENT:
                    ListUtils.printHashMap(Recent.userOffsets, logger);
                    break;

                default:
                    logger.log(Prefix.INFO, "Invalid cache type", 0);
                    break;
            }
            
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    public String getAlias() {
        StringBuilder aliasBuilder = new StringBuilder("checkcache <");
        DcCacheType[] cacheTypes = DcCacheType.values();
        for (int i = 0; i < cacheTypes.length; i++) {
            aliasBuilder.append(cacheTypes[i]);
            if (i < cacheTypes.length - 1) {
                aliasBuilder.append("/");
            }
        }
        aliasBuilder.append(">");
        return aliasBuilder.toString();
    }

    @Override
    public String getDescription() {
        return "check hashmaps of dc cache";
    }

    @Override
    public String getName() {
        return "checkdcache";
    }
}
