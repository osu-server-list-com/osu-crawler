package osu.serverlist.cache.action;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.sql.ResultSet;

import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import osu.serverlist.Main.Crawler;

public class PrometheusAction extends DatabaseAction {
    private final OperatingSystemMXBean osBean;

    public PrometheusAction() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        Crawler.metrics.registerCounter("osl_cr_cpu", "Crawler CPU usage");
        Crawler.metrics.registerCounter("osl_cr_memory", "Crawler memory usage");
        Crawler.metrics.registerCounter("osl_cr_active_threads", "Number of active threads in the crawler");
    
        Crawler.metrics.registerCounter("osl_cr_srv_incidents", "Number of current server incidents");

        Crawler.metrics.registerCounter("osl_web_servers_visible", "Visible Servers on osu-server-list.com");
        Crawler.metrics.registerCounter("osl_web_servers_unlocked", "Crawlable Servers on osu-server-list.com");
    }

    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);

        double cpuLoad = osBean.getSystemLoadAverage();
        if (cpuLoad >= 0) {
            Crawler.metrics.setCounter("osl_cr_cpu", cpuLoad);
        }

        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory * 100; 
        Crawler.metrics.setCounter("osl_cr_memory", memoryUsage);

        int activeThreads = Thread.activeCount();
        Crawler.metrics.setCounter("osl_cr_active_threads", activeThreads);

        Crawler.metrics.setCounter("osl_cr_srv_incidents", CrawlerAction.incidentServerList.size());
    
        ResultSet extraServerQuery = mysql.Query("SELECT (SELECT COUNT(`id`) FROM `un_servers` WHERE `locked` != 1) AS `unlocked_servers`, (SELECT COUNT(`id`) FROM `un_servers` WHERE `visible` = 1) AS `visible_servers`;");
        try {
            if (extraServerQuery.next()) {
                Crawler.metrics.setCounter("osl_web_servers_visible", extraServerQuery.getInt("unlocked_servers"));
                Crawler.metrics.setCounter("osl_web_servers_unlocked", extraServerQuery.getInt("visible_servers"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
