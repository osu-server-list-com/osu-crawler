package osu.serverlist.DiscordBot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import commons.marcandreher.Commons.Database;
import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import commons.marcandreher.Commons.MySQL;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import osu.serverlist.DiscordBot.commands.Best;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.DiscordBot.commands.Stats;
import osu.serverlist.Handlers.AlertHandler;

public class DiscordCommandHandler extends ListenerAdapter {

    HashMap<String, File> awaitingMaps = new HashMap<String, File>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        MySQL mysql = null;;
        try {
            mysql = Database.getConnection();
        } catch (SQLException e) {
            Flogger.instance.error(e);
            return;
        }
        AlertHandler alertHandler = new AlertHandler(mysql);
        alertHandler.createSystemAlert("OSL Bot joined a Server", "OSL Discord Bot joined the " + event.getGuild().getName() + " server with " + event.getGuild().getMemberCount() + " members");
        mysql.close();
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        MySQL mysql = null;
        try {
            mysql = Database.getConnection();
        } catch (SQLException e) {
            Flogger.instance.error(e);
            return;
        }
        AlertHandler alertHandler = new AlertHandler(mysql);
        alertHandler.createSystemAlert("OSL Bot left a Server", "OSL Discord Bot left the " + event.getGuild().getName() + " server.");
        mysql.close();
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        if (!event.getUser().isBot()) {
            return;
        }
        MySQL mysql = null;
        try {
            mysql = Database.getConnection();
        } catch (SQLException e) {
            Flogger.instance.error(e);
            return;
        }
        AlertHandler alertHandler = new AlertHandler(mysql);
        alertHandler.createSystemAlert("OSL Bot was banned from a Server", "OSL Discord Bot was banned from the " + event.getGuild().getName() + " server.");
        mysql.close();
    }


    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("stats") && event.getFocusedOption().getName().equals("server")) {
            new Stats().handleAutoComplete(event);
            return;
        }

        if (event.getName().equals("profile")) {
            new Profile().handleAutoComplete(event);
            return;
        }

        if (event.getName().equals("leaderboard")) {
            new Leaderboard().handleAutoComplete(event);
            return;
        }

        else if (event.getName().equals("recent")) {
            new Recent().handleAutoComplete(event);
        }

        else if (event.getName().equals("best")) {
            new Best().handleAutoComplete(event);
        }

    }

    @SuppressWarnings("deprecation")
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        List<Attachment> attachments = event.getMessage().getAttachments();
        if (!attachments.isEmpty()) {
            for (Attachment attachment : attachments) {
                if (attachment.isVideo() || attachment.isImage()) {
                    continue;
                }

                String fileName = attachment.getFileName();

                if (fileName.endsWith(".osr")) {
                    Flogger.instance.log(Prefix.INFO, "OSR Received " + fileName, 0);
                    attachment.downloadToFile("osr/" + attachment.getFileName()).thenAccept(file -> {
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setColor(0x5755d9);
                        String userMention = event.getAuthor().getAsMention();
                        embed.setDescription(userMention + " do you want to generate a [lookatmysco.re](https://lookatmysco.re) Panel with your given replay?\n-# Message disappears in 8 seconds");
                        event.getChannel().sendMessage("").addEmbeds(embed.build())
                                .queue(message -> {

                                    awaitingMaps.put(message.getId(), file);
                                    message.addReaction(Emoji.fromUnicode("U+2705")).queue();
                                    message.addReaction(Emoji.fromUnicode("U+274C")).queue();
                                    scheduler.schedule(() -> {
                                        message.delete().queue();
                                        awaitingMaps.remove(message.getId());
                                    }, 8, TimeUnit.SECONDS);
                                });

                    }).exceptionally(e -> {
                        Flogger.instance.error(new Exception(e));
                        return null;
                    });
                }
            }
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser().isBot()) {
            return;
        }

        String messageId = event.getMessageId();
        Emoji emoji = event.getEmoji();

        if (!awaitingMaps.containsKey(messageId)) {
            return;
        }

        if (emoji.getAsReactionCode().equals("✅")) {
            try {
                String image = uploadFile(awaitingMaps.get(messageId));
                if (image != null) {
                    event.getChannel().deleteMessageById(messageId).queue();
                    awaitingMaps.remove(messageId);
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setImage(image);
                    embed.setColor(0x5755d9);
                    embed.setDescription("-# Panel by [lookatmysco.re](https://lookatmysco.re) | Powered by [osu!ServerList](https://osu-server-list.com)");
                    event.getChannel().sendMessage("").addEmbeds(embed.build()).queue();
                }

            } catch (IOException e) {
                Flogger.instance.error(new Exception(e));
            } catch (URISyntaxException e) {
                Flogger.instance.error(new Exception(e));
            }
        } else if (emoji.getAsReactionCode().equals("❌")) {
            event.getChannel().deleteMessageById(messageId).queue();
            awaitingMaps.remove(messageId);

        } else {
            Flogger.instance.log(Prefix.API, "Unknown reaction: " + emoji.getAsReactionCode(), 0);
        }
    }

    private String uploadFile(File file) throws IOException, URISyntaxException {
        String url = "https://lookatmysco.re/api/submit-osr";
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just a random unique string
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.
        String outputUrl = null;
        HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "https://osu-server-list.com (compatible: Linux OSL-App)");
        try (
                OutputStream output = connection.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {
            // Send binary file.
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"osr_file\"; filename=\"").append(file.getName())
                    .append("\"").append(CRLF);
            writer.append("Content-Type: ").append(Files.probeContentType(file.toPath())).append(CRLF);
            writer.append(CRLF).flush();
            Files.copy(file.toPath(), output);
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // End of multipart/form-data.
            writer.append("--").append(boundary).append("--").append(CRLF).flush();
        }

        // Log the response headers and redirect URL
        int responseCode = connection.getResponseCode();
        Flogger.instance.log(Prefix.API, url + " | " + responseCode, 0);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            if (response.length() > 0) {
                try {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
                    JSONObject image = (JSONObject) jsonResponse.get("image");
                    if (image != null) {
                        outputUrl = (String) image.get("url");
                    }
                } catch (ParseException e) {
                    Flogger.instance.error(e);
                }
            }
        } catch (IOException e) {
            Flogger.instance.error(new Exception(e));
        }

        connection.disconnect();
        return outputUrl;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Flogger.instance.log(Prefix.API, "Slash Command: " + event.getName(), 0);
        if (event.getName().equals("stats")) {
            new Stats().handleCommand(event);
        } else if (event.getName().equals("profile")) {
            new Profile().handleCommand(event);
        } else if (event.getName().equals("leaderboard")) {
            new Leaderboard().handleCommand(event);
        } else if (event.getName().equals("recent")) {
            new Recent().handleCommand(event);
        } else if (event.getName().equals("best")) {
            new Best().handleCommand(event);
        } else {
            String inviteUrl = event.getJDA().getInviteUrl();
            event.reply("Here's the invite link: " + inviteUrl).queue();
        }
    }

}
