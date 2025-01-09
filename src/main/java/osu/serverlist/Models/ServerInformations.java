package osu.serverlist.Models;

import lombok.Data;

@Data
public class ServerInformations {

    private String endpoint;
    private String name;
    private String avatarServer;
    private String url;

    private String type;


}