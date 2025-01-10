package osu.serverlist.Input.Commands;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Input.Command;
import osu.serverlist.cache.action.CrawlerAction;

public class Incidents implements Command {

    @Override
    public void executeAction(String[] arg0, Flogger logger) {
        logger.log(Prefix.INFO + "Incidents: " + CrawlerAction.incidentServerList.size());
    }

    @Override
    public String getAlias() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Get incidents";
    }

    @Override
    public String getName() {
        return "incidents";
    }
    
}
