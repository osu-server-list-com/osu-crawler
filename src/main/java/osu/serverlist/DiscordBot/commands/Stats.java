package osu.serverlist.DiscordBot.commands;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import commons.marcandreher.Commons.Flogger.Prefix;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import osu.serverlist.DiscordBot.base.DiscordCommand;
import osu.serverlist.DiscordBot.base.MessageBuilder;
import osu.serverlist.DiscordBot.models.CategorieModel;
import osu.serverlist.DiscordBot.models.ServerModal;
import osu.serverlist.Main.Crawler;

public class Stats implements DiscordCommand {

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    public static Map<String, Integer> serverNames = new HashMap<String, Integer>();
    public static String[] servers = { "Loading..." };

    @Override
    public void handleCommand(SlashCommandInteractionEvent event) {
        String server = event.getOption("server").getAsString().toLowerCase();
        event.deferReply().queue();

        Integer serverId = serverNames.get(server.toLowerCase());

        if(serverId == null) {
            event.getHook().sendMessage("").setEmbeds(MessageBuilder.buildMessageError("Server was not found").build()).queue();
            return;
        }

        String getServerUrl = Crawler.env.get("DOMAIN") + "/api/v3/server?id=" + serverId;
        System.out.println(Prefix.API + getServerUrl);
        

        Request apiRequest = new Request.Builder().url(getServerUrl).build();

        try (Response apiRequestResponse = client.newCall(apiRequest).execute()) {

            if (!apiRequestResponse.isSuccessful()) {
                event.getHook().sendMessage("OSL API is not reachable.").queue();
                return;
            }

            JsonObject serverJson = JsonParser.parseString(apiRequestResponse.body().string()).getAsJsonObject();
            ServerModal serverModal = gson.fromJson(serverJson, ServerModal.class);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(serverModal.getName(), Crawler.env.get("DOMAIN") + "/server/" + serverModal.getSafeName());
            embed.setThumbnail(serverModal.getCustomizations().get("logo"));
            embed.setColor(0x5755d9);

            String embedDescription = "**Categories**: ";
            
            for (CategorieModel category : serverModal.getCategories()) {
                embedDescription += ":placard: " + category.getName() + " ";
            }
            embedDescription += "\n**Created**: :watch: " + serverModal.getCreated().get("converted") + " ago";
            embedDescription += "\n**Status**: " + (serverModal.isOnline() ? ":white_check_mark:" : ":x:");
         
            if(serverModal.getCustomizations().get("banner") != null) {
                embed.setImage(serverModal.getCustomizations().get("banner"));
            }
            DecimalFormat decimalFormat = new DecimalFormat("#,###");
            for (Map.Entry<String, Integer> statsMap : serverModal.getStats().entrySet()) {
                List<String> allowList = List.of("players", "votes");
                if (!allowList.contains(statsMap.getKey()) && statsMap.getValue() == 0) {
                    continue;
                }

                Field embedField = new Field(statsMap.getKey().substring(0, 1).toUpperCase() + statsMap.getKey().substring(1), decimalFormat.format(statsMap.getValue()), true);
                embed.addField(embedField);
            }

            embed.setDescription(embedDescription);
            String baseUrl =  Crawler.env.get("DOMAIN") + "/server/";
            List<ItemComponent> components = List.of(Button.link(baseUrl+ server.toLowerCase().replaceAll(" ", "_"), "View Server"), Button.link(baseUrl + server.toLowerCase().replaceAll(" ", "_") + "/vote", "Vote"));
            event.getHook().sendMessage("").setEmbeds(embed.build()).setActionRow(components).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("An error occurred while fetching server stats.").queue();
            return;
        }
    }
    
    @Override
    public void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        List<Command.Choice> options = Stream.of(servers)
                .filter(server -> server.toLowerCase()
                        .startsWith(event.getFocusedOption().getValue().toLowerCase()))
                .map(server -> new Command.Choice(server, server))
                .collect(Collectors.toList());
        event.replyChoices(options).queue();
    }

    @Override
    public String getName() {
        return "stats";
    }

}
