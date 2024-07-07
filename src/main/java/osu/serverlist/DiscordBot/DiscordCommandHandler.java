package osu.serverlist.DiscordBot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import commons.marcandreher.Commons.Flogger;
import commons.marcandreher.Commons.Flogger.Prefix;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import osu.serverlist.DiscordBot.commands.Best;
import osu.serverlist.DiscordBot.commands.Leaderboard;
import osu.serverlist.DiscordBot.commands.Profile;
import osu.serverlist.DiscordBot.commands.Recent;
import osu.serverlist.DiscordBot.commands.Stats;

public class DiscordCommandHandler extends ListenerAdapter {

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

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        List<Attachment> attachments = event.getMessage().getAttachments();
        if (!attachments.isEmpty()) {
            for (Attachment attachment : attachments) {
                // Skip if the attachment is a video or image
                if (attachment.isVideo() || attachment.isImage()) {
                    continue;
                }

                // Check if the user has the right permissions
                if (!event.getAuthor().getId().equals("307257319266320394")) {
                    Flogger.instance.log(Prefix.API, "Permission denied for " + event.getAuthor().getAsTag(), 0);
                    return;
                }

                String fileName = attachment.getFileName();

                // Process only .osr files
                if (fileName.endsWith(".osr")) {
                    Flogger.instance.log(Prefix.INFO, "OSR Received " + fileName, 0);

                    // Download the file
                    attachment.downloadToFile().thenAccept(file -> {
                        try {
                            String image = uploadFile(file);
                            if (image != null) {
                                event.getChannel().sendMessage("https://lookatmysco.re " + image).queue();

                                // Ask if the user wants to generate a card
                                event.getChannel().sendMessage("Do you want to generate a card? Reply with 'yes' or 'no'.")
                                    .queue(message -> {
                                        // Add a listener for the next message from the same user
                                        event.getJDA().addEventListener(new ListenerAdapter() {
                                            @Override
                                            public void onMessageReceived(MessageReceivedEvent responseEvent) {
                                                // Check if the response is from the same user and the same channel
                                                if (responseEvent.getAuthor().equals(event.getAuthor()) && 
                                                    responseEvent.getChannel().equals(event.getChannel())) {
                                                    
                                                    // Check if the response is 'yes'
                                                    if (responseEvent.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                                                        LocalDateTime now = LocalDateTime.now();
                                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                                        String timestamp = now.format(formatter);
                                                        event.getChannel().sendMessage("Card generated at: " + timestamp)
                                                            .queue(cardMessage -> {
                                                                cardMessage.delete().queueAfter(8, TimeUnit.SECONDS);
                                                            });
                                                    }

                                                    // Remove this listener after handling the response
                                                    event.getJDA().removeEventListener(this);
                                                }
                                            }
                                        });

                                        // Set a timeout to remove the listener if no response is received within 10 seconds
                                        event.getChannel().sendMessage("Waiting for response...")
                                            .queue(waitingMessage -> waitingMessage.delete().queueAfter(10, TimeUnit.SECONDS));
                                    });
                            }
                        } catch (IOException e) {
                            Flogger.instance.error(new Exception(e));
                        }
                    }).exceptionally(e -> {
                        Flogger.instance.error(new Exception(e));
                        return null;
                    });
                }
            }
        }
    }

     private String uploadFile(File file) throws IOException {
        String url = "https://lookatmysco.re/api/submit-osr";
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just a random unique string
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.
        String outputUrl = null;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (
            OutputStream output = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)
        ) {
            // Send binary file.
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"osr_file\"; filename=\"").append(file.getName()).append("\"").append(CRLF);
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

        Map<String, List<String>> headerFields = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (key != null && values != null) {
                for (String value : values) {
                    Flogger.instance.log(Prefix.INFO, key + ": " + value, 0);
                }
            }
        }

    
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
