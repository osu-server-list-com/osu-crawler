package osu.serverlist.Handlers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import commons.marcandreher.Commons.MySQL;
import osu.serverlist.Models.Alert;
import osu.serverlist.Models.Alert.Author;

// Base for AP alerts

public class AlertHandler {

    private MySQL mysql;

    private final String CREATE_USER_WITH_ID_ALERT = "INSERT INTO `osl_alerts`(`title`, `description`, `author_id`) VALUES (?,?,?)";
    private final String CREATE_SYSTEM_ALERT = "INSERT INTO `osl_alerts`(`title`, `description`) VALUES (?,?)";
    private final String SELECT_ALL_ALERTS = "SELECT * FROM `osl_alerts` ORDER BY `osl_alerts`.`id` DESC";
    private final String SELECT_AUTHOR = "SELECT `username`, `profile_picture` FROM `un_users` WHERE `id` = ?";

    public AlertHandler(MySQL mysql) {
        this.mysql = mysql;
    }

    public void createAlertWithUserId(String userId, String title, String description) {
        mysql.Exec(CREATE_USER_WITH_ID_ALERT, title, description, userId);
    }

    public void createSystemAlert(String title, String description) {
        mysql.Exec(CREATE_SYSTEM_ALERT, title, description);
    }

    public Alert[] getAllAlerts() {
        ResultSet alertResult = mysql.Query(SELECT_ALL_ALERTS);
        List<Alert> alerts = new ArrayList<>();
        try {
            while (alertResult.next()) {
                Alert alert = new Alert();
                alert.setTitle(alertResult.getString("title"));
                alert.setDescription(alertResult.getString("description"));
                alert.setId(alertResult.getString("id"));
                
                if(alertResult.getString("author_id") == null) {
                    alerts.add(alert);
                    continue;
                }
                Author author = alert.new Author();
                ResultSet authorResult = mysql.Query(SELECT_AUTHOR, alertResult.getString("author_id"));
                authorResult.next();
                author.setName(authorResult.getString("username"));
                author.setIcon_url(authorResult.getString("profile_picture"));
                alert.setAuthor(author);
    
                alerts.add(alert);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts.toArray(new Alert[0]);
    }

}
