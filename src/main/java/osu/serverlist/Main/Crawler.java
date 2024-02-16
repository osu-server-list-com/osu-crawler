package osu.serverlist.Main;

import commons.marcandreher.Cache.CacheTimer;
import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.MySQL;
import commons.marcandreher.Commons.Database.ServerTimezone;
import commons.marcandreher.Input.CommandHandler;
import osu.serverlist.Cache.Action.CheckServer;
import osu.serverlist.Input.Commands.ExceptionManager;
import osu.serverlist.Models.Config;

public class Crawler {
    protected static Flogger LOG = null;
    protected static Config CONFIG;

    public static void main(String[] args) {
        MySQL.LOGLEVEL = 5;
        CONFIG = Config.initializeNewConfig();

        LOG = new Flogger(CONFIG.getLogLevel());

        Database db = new Database();
        db.setDefaultSettings();
        db.setMaximumPoolSize(5);
        db.setConnectionTimeout(1000);
        db.connectToMySQL(CONFIG.getMySQLIp(), CONFIG.getMySQLUserName(), CONFIG.getMySQLPassword(), CONFIG.getMySQLDatabase(), ServerTimezone.UTC);
        
        CacheTimer cacheTimer = new CacheTimer(15, 1, LOG);
        cacheTimer.addAction(new CheckServer());
       
        if(args.length == 1 && !args[0].contains("-nocmd")) {
            return;
        }
        CommandHandler cmd = new CommandHandler(LOG);
        cmd.registerCommand(new ExceptionManager());
        cmd.initialize();
        
    }
}
