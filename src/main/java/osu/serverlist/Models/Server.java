package osu.serverlist.Models;

import lombok.Data;

@Data
public class Server {

    private int id;
    private String name;
    private String url;
    private String logo_loc;
    private String created;
    private Boolean rippleApiV2;
    private Boolean rippleApiV1;
    private int votes;
    private int players;

    private String safe_name;

    private String apiKey;

    private Boolean banchopy;
    private Boolean lisekApi;

    public Boolean getLisekApi() {
        return this.lisekApi;
    }

    public void setLisekApi(Boolean lisekApi) {
        this.lisekApi = lisekApi;
    }


    public Boolean getBanchopy() {
        return this.banchopy;
    }

    public void setBanchopy(Boolean banchopy) {
        this.banchopy = banchopy;
    }

    public String getSafe_name() {
        return this.safe_name;
    }

    public void setSafe_name(String safe_name) {
        this.safe_name = safe_name;
    }


    public int getVotes() {
        return this.votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }



    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogo_loc() {
        return this.logo_loc;
    }

    public void setLogo_loc(String logo_loc) {
        this.logo_loc = logo_loc;
    }

    public String getCreated() {
        return this.created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public Boolean isRippleApiV2() {
        return this.rippleApiV2;
    }

    public Boolean getRippleApiV2() {
        return this.rippleApiV2;
    }

    public void setRippleApiV2(Boolean rippleApiV2) {
        this.rippleApiV2 = rippleApiV2;
    }

    public Boolean isRippleApiV1() {
        return this.rippleApiV1;
    }

    public Boolean getRippleApiV1() {
        return this.rippleApiV1;
    }

    public void setRippleApiV1(Boolean rippleApiV1) {
        this.rippleApiV1 = rippleApiV1;
    }

    public int getPlayers() {
        return this.players;
    }

    public void setPlayers(int players) {
        this.players = players;
    }

}