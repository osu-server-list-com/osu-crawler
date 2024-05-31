package osu.serverlist.Input.Commands;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Input.Command;
import osu.serverlist.DiscordBot.DiscordBot;

public class Servers implements Command{

    @Override
    public void executeAction(String[] arg0, Flogger arg1) {
        DiscordBot.jdaInstance.getGuilds().forEach(guild -> {
            System.out.println("Server: " + guild.getName() + " - " + guild.getMembers().size() + " players");
        });
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Shows all servers on the bot";
    }

    @Override
    public String getName() {
        return "servers";
    }
    
}
