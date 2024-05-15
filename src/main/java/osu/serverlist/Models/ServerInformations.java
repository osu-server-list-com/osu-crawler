package osu.serverlist.Models;

/**
 * ServerInformations
 */
public class ServerInformations {

    private String endpoint;
    private String name;
    private String avatarServer;
    private String url;

    private String type;


    public String getEndpoint() {
        return this.endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarServer() {
        return this.avatarServer;
    }

    public void setAvatarServer(String avatarServer) {
        this.avatarServer = avatarServer;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

}