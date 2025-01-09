package osu.serverlist.Main;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import commons.marcandreher.Commons.Flogger;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;

public class MetricsCollector {
    private final HTTPServer metrics;
    public Map<String, Gauge> counters = new HashMap<String,Gauge>();

    public MetricsCollector() throws NumberFormatException, IOException {
        metrics = new HTTPServer(Integer.parseInt(Crawler.env.get("PR_CRAWLER")));
    }

    public void registerCounter(String name, String help) {
        Gauge  counter = Gauge.build().name(name).help(help).register();
        counters.put(name, counter);
    }

    public void setCounter(String name, double value) {
        counters.get(name).set(value);
    }

    public void incCounter(String name) {
        counters.get(name).inc();
    }

    public void decCounter(String name) {
        counters.get(name).dec();
    }

}
