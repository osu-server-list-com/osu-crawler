package osu.serverlist.DiscordBot;

import java.util.List;
import java.util.Random;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

public class DiscordBotStatusSwitcher implements Runnable {
    List<Activity> activities = List.of(
            Activity.playing("osu!"),
            Activity.watching(convertPlaceholders("%servers% servers")),
            Activity.listening(convertPlaceholders("%players% users")),
            Activity.competing("osu-server-list.com"));

    private final JDA jdaInstance;

    public DiscordBotStatusSwitcher(JDA jdaInstance) {
        this.jdaInstance = jdaInstance;
    }

    @Override
    public void run() {
        while (true) {
            int chosenElement = new Random().nextInt(0, activities.size());
            jdaInstance.getPresence().setActivity(activities.get(chosenElement));

            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public String convertPlaceholders(String placeholder) {

        final int[] players = { 0 };
        final int[] servers = { 0 };
        DiscordBot.getJdaInstance().getGuilds().forEach(guild -> {
            servers[0]++;
            players[0] += guild.getMemberCount();
        });

        if (placeholder.contains("%servers%")) {
            placeholder = placeholder.replace("%servers%", String.valueOf(servers[0]));
        }
        if (placeholder.contains("%players%")) {
            placeholder = placeholder.replace("%players%", String.valueOf(players[0]));
        }
        return placeholder;

    }

}
