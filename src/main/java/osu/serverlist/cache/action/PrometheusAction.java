package osu.serverlist.cache.action;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import commons.marcandreher.Cache.Action.DatabaseAction;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import osu.serverlist.Main.Crawler;

public class PrometheusAction extends DatabaseAction {
    private final OperatingSystemMXBean osBean;

    public PrometheusAction() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        Crawler.metrics.registerCounter("osl_cr_cpu", "Crawler CPU usage");
        Crawler.metrics.registerCounter("osl_cr_memory", "Crawler memory usage");
        Crawler.metrics.registerCounter("osl_cr_active_threads", "Number of active threads in the crawler");
    
        Crawler.metrics.registerCounter("osl_cr_srv_incidents", "Number of current server incidents");
    }

    @Override
    public void executeAction(Flogger logger) {
        super.executeAction(logger);

        // Report CPU load
        double cpuLoad = osBean.getSystemLoadAverage(); // 1-minute system load average
        if (cpuLoad >= 0) {
            Crawler.metrics.setCounter("osl_cr_cpu", cpuLoad);
        }

        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory * 100; // Memory usage as a percentage
        Crawler.metrics.setCounter("osl_cr_memory", memoryUsage);

        int activeThreads = Thread.activeCount();
        Crawler.metrics.setCounter("osl_cr_active_threads", activeThreads);

        Crawler.metrics.setCounter("osl_cr_srv_incidents", CrawlerAction.incidentServerList.size());
        
        logger.log(Prefix.API + "Metrics updated: CPU=" + cpuLoad + ", Memory=" + memoryUsage + "%, Threads=" + activeThreads);
    }
}
