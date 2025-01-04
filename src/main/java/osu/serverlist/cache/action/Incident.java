package osu.serverlist.cache.action;

import lombok.Data;

@Data
public class Incident {
    private String time;
    private String message;
    private String url;
    private int responseCode;

}
