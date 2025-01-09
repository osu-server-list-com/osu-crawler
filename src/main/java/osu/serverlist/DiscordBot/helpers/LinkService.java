package osu.serverlist.DiscordBot.helpers;

import java.sql.ResultSet;

import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger.Prefix;
import lombok.Data;
import commons.marcandreher.Commons.MySQL;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import osu.serverlist.DiscordBot.base.MessageBuilder;

public class LinkService implements AutoCloseable {

    private final String INSERT_LINK_SQL = "INSERT INTO `osl_bot_links`(`discord_user_id`, `discord_server_id`, `osl_server`, `mode`, `username`) VALUES (?,?,?,?,?)";
    private final String DELETE_EXISTING_LINK_SQL = "DELETE FROM `osl_bot_links` WHERE `discord_user_id` = ? AND `discord_server_id` = ?";
    private final MySQL mysql;

    public enum LinkResponse {
        SUCCESS,
        OVERWRITTEN,
        ERROR
    }

    @Data
    public class LinkResponseObject {
        private String server;
        private String mode;
        private String name;
    }
    
    public LinkService() {
        try {
            mysql = Database.getConnection();
        } catch (Exception e) {
            System.out.println(Prefix.ERROR + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public LinkResponseObject getLink(SlashCommandInteractionEvent event) {
        LinkResponseObject response = new LinkResponseObject();
        try {
            ResultSet linkResult = mysql.Query("SELECT * FROM `osl_bot_links` WHERE `discord_user_id` = ? AND `discord_server_id` = ?", event.getUser().getId(), event.getGuild().getId());
            if(linkResult.next()) {
                response.setServer(linkResult.getString("osl_server"));
                response.setMode(linkResult.getString("mode"));
                response.setName(linkResult.getString("username"));
            }

            try {
                response.setServer(event.getOption("server").getAsString().toLowerCase());
                response.setMode(event.getOption("mode").getAsString().toLowerCase());
                response.setName(event.getOption("name").getAsString().toLowerCase());
            }catch(Exception e) {
                
            }

            if(response.getServer() == null || response.getMode() == null || response.getName() == null) {
                event.getHook().sendMessage("").addEmbeds(MessageBuilder.buildMessageError("To use this command without **parameters** you need to do __/link__ with your osu! account and server").build()).queue();
            }

        } catch (Exception e) {
            System.out.println(Prefix.ERROR + e.getMessage());
        }
        return response;
    }

    public LinkResponse addLinkToUser(SlashCommandInteractionEvent event, String server, String name, String mode) {
        LinkResponse response;
        int affected = mysql.Exec(DELETE_EXISTING_LINK_SQL, event.getUser().getId(), event.getGuild().getId());
        if(affected != 0) response = LinkResponse.OVERWRITTEN;
   

        try {
            mysql.Exec(INSERT_LINK_SQL, event.getUser().getId(), event.getGuild().getId(), server, mode, name);
            response = LinkResponse.SUCCESS;
        } catch (Exception e) {
            System.out.println(Prefix.ERROR + e.getMessage());
            response = LinkResponse.ERROR;
        }
        return response;
    }

    @Override
    public void close() {
        mysql.close();
    }
    
}
