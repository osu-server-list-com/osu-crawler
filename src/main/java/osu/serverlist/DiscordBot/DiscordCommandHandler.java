package osu.serverlist.DiscordBot;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

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
                            uploadFile(file);
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

    private void uploadFile(File file) throws IOException {
        String url = "https://lookatmysco.re/api/submit-osr";
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just a random unique string
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (
                OutputStream output = connection.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {
            // Send binary file.
            writer.append("--").append(boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"osr_file\"; filename=\"" + file.getName() + "\"")
                    .append(CRLF);
            writer.append("Content-Type: " + Files.probeContentType(file.toPath())).append(CRLF);
            writer.append(CRLF).flush();
            Files.copy(file.toPath(), output);
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // End of multipart/form-data.
            writer.append("--").append(boundary).append("--").append(CRLF).flush();
        }

        // Log the response headers and redirect URL
        int responseCode = connection.getResponseCode();
        Flogger.instance.log(Prefix.INFO, "Response Code: " + responseCode, 0);

        connection.getHeaderFields().forEach((key, value) -> {
            if (key != null && value != null) {
                value.forEach(v -> Flogger.instance.log(Prefix.INFO, key + ": " + v, 0));
            }
        });

        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
            String redirectUrl = connection.getHeaderField("Location");
            Flogger.instance.log(Prefix.INFO, "Redirect URL: " + redirectUrl, 0);
        }

        connection.disconnect();
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
