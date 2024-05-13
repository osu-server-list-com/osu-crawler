package osu.serverlist.Input.Commands;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Input.Command;
import osu.serverlist.Main.DiscordBot;

public class InitCmds implements Command{

    @Override
    public void executeAction(String[] arg0, Flogger arg1) {
       DiscordBot.initializeCommand();
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Initializes all commands for the bot";
    }

    @Override
    public String getName() {
        return "initDcCmds";
    }
    
}
