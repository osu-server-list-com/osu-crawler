package osu.serverlist.Utils.Endpoints;

public class Endpoint {

    private int id;
    private String type;
    private String apitype;
    private int srv_id;
    private String url;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getApitype() {
        return this.apitype;
    }

    public void setApitype(String apitype) {
        this.apitype = apitype;
    }

    public int getSrv_id() {
        return this.srv_id;
    }

    public void setSrv_id(int srv_id) {
        this.srv_id = srv_id;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
}
