package osu.serverlist.Models;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Config {
    // Default Settings

    private String serverIp = "127.0.0.1";
    private int port = 80;
    private int apiport = 89;
 
    private String domain = "http://localhost";
    private int threadPool = 4;
    private short cacheLevel = 1;

    private short logLevel = 1;



    private String reCaptchaSecret = "";
    private String reCaptchaSite = "";
   
    private String discordAPIID = "";
    private String discordAPISecret = "";

    private String mySQLIp = "127.0.0.1";
    private int mySQLPort = 3306;
    private String mySQLUserName = "root";
    private String mySQLPassword = "";
    private String mySQLDatabase = "osuserverlist";

    public static Config initializeNewConfig() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Path filePath = Paths.get("config.json");

            if (Files.exists(filePath)) {
                // If the file exists, read its content
                return objectMapper.readValue(Files.readAllBytes(filePath), Config.class);
            } else {
                // If the file doesn't exist, create a new ConfigModel
                Config cfg = new Config();
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                String newFileContent = objectMapper.writeValueAsString(cfg);

                // Write the new content to the file
                Files.write(filePath, newFileContent.getBytes());

                // Read the content again and return the ConfigModel
                return objectMapper.readValue(Files.readAllBytes(filePath), Config.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setApiport(int apiport) {
        this.apiport = apiport;
    }

    public int getApiport() {
        return this.apiport;
    }


    
    public String getReCaptchaSecret() {
        return this.reCaptchaSecret;
    }

    public void setReCaptchaSecret(String reCaptchaSecret) {
        this.reCaptchaSecret = reCaptchaSecret;
    }

    public String getReCaptchaSite() {
        return this.reCaptchaSite;
    }

    public void setReCaptchaSite(String reCaptchaSite) {
        this.reCaptchaSite = reCaptchaSite;
    }

    public short getCacheLevel() {
        return cacheLevel;
    }

    public String getServerIp() {
        return this.serverIp;
    }

    public String getMySQLDatabase() {
        return this.mySQLDatabase;
    }

    public int getPort() {
        return this.port;
    }

    public String getDomain() {
        return this.domain;
    }

    public int getThreadPool() {
        return this.threadPool;
    }

    public short getLogLevel() {
        return this.logLevel;
    }

    public String getDiscordAPIID() {
        return this.discordAPIID;
    }

    public String getDiscordAPISecret() {
        return this.discordAPISecret;
    }


    public String getMySQLIp() {
        return this.mySQLIp;
    }

    public int getMySQLPort() {
        return this.mySQLPort;
    }

    public String getMySQLUserName() {
        return this.mySQLUserName;
    }

    public String getMySQLPassword() {
        return this.mySQLPassword;
    }

}