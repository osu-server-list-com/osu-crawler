package osu.serverlist.DiscordBot;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

import commons.marcandreher.Cache.CacheTimer;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import osu.serverlist.DiscordBot.cache.UpdateAutocompletions;
import osu.serverlist.DiscordBot.cache.UpdateDiscordRelatedStats;
import osu.serverlist.DiscordBot.cache.UpdateStatusChannel;
import osu.serverlist.DiscordBot.commands.Best;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.Main.Crawler;

public class DiscordBot implements Runnable {
    private static final int AUTOCOMPLETE_UPDATE_INTERVAL = 15;
    private static final int STATS_UPDATE_INTERVAL = 60;
    
    private final Flogger logger;
    private Dotenv dotenv;
    private static JDA jdaInstance;
    private CacheTimer cacheTimer;
    private CacheTimer botHandler;

    public DiscordBot(Flogger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            initializeBot();
            setupCacheTimers();
            logBotInfo();
        } catch (Exception e) {
            logger.log(Prefix.ERROR, "DiscordBot failed starting: " + e.getMessage(), 0);
        }
    }

    private void initializeBot() throws InterruptedException {
        dotenv = Dotenv.configure().filename("discord.env").load();
        jdaInstance = JDABuilder.createDefault(dotenv.get("DISCORD_BOT_TOKEN"))
                .addEventListeners(
                    new DiscordCommandHandler(),
                    new Leaderboard(),
                    new Recent(),
                    new Best()
                )
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build()
                .awaitReady();

        startStatusSwitcher();
      
    
    }

    private void startStatusSwitcher() {
        Thread activityRunner = new Thread(new DiscordBotStatusSwitcher(jdaInstance));
        activityRunner.setName("DiscordBotStatusSwitcher");
        activityRunner.start();
    }

    private void setupCacheTimers() {
        setupAutocompletionTimer();
        setupStatsTimer();
    }

    private void setupAutocompletionTimer() {
        cacheTimer = new CacheTimer(AUTOCOMPLETE_UPDATE_INTERVAL, 1, TimeUnit.MINUTES);
        cacheTimer.addAction(new UpdateAutocompletions());
        cacheTimer.addAction(new UpdateStatusChannel(dotenv.get("DISCORD_STATS_CHANNEL_ID")));
    }

    private void setupStatsTimer() {
        botHandler = new CacheTimer(STATS_UPDATE_INTERVAL, 1, TimeUnit.MINUTES);
        botHandler.addAction(new UpdateDiscordRelatedStats());
    }

    @SuppressWarnings("deprecation")
    private void logBotInfo() {
        logger.log("\n");
        logger.log(Prefix.API, "Bot Username: " + jdaInstance.getSelfUser().getName());
        logger.log(Prefix.API, "Bot Discriminator: " + jdaInstance.getSelfUser().getDiscriminator());
        if(Crawler.metrics != null) {
            Crawler.metrics.registerCounter("osl_bot_cmds_execs", "Commands executed by the bot");
            Crawler.metrics.registerCounter("osl_bot_lams_panels", "Panels shown by the bot");
        }
    }

    public static void deleteCommandsForAllServers() {
        try {
            // Delete global commands first
            jdaInstance.retrieveCommands().complete().forEach(command -> {
                jdaInstance.deleteCommandById(command.getId()).complete();
                System.out.println("Global command deleted: " + command.getName());
            });
    
            // Then delete guild-specific commands
            List<Guild> guilds = jdaInstance.getGuilds();
            for (Guild guild : guilds) {
                guild.retrieveCommands().complete().forEach(command -> {
                    guild.deleteCommandById(command.getId()).complete();
                    System.out.println("Guild command deleted: " + command.getName());
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void initializeCommand() {
        try {
            deleteCommandsForAllServers();
            registerCommands();
        } catch (Exception e) {
            Flogger.instance.error(e);
        }
    }

    private static void registerCommands() {
        CompletableFuture.allOf(
            registerCommand(createStatsCommand()),
            registerCommand(createInviteCommand()),
            registerCommand(createProfileCommand()),
            registerCommand(createLeaderboardCommand()),
            registerCommand(createRecentCommand()),
            registerCommand(createBestCommand()),
            registerCommand(createLinkCommand())
        ).join();
    }

    private static CompletableFuture<Void> registerCommand(CommandData command) {
        return jdaInstance.upsertCommand(command).submit()
            .thenAccept(cmd -> Flogger.instance.log(Prefix.API, "Registered command: " + cmd.getName()));
    }

    private static CommandData createStatsCommand() {
        return Commands.slash("stats", "Get stats of a server")
            .addOption(OptionType.STRING, "server", "The name of the server", true, true);
    }

    private static CommandData createInviteCommand() {
        return Commands.slash("invite", "Invite the bot");
    }

    private static CommandData createProfileCommand() {
        return Commands.slash("profile", "Shows profile of the user")
            .addOptions(
                new OptionData(OptionType.STRING, "server", "The name of the server", false).setAutoComplete(true),
                new OptionData(OptionType.STRING, "mode", "Mode you want to see stats for", false).setAutoComplete(true),
                new OptionData(OptionType.STRING, "name", "Name on the server", false)
            );
    }

    private static CommandData createLeaderboardCommand() {
        return Commands.slash("leaderboard", "Shows leaderboard")
            .addOptions(
                new OptionData(OptionType.STRING, "server", "The name of the server", true).setAutoComplete(true),
                new OptionData(OptionType.STRING, "mode", "Mode you want to see stats for", true).setAutoComplete(true),
                new OptionData(OptionType.STRING, "sort", "How you want to sort the leaderboard", true).setAutoComplete(true)
            );
    }

    private static CommandData createRecentCommand() {
        return Commands.slash("recent", "Shows recent scores")
            .addOptions(
                new OptionData(OptionType.STRING, "server", "The name of the server", false).setAutoComplete(true),
                new OptionData(OptionType.STRING, "mode", "Mode you want to see stats for", false).setAutoComplete(true),
                new OptionData(OptionType.STRING, "name", "Name on the server", false)
            );
    }

    private static CommandData createBestCommand() {
        return Commands.slash("best", "Shows best scores")
            .addOptions(
                new OptionData(OptionType.STRING, "server", "The name of the server", false).setAutoComplete(true),
                new OptionData(OptionType.STRING, "mode", "Mode you want to see stats for", false).setAutoComplete(true),
                new OptionData(OptionType.STRING, "name", "Name on the server", false)
            );
    }

    private static CommandData createLinkCommand() {
        return Commands.slash("link", "Link your osu account to a discord server account")
            .addOptions(
                new OptionData(OptionType.STRING, "server", "The name of the server", true).setAutoComplete(true),
                new OptionData(OptionType.STRING, "mode", "Mode you want to see stats for", true).setAutoComplete(true),
                new OptionData(OptionType.STRING, "name", "Name on the server", true)
            );
    }

    // Getter for jdaInstance if needed elsewhere
    public static JDA getJdaInstance() {
        return jdaInstance;
    }
}