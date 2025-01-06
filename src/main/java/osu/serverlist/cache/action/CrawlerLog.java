package osu.serverlist.cache.action;

import commons.marcandreher.Utils.Color;
import osu.serverlist.Input.Commands.Crawlerlog;

public class CrawlerLog {

    private final String PREFIX = Color.GREEN + "[Crawler] " + Color.RESET;

    public void log(String message) {
        if(Crawlerlog.enabled) {
            System.out.print(PREFIX + message);
        }
    }

    public void logln(String message) {
        if(Crawlerlog.enabled) {
            System.out.println(PREFIX + message);
        }
    }

    public void fail() {
        if(Crawlerlog.enabled) {
            System.out.print(" [" + Color.RED + "FAIL" + Color.RESET + "]\n");
        }
    }

    public void success() {
        if(Crawlerlog.enabled) {
            System.out.print(" [" + Color.GREEN + "SUCCESS" + Color.RESET + "]\n");
        }
    }

    public void logStat(String name, Integer stat) {
        if (Crawlerlog.enabled) {
            int nameWidth = 15; 
            String formattedName = String.format("%-" + nameWidth + "s", name); 
            System.out.println(PREFIX + formattedName + " -> (" + Color.GREEN + stat + Color.RESET + ")");
        }
    }
    
}
