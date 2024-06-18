package osu.serverlist.DiscordBot.cache;

import java.util.List;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import osu.serverlist.Cache.Action.Helpers.NewCrawler;
import osu.serverlist.Cache.Action.Helpers.NewCrawler.BotType;
import osu.serverlist.DiscordBot.DiscordBot;

public class UpdateDiscordRelatedStats extends DatabaseAction {
    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);
        JDA jda = DiscordBot.jdaInstance;
        NewCrawler newCrawler = new NewCrawler(mysql);
        
        List<Guild> guilds = jda.getGuilds();
        
        newCrawler.updateBotCount(BotType.SERVERS, guilds.size());

        int totalUsers = 0;
        
        for (Guild guild : guilds) {
            totalUsers += guild.getMemberCount();
        }
        newCrawler.updateBotCount(BotType.PLAYERS, totalUsers);
    }
}
