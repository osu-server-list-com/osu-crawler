package osu.serverlist.Models;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.Setter;

// Base for AP alerts

public class Alert {
    public class Author {
        @Getter
        @Setter
        private String name;
        @Getter
        @Setter
        private String icon_url;
    }

    @Nullable
    @Getter
    @Setter
    private Author author;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private String id;
}
