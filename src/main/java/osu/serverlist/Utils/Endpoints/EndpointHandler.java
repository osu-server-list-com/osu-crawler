package osu.serverlist.Utils.Endpoints;

import java.sql.ResultSet;

import commons.marcandreher.Commons.MySQL;
import osu.serverlist.Models.Server;


public class EndpointHandler {

    private MySQL mysql;

    public EndpointHandler(MySQL mysql) {
        this.mysql = mysql;
    }

    public Endpoint getEndpoint(Server v, ServerEndpoints type) throws Exception {
        Endpoint p = new Endpoint();
        ResultSet endpointRs = mysql.Query("SELECT * FROM `un_endpoints` WHERE `srv_id` = ? AND `type` = ?", String.valueOf(v.getId()), type.toString());
        while(endpointRs.next()) {
            p.setApitype(endpointRs.getString("apitype"));
            p.setUrl(endpointRs.getString("endpoint"));
            
        }
        return p;
    }
    
}
