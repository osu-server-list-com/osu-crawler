package osu.serverlist.Main;

import java.io.File;
import java.util.concurrent.TimeUnit;

import commons.marcandreher.Cache.CacheTimer;
import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Database.ServerTimezone;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.MySQL;
import commons.marcandreher.Input.CommandHandler;
import io.github.cdimascio.dotenv.Dotenv;
import osu.serverlist.Cache.Action.CheckServer;
import osu.serverlist.DiscordBot.DiscordBot;
import osu.serverlist.Input.Commands.CheckDcCache;
import osu.serverlist.Input.Commands.Crawlerlog;
import osu.serverlist.Input.Commands.DelCmds;
import osu.serverlist.Input.Commands.ExceptionManager;
import osu.serverlist.Input.Commands.ExpireVotes;
import osu.serverlist.Input.Commands.InitCmds;
import osu.serverlist.Input.Commands.Servers;

public class Crawler {
    protected static Flogger LOG = null;
    public static Dotenv env;

    public static void main(String[] args) {
        MySQL.LOGLEVEL = 5;

        File directory = new File("osr");
        if (!directory.exists()) directory.mkdir();
  
        env = Dotenv.load();
        LOG = new Flogger(Integer.parseInt(env.get("LOGLEVEL")));

        Database db = new Database();
        db.setDefaultSettings();
        db.setMaximumPoolSize(5);
        db.setConnectionTimeout(1000);
        db.connectToMySQL(env.get("DBHOST"), env.get("DBUSER"), env.get("DBPASS"), env.get("DBNAME"), ServerTimezone.UTC);
        
        CacheTimer cacheTimer = new CacheTimer(15, 1, TimeUnit.MINUTES);
        cacheTimer.addAction(new CheckServer());

        Runnable discordBotRunnable = new DiscordBot(LOG);
        Thread discordBotStarter = new Thread(discordBotRunnable);
        discordBotStarter.setName("DiscordBot-Executor");
        discordBotStarter.start();

        if(args.length == 1 && !args[0].contains("-nocmd")) {
            return;
        }

        CommandHandler cmd = new CommandHandler(LOG);
        cmd.registerCommand(new ExceptionManager());
        cmd.registerCommand(new DelCmds());
        cmd.registerCommand(new CheckDcCache());
        cmd.registerCommand(new InitCmds());
        cmd.registerCommand(new Servers());
        cmd.registerCommand(new Crawlerlog());
        cmd.registerCommand(new ExpireVotes());
        cmd.initialize();
        
    }
}
