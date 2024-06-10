package osu.serverlist.Input.Commands;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Input.Command;

public class Crawlerlog implements Command{

    public static boolean enabled = false;

    @Override
    public void executeAction(String[] arg0, Flogger arg1) {
       Crawlerlog.enabled = !Crawlerlog.enabled;
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Toggles the crawler log";
    }

    @Override
    public String getName() {
        return "crawlerlog";
    }
    
}
