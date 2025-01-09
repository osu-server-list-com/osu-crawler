package osu.serverlist.DiscordBot.models;

import java.util.HashMap;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class ServerModal {
    private int id;
    private String name;
    private String url;
    private boolean online;

    @SerializedName("safe_name")
    private String safeName;

    @SerializedName("safe_categories")
    private String safeCategories;

    
    private List<CategorieModel> categories;
    private HashMap<String, Integer> stats;
    private HashMap<String, String> created;
    private HashMap<String, String> customizations;
}
