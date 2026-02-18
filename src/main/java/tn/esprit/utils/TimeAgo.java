package tn.esprit.utils;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeAgo {

    public static String from(LocalDateTime createdAt) {
        if (createdAt == null) return "recent";

        Duration d = Duration.between(createdAt, LocalDateTime.now());
        long seconds = d.getSeconds();

        if (seconds < 60) return "just now";

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");

        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");

        long days = hours / 24;
        if (days < 7) return days + (days == 1 ? " day ago" : " days ago");

        long weeks = days / 7;
        if (weeks < 4) return weeks + (weeks == 1 ? " week ago" : " weeks ago");

        long months = days / 30;
        if (months < 12) return months + (months == 1 ? " month ago" : " months ago");

        long years = days / 365;
        return years + (years == 1 ? " year ago" : " years ago");
    }
}
