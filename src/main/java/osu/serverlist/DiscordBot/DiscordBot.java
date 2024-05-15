package osu.serverlist.DiscordBot;

import java.util.List;

import commons.marcandreher.Cache.CacheTimer;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import osu.serverlist.DiscordBot.cache.UpdateAutocompletions;
import osu.serverlist.DiscordBot.cache.UpdateStatusChannel;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Recent;

public class DiscordBot {

    public static JDA jdaInstance;

    public Dotenv dotenv;

    public DiscordBot(Flogger logger, CacheTimer cacheTimer) {

        Activity activity = Activity.playing("osu-server-list.com");

        try {
            dotenv = Dotenv.configure().filename("discord.env").load();
            jdaInstance = JDABuilder.createDefault(dotenv.get("DISCORD_BOT_TOKEN"))
                    .addEventListeners(new DiscordCommandHandler(), new Leaderboard(), new Recent()).setActivity(activity).build().awaitReady();
        } catch (Exception e) {
            logger.error(e);
            logger.log(Prefix.ERROR, "Failed to start DiscordBot", 0);
            return;
        }
        cacheTimer.addAction(new UpdateStatusChannel(dotenv.get("DISCORD_STATS_CHANNEL_ID")));
        cacheTimer.addAction(new UpdateAutocompletions());

    }

    public static void deleteCommandsForAllServers() {
        try {
            List<Guild> guilds = jdaInstance.getGuilds();
            for (Guild guild : guilds) {
                guild.retrieveCommands().complete().forEach(command -> {
                    guild.deleteCommandById(command.getId()).queue(
                        success -> System.out.println("Command deleted: " + command.getName()),
                        error -> System.err.println("Failed to delete command: " + command.getName() + ", Error: " + error.getMessage())
                    );
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initializeCommand() {
        try {
            deleteCommandsForAllServers();
    

            jdaInstance.upsertCommand("stats", "Get stats of a server")
            .addOption(OptionType.STRING, "server", "The name of the server", true, true)
            .queue();

            jdaInstance.upsertCommand("invite", "Invite the bot")
            .queue();

            jdaInstance.upsertCommand("profile", "Show the profile of the user")
            .addOption(OptionType.STRING, "server", "The name of the server", true, true)
            .addOption(OptionType.STRING, "mode", "Mode you want to see stats for", true, true)
            .addOption(OptionType.STRING, "name", "Name on the server", true, false)
            .queue();

            jdaInstance.upsertCommand("leaderboard", "Show the leaderboard")
            .addOption(OptionType.STRING, "server", "The name of the server", true, true)
            .addOption(OptionType.STRING, "mode", "Mode you want to see stats for", true, true)
            .addOption(OptionType.STRING, "sort", "How you want to sort the leaderboard", true, true)
            .queue();

            jdaInstance.upsertCommand("recent", "Show the leaderboard")
            .addOption(OptionType.STRING, "server", "The name of the server", true, true)
            .addOption(OptionType.STRING, "mode", "Mode you want to see stats for", true, true)
            .addOption(OptionType.STRING, "name", "Name on the server", true, false)
            .queue();
        } catch (Exception e) {
            Flogger.instance.error(e);
        }
    }

   

}
